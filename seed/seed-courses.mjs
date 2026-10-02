// Crea en el back los cursos de demostración de seed/demo-courses.json que aún no existan
// (se comparan por título). Uso: node seed/seed-courses.mjs  (lo invoca seed-dev.sh)
//
// Variables: BASE (default http://localhost:8080/api/v1), DEV_PASSWORD, INSTITUTION_ID.
import { readFileSync } from 'node:fs';

const BASE = process.env.BASE || 'http://localhost:8080/api/v1';
const PASSWORD = process.env.DEV_PASSWORD || 'GemsDev2026!';
const INSTITUTION_ID = process.env.INSTITUTION_ID || 'inst-1';

const courses = JSON.parse(readFileSync(new URL('./demo-courses.json', import.meta.url), 'utf8'));

const login = await fetch(`${BASE}/auth/login`, {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'super@gems.lms', password: PASSWORD })
});
if (!login.ok) {
  console.error(`  No se pudo iniciar sesión como super@gems.lms (${login.status})`);
  process.exit(1);
}
const { token } = await login.json();
const headers = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

const existing = await fetch(`${BASE}/courses?page=1&limit=500&institutionId=${INSTITUTION_ID}`, { headers })
  .then(r => r.json());
const titles = new Set(existing.courses.map(c => c.title));

for (const course of courses) {
  if (titles.has(course.title)) {
    console.log(`  ${course.title} -> ya existe`);
    continue;
  }
  const res = await fetch(`${BASE}/courses`, {
    method: 'POST',
    headers,
    body: JSON.stringify({ ...course, institutionId: INSTITUTION_ID })
  });
  console.log(`  ${course.title} -> ${res.status}`);
}
