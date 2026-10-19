#!/usr/bin/env node
// Genera ms-education/src/main/resources/taps/banco.txt, la lista blanca de Taps que acepta el ranking,
// a partir del banco de la web (gems-lms-web: projects/main/src/app/taps/taps.banco.generated.ts).
//
// Uso (desde la raíz de gems-lms-api):
//   node ms-education/scripts/generar-taps-banco.mjs ../gems-lms-web/projects/main/src/app/taps/taps.banco.generated.ts
//
// Cada línea del resultado: «<tapId> <tema> <total de preguntas>». Hay que regenerarlo (y desplegar el API)
// cada vez que la web añada, quite o cambie Taps; si no, las partidas de los Taps nuevos responden 400.
import { readFileSync, writeFileSync } from 'node:fs';
import { dirname, join } from 'node:path';
import { fileURLToPath } from 'node:url';

const origen = process.argv[2];
if (!origen) {
  console.error('Falta la ruta a taps.banco.generated.ts');
  process.exit(1);
}

const texto = readFileSync(origen, 'utf8');
const inicio = texto.indexOf('[', texto.indexOf('=', texto.indexOf('TAPS_BANCO')));
// El arreglo termina en la primera línea «];» (después del arreglo el archivo tiene más código).
const fin = texto.indexOf('\n];', inicio) + 1;
if (inicio < 0 || fin < inicio) {
  console.error('No encontré el arreglo TAPS_BANCO');
  process.exit(1);
}
// El arreglo es JSON salvo por la coma final de cada elemento.
const banco = JSON.parse(texto.slice(inicio, fin + 1).replace(/,\s*\]$/, ']'));

const temas = new Set(['minecraft', 'logica', 'cultura']);
const vistos = new Set();
const lineas = banco.map((tap) => {
  const id = tap.id;
  const tema = tap.topic;
  const total = tap.questions?.length ?? 0;
  if (!/^[a-z0-9-]{1,60}$/.test(id)) throw new Error(`tapId no válido: ${id}`);
  if (!temas.has(tema)) throw new Error(`tema no válido en ${id}: ${tema}`);
  if (total < 1 || total > 10) throw new Error(`total no válido en ${id}: ${total}`);
  if (vistos.has(id)) throw new Error(`tapId repetido: ${id}`);
  vistos.add(id);
  return `${id} ${tema} ${total}`;
});

const destino = join(dirname(fileURLToPath(import.meta.url)), '..', 'src', 'main', 'resources', 'taps', 'banco.txt');
writeFileSync(destino, [
  '# GENERADO por ms-education/scripts/generar-taps-banco.mjs desde el banco de la web. No lo edites a mano.',
  '# <tapId> <tema> <total de preguntas>',
  ...lineas,
  '',
].join('\n'));
console.log(`${lineas.length} Taps escritos en ${destino}`);
