// Utilidades compartidas por los cursos gratis de «GEMS Abierto».
// - Arma lecciones (teoría + práctica) con la duración calculada del texto real.
// - Publica cada curso: si no existe lo crea; si existe lo actualiza conservando los ids de módulos,
//   lecciones y bloques (así nadie pierde su avance, sus intentos ni sus notas).
export const BASE = process.env.BASE || 'http://localhost:8080/api/v1';
export const OPEN = process.env.OPEN_INSTITUTION_ID || 'gems-abierto';

export async function login() {
  const password = process.env.DEV_PASSWORD;
  if (!password) { console.error('Define DEV_PASSWORD.'); process.exit(1); }
  const r = await fetch(`${BASE}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'super@gems.lms', password }) });
  if (!r.ok) { console.error('No se pudo entrar como super@gems.lms', r.status); process.exit(1); }
  const { token } = await r.json();
  return { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };
}

/** Pregunta de opción múltiple. `correct` es el índice (0-3) de la opción correcta. */
const question = (n, text, options, correct, explanation) => {
  if (options.length < 3 || correct < 0 || correct >= options.length || !explanation) {
    throw new Error(`Pregunta mal formada: ${text}`);
  }
  return {
    id: `q${n}`, type: 'multiple-choice', question: text, points: 1, order: n, allowMultiple: false,
    options: options.map((t, i) => ({ id: 'abcdef'[i], text: t })), correctAnswers: ['abcdef'[correct]], explanation,
  };
};

/** `questions`: [pregunta, opciones, índice correcto, explicación][]. ~1,5 min por pregunta. */
export const quiz = (title, questions, passingScore = 60) => JSON.stringify({
  title, duration: Math.max(5, Math.round(questions.length * 1.5)), isRequired: true, timeLimit: 0, passingScore,
  maxAttempts: 0, shuffleQuestions: false, questions: questions.map((x, i) => question(i + 1, ...x)),
});

/** Minutos reales de lectura a 200 palabras por minuto (el mismo ritmo que exige el reproductor). */
export const readingMinutes = md => Math.max(2, Math.ceil(md.split(/\s+/).filter(Boolean).length / 200));

export const doc = (title, markdownContent) =>
  JSON.stringify({ title, duration: readingMinutes(markdownContent), isRequired: true, markdownContent });

/** Lección con teoría y, si trae preguntas, su práctica. */
export const lesson = (title, md, questions, docTitle = 'Lo esencial') => ({
  title, contents: [
    { type: 'document', orderIndex: 1, value: doc(docTitle, md) },
    ...(questions ? [{ type: 'quiz', orderIndex: 2, value: quiz(`Práctica: ${title.charAt(0).toLowerCase()}${title.slice(1)}`, questions) }] : []),
  ],
});

/** Lección que es solo una evaluación (simulacro o reto final). */
export const exam = (title, quizTitle, questions, passingScore = 60) => ({
  title, contents: [{ type: 'quiz', orderIndex: 1, value: quiz(quizTitle, questions, passingScore) }],
});

export const img = id => `https://images.unsplash.com/photo-${id}?w=800&q=70`;

/** Completa los campos comunes; la primera lección queda abierta en la vista previa. */
export const build = c => ({
  ...c, status: 'published', instructorName: 'Equipo GEMS', institutionId: OPEN,
  modules: c.modules.map((m, mi) => ({ ...m, orderIndex: mi + 1,
    lessons: m.lessons.map((l, li) => ({ ...l, orderIndex: li + 1, isFree: mi === 0 && li === 0,
      contents: l.contents.map((ct, ci) => ({ ...ct, orderIndex: ci + 1 })) })) })),
});

/**
 * Copia los ids existentes buscando por título (módulo y lección) y, dentro de la lección, por posición y
 * tipo de bloque. Así una lección nueva intercalada nunca hereda el avance de otra. `renamed` mapea
 * título nuevo → título anterior para lecciones que solo cambiaron de nombre.
 */
function keepIds(next, current, renamed = {}) {
  const oldModules = current.modules ?? [];
  const oldLessons = oldModules.flatMap(m => m.lessons ?? []);
  return {
    ...next,
    modules: next.modules.map(m => {
      const cm = oldModules.find(o => o.title === m.title);
      return { ...m, ...(cm ? { id: cm.id } : {}),
        lessons: m.lessons.map(l => {
          const cl = oldLessons.find(o => o.title === l.title || o.title === renamed[l.title]);
          return { ...l, ...(cl ? { id: cl.id } : {}),
            contents: l.contents.map((ct, ci) => {
              const cc = cl?.contents?.[ci];
              return { ...ct, ...(cc && cc.type === ct.type ? { id: cc.id } : {}) };
            }) };
        }) };
    }),
  };
}

/** Crea el curso o, si ya hay uno con ese título (o con `previousTitle`), lo actualiza en su lugar. */
export async function publish(H, course, { previousTitle, renamed } = {}) {
  const list = await fetch(`${BASE}/courses?page=1&limit=100&institutionId=${OPEN}`, { headers: H })
    .then(r => r.json()).catch(() => ({ courses: [] }));
  const found = (list.courses ?? []).find(c => c.title === course.title || (previousTitle && c.title === previousTitle));
  const lessons = course.modules.reduce((n, m) => n + m.lessons.length, 0);
  const questions = course.modules.flatMap(m => m.lessons).flatMap(l => l.contents)
    .filter(ct => ct.type === 'quiz').reduce((n, ct) => n + JSON.parse(ct.value).questions.length, 0);
  const label = `${course.title} (${lessons} lecciones, ${questions} preguntas)`;
  if (!found) {
    const r = await fetch(`${BASE}/courses`, { method: 'POST', headers: H, body: JSON.stringify(course) });
    console.log(`${label} -> creado`, r.status, r.ok ? '' : (await r.text()).slice(0, 300));
    return;
  }
  const current = await fetch(`${BASE}/courses/${found.id}`, { headers: H }).then(r => r.json());
  const r = await fetch(`${BASE}/courses/${found.id}`, { method: 'PUT', headers: H,
    body: JSON.stringify(keepIds(course, current, renamed)) });
  console.log(`${label} -> actualizado (id ${found.id})`, r.status, r.ok ? '' : (await r.text()).slice(0, 300));
}
