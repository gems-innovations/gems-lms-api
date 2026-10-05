// Simulación de un semestre en una universidad, contra el back real (api-gateway).
//
// Crea una institución nueva y desechable con su administrador, docentes, estudiantes, periodo
// académico, cursos (lectura + quiz + tarea con rúbrica), grupos e inscripciones. Luego simula la
// actividad concurrente de una semana de clases y comprueba que los datos queden coherentes:
// cupos que no se sobrepasan, intentos que respetan el máximo, notas que llegan al libro de
// calificaciones, foros, encuestas, notificaciones y reportes.
//
// Uso: DEV_PASSWORD=... node seed/university-sim.mjs
// Variables: BASE (http://localhost:8080/api/v1), STUDENTS (200), INSTRUCTORS (8), COURSES (8),
//            CONCURRENCY (40), CAPACITY (40, cupo del curso que se disputa).
// Todos los usuarios simulados usan DEV_PASSWORD; no lo ejecutes contra producción.

const BASE = process.env.BASE || 'http://localhost:8080/api/v1';
const PASSWORD = process.env.DEV_PASSWORD;
const N_STUDENTS = +(process.env.STUDENTS || 200);
const N_INSTRUCTORS = +(process.env.INSTRUCTORS || 8);
const N_COURSES = +(process.env.COURSES || 8);
const CONCURRENCY = +(process.env.CONCURRENCY || 40);
const CAPACITY = +(process.env.CAPACITY || 40);
if (!PASSWORD) { console.error('Define DEV_PASSWORD.'); process.exit(1); }

const RUN = Date.now().toString(36);
const INST = `sim-${RUN}`;

// ---------------------------------------------------------------- métricas
const stats = new Map(); // "METHOD /ruta/normalizada" -> { ms: [], codes: {} }
const failures = [];
const checks = [];

function routeKey(method, path) {
  return `${method} ${path.split('?')[0].replace(/\/\d+(?=\/|$)/g, '/:id').replace(/sim-[a-z0-9]+/g, ':inst')}`;
}

async function api(method, path, { token, body, expect = [200, 201, 204], raw = false } = {}) {
  const started = performance.now();
  let res, text = '';
  try {
    res = await fetch(BASE + path, {
      method,
      headers: { 'Content-Type': 'application/json', ...(token ? { Authorization: `Bearer ${token}` } : {}) },
      body: body === undefined ? undefined : JSON.stringify(body),
    });
    text = await res.text();
  } catch (e) {
    res = { status: 0 };
    text = String(e.cause?.code || e.message);
  }
  const ms = performance.now() - started;
  const key = routeKey(method, path);
  const s = stats.get(key) ?? { ms: [], codes: {} };
  s.ms.push(ms);
  s.codes[res.status] = (s.codes[res.status] ?? 0) + 1;
  stats.set(key, s);
  if (!expect.includes(res.status)) failures.push(`${key} -> ${res.status} ${text.slice(0, 160)}`);
  let data = null;
  if (!raw) { try { data = text ? JSON.parse(text) : null; } catch { data = text; } }
  return { status: res.status, data: raw ? text : data };
}

async function pool(items, fn, size = CONCURRENCY) {
  const out = new Array(items.length);
  let next = 0;
  await Promise.all(Array.from({ length: Math.min(size, items.length) }, async () => {
    while (next < items.length) { const i = next++; out[i] = await fn(items[i], i); }
  }));
  return out;
}

function check(name, ok, detail = '') {
  checks.push({ name, ok, detail });
  console.log(`  ${ok ? 'OK ' : 'FALLA'} ${name}${detail ? ` — ${detail}` : ''}`);
}

const phase = name => console.log(`\n== ${name}`);
const pick = (arr, n, seed) => arr.filter((_, i) => (i * 7 + seed) % arr.length < n);
const list = d => Array.isArray(d) ? d : (d?.content ?? d?.items ?? d?.data ?? d?.courses ?? d?.users ?? []);

async function login(email) {
  const r = await api('POST', '/auth/login', { body: { email, password: PASSWORD } });
  return r.data?.token ? { token: r.data.token, id: r.data.userId ?? r.data.user?.id } : null;
}

// ---------------------------------------------------------------- contenido de un curso
function courseBody(n) {
  const quiz = {
    title: `Parcial ${n}`, duration: 10, isRequired: true, timeLimit: 0, passingScore: 60, maxAttempts: 2,
    shuffleQuestions: false,
    questions: [1, 2, 3].map(q => ({
      id: `q${q}`, type: 'multiple-choice', question: `Pregunta ${q} del curso ${n}`, points: 1, order: q,
      allowMultiple: false, options: ['a', 'b', 'c', 'd'].map(id => ({ id, text: `Opción ${id}` })),
      correctAnswers: ['a'],
    })),
  };
  const assignment = {
    title: `Taller ${n}`, duration: 30, isRequired: true, assignmentInstructions: 'Entrega un informe corto.',
    maxScore: 100, allowedFileTypes: ['pdf'],
    rubric: [{ id: 'r1', criterion: 'Contenido', maxPoints: 60 }, { id: 'r2', criterion: 'Presentación', maxPoints: 40 }],
  };
  return {
    title: `Simulación ${RUN} · Curso ${n}`, description: `Curso ${n} de la universidad simulada.`, status: 'published',
    difficulty: 'beginner', tags: ['simulación'], instructorName: `Docente ${n}`, institutionId: INST,
    // Tamaño de un curso real: 3 unidades de 4 semanas; el parcial y el taller van en la última.
    modules: [1, 2, 3].map(u => ({
      title: `Unidad ${u}`, orderIndex: u, lessons: [1, 2, 3, 4].map(w => ({
        title: `Semana ${(u - 1) * 4 + w}`, orderIndex: w, contents: [
          { type: 'document', orderIndex: 1, value: JSON.stringify({ title: 'Lectura', duration: 5, isRequired: true, markdownContent: '# Lectura\n\nTexto.' }) },
          ...(u === 3 && w === 4 ? [
            { type: 'quiz', orderIndex: 2, value: JSON.stringify(quiz) },
            { type: 'assignment', orderIndex: 3, value: JSON.stringify(assignment) },
          ] : []),
        ],
      })),
    })),
  };
}

function blocksOf(course) {
  const blocks = [];
  for (const m of course.modules ?? []) for (const l of m.lessons ?? []) for (const c of l.contents ?? []) blocks.push(c);
  return { quiz: blocks.find(b => b.type === 'quiz'), assignment: blocks.find(b => b.type === 'assignment') };
}

// ================================================================ simulación
const t0 = performance.now();
console.log(`Universidad simulada ${INST}: ${N_STUDENTS} estudiantes, ${N_INSTRUCTORS} docentes, ${N_COURSES} cursos, concurrencia ${CONCURRENCY}`);

phase('1. Superadministrador crea la institución y su administrador');
const superAdmin = await login('super@gems.lms');
if (!superAdmin) { console.error('No se pudo iniciar sesión como super@gems.lms'); process.exit(1); }
const S = superAdmin.token;
await api('POST', '/institutions', { token: S, body: {
  id: INST, name: `Universidad Simulada ${RUN}`, type: 'university', status: 'active',
  metadata: { description: 'Institución creada por la simulación de carga.', contactEmail: `contacto@${INST}.edu`, maxUsers: 100000, subscriptionType: 'enterprise' },
  branding: { type: 'color-badge', colorPrimary: '#2563EB', colorSecondary: '#1E1B4B', darkMode: true },
} });

const register = (first, last, email, role) => api('POST', '/auth/register', { token: S, body: {
  firstName: first, lastName: last, username: email.split('@')[0].replace(/[^a-z0-9]/gi, '.'), email, password: PASSWORD, role, institutionId: INST,
} });
const adminEmail = `admin@${INST}.edu`;
await register('Admin', 'Simulado', adminEmail, 'ADMIN');
const admin = await login(adminEmail);
check('el administrador de la institución inicia sesión', !!admin);
const A = admin?.token;

phase('2. Alta masiva de docentes y estudiantes');
const instructorEmails = Array.from({ length: N_INSTRUCTORS }, (_, i) => `docente${i + 1}@${INST}.edu`);
const studentEmails = Array.from({ length: N_STUDENTS }, (_, i) => `est${i + 1}@${INST}.edu`);
await pool(instructorEmails, (e, i) => register('Docente', `Número ${i + 1}`, e, 'INSTRUCTOR'), 10);
await pool(studentEmails, (e, i) => register('Estudiante', `Número ${i + 1}`, e, 'STUDENT'), 10);
const instructors = (await pool(instructorEmails, login, 10)).filter(Boolean);
const students = (await pool(studentEmails, login)).filter(Boolean);
check('todos los docentes inician sesión', instructors.length === N_INSTRUCTORS, `${instructors.length}/${N_INSTRUCTORS}`);
check('todos los estudiantes inician sesión (ráfaga concurrente)', students.length === N_STUDENTS, `${students.length}/${N_STUDENTS}`);

phase('3. Periodo académico, cursos, reglas de inscripción y grupos');
const today = new Date();
const iso = d => d.toISOString().slice(0, 10);
const period = await api('POST', '/academic-periods', { token: A, body: {
  name: `Semestre ${RUN}`, startsOn: iso(today), endsOn: iso(new Date(today.getTime() + 120 * 864e5)),
} });
const courses = [];
for (let n = 1; n <= N_COURSES; n++) {
  const created = await api('POST', '/courses', { token: A, body: courseBody(n) });
  if (!created.data?.id) continue;
  const full = (await api('GET', `/courses/${created.data.id}`, { token: A })).data;
  courses.push({ id: created.data.id, ...blocksOf(full), instructor: instructors[(n - 1) % instructors.length] });
}
check('se crean todos los cursos con quiz y tarea', courses.length === N_COURSES && courses.every(c => c.quiz?.id && c.assignment?.id),
  `${courses.length}/${N_COURSES}`);

const raceCourse = courses[0];
const opensAt = new Date(today.getTime() - 864e5).toISOString().slice(0, 19);
const closesAt = new Date(today.getTime() + 30 * 864e5).toISOString().slice(0, 19);
for (const c of courses) {
  await api('PUT', `/courses/${c.id}/enrollment-rules`, { token: A, body: {
    periodId: period.data?.id, opensAt, closesAt, capacity: c === raceCourse ? CAPACITY : null, selfEnrollment: true, prerequisiteIds: [],
  } });
}

phase('4. Inscripciones');
// Cada estudiante (salvo el curso con cupo) queda en 3 cursos, inscrito por el administrador en bloque.
const regular = courses.slice(1);
const rosters = new Map(regular.map(c => [c.id, []]));
students.forEach((s, i) => { for (let k = 0; k < 3 && regular.length; k++) rosters.get(regular[(i + k) % regular.length].id).push(s.id); });
await pool([...rosters], ([courseId, ids]) => api('POST', '/enrollments/bulk', { token: A, body: { courseId, studentIds: ids } }), 4);

// Carrera por el cupo: todos los estudiantes intentan inscribirse a la vez en el curso con cupo.
const race = await pool(students, s => api('POST', '/enrollments', { token: s.token, body: { studentId: s.id, courseId: raceCourse.id }, expect: [201, 400, 409, 422] }), students.length);
const won = race.filter(r => r.status === 201).length;
const raceRoster = (await api('GET', `/enrollments/course/${raceCourse.id}`, { token: A })).data;
check(`el cupo de ${CAPACITY} no se sobrepasa con ${students.length} inscripciones simultáneas`,
  won === Math.min(CAPACITY, students.length) && list(raceRoster).length === won, `aceptadas ${won}, en lista ${list(raceRoster).length}`);
rosters.set(raceCourse.id, students.filter((_, i) => race[i].status === 201).map(s => s.id));

phase('5. Grupos con su docente');
await pool(courses, c => api('POST', '/groups', { token: A, body: {
  name: `Grupo ${c.id}`, institutionId: INST, instructorId: c.instructor.id, studentIds: rosters.get(c.id), courseIds: [c.id], pathIds: [],
} }), 4);

phase('6. Semana de clases: todos los estudiantes trabajan a la vez');
const byStudent = new Map(students.map(s => [s.id, []]));
for (const c of courses) for (const id of rosters.get(c.id) ?? []) byStudent.get(id)?.push(c);
let extraAttemptRejected = 0, extraAttemptAccepted = 0;
await pool(students, async (s, i) => {
  const T = s.token;
  await api('GET', '/notifications', { token: T });
  await api('GET', '/activity/me', { token: T });
  await api('GET', `/courses/eligibility?courseIds=${(byStudent.get(s.id) ?? []).map(c => c.id).join(",") || 0}`, { token: T });
  const enrollments = list((await api('GET', `/enrollments/student/${s.id}`, { token: T })).data);
  for (const c of byStudent.get(s.id)) {
    await api('GET', `/courses/${c.id}`, { token: T });
    const good = i % 3 !== 0; // dos de cada tres estudiantes aprueban
    const answers = ['q1', 'q2', 'q3'].map(q => ({ questionId: q, answer: good ? 'a' : 'b' }));
    await api('POST', `/courses/${c.id}/blocks/${c.quiz.id}/attempts`, { token: T, body: { answers } });
    await api('POST', `/courses/${c.id}/blocks/${c.quiz.id}/attempts`, { token: T, body: { answers } });
    // Tercer intento: el quiz permite 2.
    const extra = await api('POST', `/courses/${c.id}/blocks/${c.quiz.id}/attempts`, { token: T, body: { answers }, expect: [400, 409, 422, 403] });
    if (extra.status >= 400) extraAttemptRejected++; else extraAttemptAccepted++;
    await api('PUT', `/courses/${c.id}/blocks/${c.assignment.id}/submission`, { token: T, body: { textContent: `Informe de ${s.id}`, fileUrls: [] } });
    const enrollment = enrollments.find(e => e.courseId === c.id);
    if (enrollment) await api('PUT', `/enrollments/${enrollment.id}/progress`, { token: T, body: { progress: 66 } });
    if (i % 5 === 0) await api('POST', `/courses/${c.id}/forum/threads`, { token: T, body: { title: `Duda de ${s.id}`, body: '¿Cuándo es el parcial?' } });
    if (i % 4 === 0) await api('PUT', `/courses/${c.id}/review`, { token: T, body: { rating: 1 + (i % 5), comment: 'Buen curso' } });
    await api('GET', `/courses/${c.id}/gradebook/me`, { token: T });
  }
});
// Doble clic: un estudiante nuevo en el curso envía cinco intentos a la vez; el quiz permite 2.
{
  const c = courses[1];
  const s = students.find(st => !(rosters.get(c?.id) ?? []).includes(st.id));
  if (s && c) {
    await api('POST', '/enrollments/bulk', { token: A, body: { courseId: c.id, studentIds: [s.id] } });
    const answers = [{ questionId: 'q1', answer: 'a' }];
    const burst = await Promise.all(Array.from({ length: 5 }, () =>
      api('POST', `/courses/${c.id}/blocks/${c.quiz.id}/attempts`, { token: s.token, body: { answers }, expect: [201, 409] })));
    const saved = burst.filter(r => r.status === 201).length;
    check('cinco envíos simultáneos del mismo quiz no superan los 2 permitidos', saved >= 1 && saved <= 2, `guardados ${saved}`);
  }
}
check('el máximo de intentos del quiz se respeta', extraAttemptAccepted === 0, `rechazados ${extraAttemptRejected}, aceptados de más ${extraAttemptAccepted}`);

phase('7. Docentes: calificar, anunciar, responder el foro');
let graded = 0;
await pool(courses, async c => {
  const T = c.instructor.token;
  await api('POST', `/courses/${c.id}/announcements`, { token: T, body: { title: 'Bienvenidos', body: 'El parcial es el viernes.', pinned: true } });
  const threads = list((await api('GET', `/courses/${c.id}/forum/threads`, { token: T })).data);
  for (const t of threads.slice(0, 5)) await api('POST', `/courses/${c.id}/forum/threads/${t.id}/posts`, { token: T, body: { body: 'Es el viernes.' } });
  const subs = list((await api('GET', `/courses/${c.id}/submissions`, { token: T })).data);
  await pool(subs, async (sub, k) => {
    const r = await api('PUT', `/submissions/${sub.id}/grade`, { token: T, body: {
      grade: 60 + (k % 41), feedback: 'Buen trabajo', rubricScores: [{ criterionId: 'r1', points: 50 }, { criterionId: 'r2', points: 30 }],
    } });
    if (r.status < 300) graded++;
  }, 8);
  await api('GET', `/courses/${c.id}/gradebook`, { token: T });
}, 4);
const expectedSubmissions = courses.reduce((n, c) => n + (rosters.get(c.id)?.length ?? 0), 0);
check('cada entrega se califica', graded === expectedSubmissions, `${graded}/${expectedSubmissions}`);

phase('8. Coherencia de datos');
const sample = students.slice(0, 15);
let gradebookOk = 0, notified = 0;
await pool(sample, async s => {
  const c = byStudent.get(s.id)[0];
  if (!c) return;
  const gb = (await api('GET', `/courses/${c.id}/gradebook/me`, { token: s.token })).data;
  if (JSON.stringify(gb ?? '').match(/"(grade|score|points)":\s*\d/)) gradebookOk++;
  const notes = list((await api('GET', '/notifications', { token: s.token })).data);
  if (notes.length) notified++;
});
check('las notas aparecen en el libro de calificaciones del estudiante', gradebookOk === sample.length, `${gradebookOk}/${sample.length}`);
check('el estudiante recibe notificaciones (nota, anuncio)', notified === sample.length, `${notified}/${sample.length}`);
const otherInst = await api('GET', `/courses/${courses[1]?.id}`, { token: (await login('estudiante@unal.edu.co'))?.token, expect: [403, 404] });
check('un estudiante de otra institución no ve los cursos de esta', [403, 404].includes(otherInst.status), `HTTP ${otherInst.status}`);

phase('9. Administración: reportes');
const report = await api('GET', `/reports/institutions/${INST}`, { token: A });
const csv = await api('GET', `/reports/institutions/${INST}/export`, { token: A, raw: true, expect: [200, 404] });
check('el reporte de la institución responde', report.status === 200);
await api('GET', `/enrollments/institution/${INST}`, { token: A });
await api('GET', `/users/institution/${INST}`, { token: A });

// ---------------------------------------------------------------- informe
const pct = (a, p) => a[Math.min(a.length - 1, Math.floor(a.length * p))];
console.log(`\n== Latencias por endpoint (ms)`);
console.log('endpoint'.padEnd(64), 'n'.padStart(6), 'p50'.padStart(7), 'p95'.padStart(7), 'max'.padStart(7), ' códigos');
let total = 0, slow = [];
for (const [k, s] of [...stats].sort((a, b) => b[1].ms.length - a[1].ms.length)) {
  const a = [...s.ms].sort((x, y) => x - y);
  total += a.length;
  const p95 = pct(a, 0.95);
  if (p95 > 1000) slow.push(`${k} (p95 ${p95.toFixed(0)} ms)`);
  console.log(k.slice(0, 63).padEnd(64), String(a.length).padStart(6), pct(a, 0.5).toFixed(0).padStart(7), p95.toFixed(0).padStart(7),
    a[a.length - 1].toFixed(0).padStart(7), ' ', JSON.stringify(s.codes));
}
const secs = (performance.now() - t0) / 1000;
console.log(`\n${total} peticiones en ${secs.toFixed(1)} s (${(total / secs).toFixed(1)} req/s)`);
const unexpected = failures.length;
console.log(`Respuestas inesperadas: ${unexpected}`);
const grouped = {};
for (const f of failures) { const k = f.slice(0, 120); grouped[k] = (grouped[k] ?? 0) + 1; }
for (const [k, n] of Object.entries(grouped).sort((a, b) => b[1] - a[1]).slice(0, 25)) console.log(`  ${n}× ${k}`);
if (slow.length) console.log(`Endpoints lentos (p95 > 1 s):\n  ${slow.join('\n  ')}`);
const failed = checks.filter(c => !c.ok);
console.log(`\nVerificaciones: ${checks.length - failed.length}/${checks.length} correctas`);
for (const c of failed) console.log(`  FALLA ${c.name} — ${c.detail}`);
console.log(`Institución de prueba: ${INST}`);
process.exit(failed.length || unexpected ? 1 : 0);
