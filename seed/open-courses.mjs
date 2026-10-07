// Cursos gratis de «GEMS Abierto»: uno corto, uno mediano y uno medio largo, con contenido original.
// Idempotente por título. Requiere haber corrido seed/open-campus.mjs. Uso: DEV_PASSWORD=... node seed/open-courses.mjs
const BASE = process.env.BASE || 'http://localhost:8080/api/v1';
const PASSWORD = process.env.DEV_PASSWORD;
const OPEN = process.env.OPEN_INSTITUTION_ID || 'gems-abierto';
if (!PASSWORD) { console.error('Define DEV_PASSWORD.'); process.exit(1); }

const login = await fetch(`${BASE}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'super@gems.lms', password: PASSWORD }) });
if (!login.ok) { console.error('No se pudo entrar como super@gems.lms', login.status); process.exit(1); }
const { token } = await login.json();
const H = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

// ── Helpers ────────────────────────────────────────────────────────────────
const q = (n, question, options, correct, explanation) => ({
  id: `q${n}`, type: 'multiple-choice', question, points: 1, order: n, allowMultiple: false,
  options: options.map((text, i) => ({ id: 'abcd'[i], text })), correctAnswers: ['abcd'[correct]], explanation,
});
const quiz = (title, questions, passingScore = 60, duration = 10) => JSON.stringify({
  title, duration, isRequired: true, timeLimit: 0, passingScore, maxAttempts: 0, shuffleQuestions: false,
  questions: questions.map((x, i) => q(i + 1, ...x)),
});
const doc = (title, minutes, markdownContent) => JSON.stringify({ title, duration: minutes, isRequired: true, markdownContent });
/** Lección de teoría + práctica. `questions`: [pregunta, opciones, índice correcto, explicación][] */
const lesson = (title, minutes, md, questions) => ({
  title, contents: [
    { type: 'document', orderIndex: 1, value: doc('Lo esencial', minutes, md) },
    ...(questions ? [{ type: 'quiz', orderIndex: 2, value: quiz(`Practica: ${title.toLowerCase()}`, questions) }] : []),
  ],
});
const build = c => ({
  ...c, status: 'published', instructorName: 'Equipo GEMS', institutionId: OPEN,
  modules: c.modules.map((m, mi) => ({ ...m, orderIndex: mi + 1,
    lessons: m.lessons.map((l, li) => ({ ...l, orderIndex: li + 1, ...(mi === 0 && li === 0 ? { isFree: true } : {}) })) })),
});
const img = id => `https://images.unsplash.com/photo-${id}?w=800&q=70`;

// ── 1. Corto: finanzas personales ──────────────────────────────────────────
const finanzas = build({
  title: 'Tu primer presupuesto: finanzas personales en 3 lecciones',
  description: 'Aprende a saber en qué se va tu plata, a armar un presupuesto que sí se cumple y a empezar un fondo de emergencias. Sin fórmulas raras: ejemplos con pesos colombianos y decisiones del día a día.',
  difficulty: 'beginner', tags: ['Finanzas personales', 'Ahorro', 'Vida práctica', 'Gratis'],
  thumbnailUrl: img('1554224155-6726b3ff858f'),
  modules: [{ title: 'Ordena tu plata', description: 'Gastos, presupuesto y ahorro.', lessons: [
    lesson('¿En qué se va tu plata?', 5, `# ¿En qué se va tu plata?

Antes de ahorrar hay que **ver**. Durante una semana anota todo lo que gastas, hasta el tinto y el pasaje.

Luego clasifica cada gasto:

| Tipo | Qué es | Ejemplos |
|---|---|---|
| **Fijo** | Igual todos los meses | Arriendo, plan del celular |
| **Variable necesario** | Cambia, pero no puedes evitarlo | Mercado, transporte |
| **Hormiga** | Pequeño, frecuente y fácil de olvidar | Snacks, domicilios, apps |

> Un gasto hormiga de $6.000 diarios son unos **$180.000 al mes**. No es pecado tenerlos; el problema es no saber cuánto suman.`, [
      ['¿Cuál de estos es un gasto fijo?', ['El arriendo', 'Un domicilio el viernes', 'Un café en la universidad', 'Una salida a cine'], 0, 'El arriendo se paga igual cada mes; los demás cambian o son ocasionales.'],
      ['Si gastas $5.000 diarios en snacks, ¿cuánto suma aproximadamente en un mes de 30 días?', ['$50.000', '$100.000', '$150.000', '$500.000'], 2, '5.000 × 30 = 150.000. Por eso conviene sumar los gastos hormiga.'],
      ['¿Cuál es el primer paso para organizar tus finanzas?', ['Pedir un préstamo', 'Registrar en qué gastas', 'Cancelar todas las suscripciones', 'Abrir una cuenta de inversión'], 1, 'Sin saber en qué se va la plata no puedes decidir qué recortar.'],
    ]),
    lesson('Arma un presupuesto que sí se cumple', 6, `# Arma un presupuesto que sí se cumple

Una regla sencilla para empezar es la **50/30/20** sobre lo que te entra al mes:

- **50 %** para necesidades: vivienda, comida, transporte, servicios.
- **30 %** para gustos: salidas, ropa, entretenimiento.
- **20 %** para ahorro o pagar deudas.

Ejemplo con un ingreso de **$1.600.000**: 800.000 necesidades, 480.000 gustos y 320.000 ahorro.

No es una ley: si tu arriendo se come más del 50 %, ajusta los gustos, no el ahorro. Lo importante es **decidir antes de gastar**, no al final del mes con lo que sobre.

> Truco: apenas te pagan, separa el ahorro primero. Lo que no ves, no lo gastas.`, [
      ['Con la regla 50/30/20 y un ingreso de $2.000.000, ¿cuánto va a ahorro?', ['$200.000', '$400.000', '$600.000', '$1.000.000'], 1, 'El 20 % de 2.000.000 es 400.000.'],
      ['Si tu arriendo ya ocupa el 60 % de tu ingreso, lo más sensato es:', ['Dejar de ahorrar', 'Reducir el porcentaje de gustos', 'Endeudarte con tarjeta', 'Ignorar el presupuesto'], 1, 'Se ajustan los gastos flexibles (gustos) para proteger el ahorro.'],
      ['¿Por qué conviene separar el ahorro apenas te pagan?', ['Porque da intereses de inmediato', 'Porque lo que queda a la vista se tiende a gastar', 'Porque el banco lo exige', 'No conviene'], 1, '«Págate primero»: lo que no está disponible es más difícil de gastar.'],
    ]),
    lesson('Tu fondo de emergencias', 5, `# Tu fondo de emergencias

Es una plata guardada **solo** para imprevistos: una calamidad, quedarte sin trabajo, un arreglo urgente.

- **Meta:** entre 3 y 6 meses de tus gastos necesarios.
- **Dónde:** en un lugar seguro y fácil de sacar, separado de tu cuenta del día a día (por ejemplo, un bolsillo o cuenta de ahorro aparte).
- **Cómo:** empieza pequeño. Si tus gastos necesarios son $900.000 al mes, una primera meta de $900.000 ya te da un mes de tranquilidad.

> Una emergencia **no** es una oferta ni un viaje. Si lo usas, tu prioridad es volver a llenarlo.`, [
      ['Si tus gastos necesarios son $1.000.000 al mes, un fondo de 3 meses es:', ['$300.000', '$1.000.000', '$3.000.000', '$6.000.000'], 2, '3 meses × 1.000.000 = 3.000.000.'],
      ['¿Cuál de estos SÍ es un buen uso del fondo de emergencias?', ['Un descuento del Black Friday', 'Una cirugía imprevista', 'Unas vacaciones', 'Un celular nuevo'], 1, 'Es para imprevistos necesarios, no para compras planeadas o gustos.'],
      ['¿Dónde conviene guardar el fondo?', ['En efectivo en la billetera', 'En la misma cuenta de los gastos diarios', 'En un lugar aparte, seguro y fácil de sacar', 'Prestado a un amigo'], 2, 'Separado para no gastarlo, pero disponible cuando haga falta.'],
    ]),
  ] }],
});

// ── 2. Mediano: escribir mejor ─────────────────────────────────────────────
const escritura = build({
  title: 'Escribe mejor: ortografía y redacción que se notan',
  description: 'Para la universidad, el trabajo o un mensaje importante. Repasa las reglas que más se fallan, aprende a armar párrafos claros y practica con ejercicios cortos y explicados.',
  difficulty: 'beginner', tags: ['Escritura', 'Ortografía', 'Redacción', 'Gratis'],
  thumbnailUrl: img('1455390582262-044cdead277a'),
  modules: [
    { title: 'Ortografía que más se falla', description: 'Tildes, b/v, c/s/z y las dudas de siempre.', lessons: [
      lesson('Tildes sin miedo', 7, `# Tildes sin miedo

Primero encuentra la **sílaba tónica** (la que suena más fuerte). Luego aplica:

| Palabra | Sílaba tónica | Lleva tilde si… | Ejemplos |
|---|---|---|---|
| **Aguda** | Última | Termina en **n, s o vocal** | canción, compás, café |
| **Grave** | Penúltima | **No** termina en n, s o vocal | árbol, lápiz, fácil |
| **Esdrújula** | Antepenúltima | **Siempre** | música, teléfono, rápido |

**Tilde diacrítica:** distingue palabras que se escriben igual: *tú* (pronombre) / *tu* (posesivo), *él* / *el*, *más* (cantidad) / *mas* (pero), *sí* (afirmación) / *si* (condición).`, [
      ['¿Cuál palabra está bien escrita?', ['Cancion', 'Arbol', 'Música', 'Lapiz'], 2, 'Música es esdrújula y siempre lleva tilde. Las otras deberían ser canción, árbol y lápiz.'],
      ['«Reloj» es aguda y termina en j. Entonces:', ['Lleva tilde', 'No lleva tilde', 'Depende del contexto', 'Lleva tilde en la o'], 1, 'Las agudas solo llevan tilde si terminan en n, s o vocal.'],
      ['Completa: «¿___ trajiste ___ cuaderno?»', ['Tu / tú', 'Tú / tu', 'Tú / tú', 'Tu / tu'], 1, 'El primero es pronombre (tú trajiste) y el segundo, posesivo (tu cuaderno).'],
      ['¿En cuál oración «más» va con tilde?', ['Quería ir, ___ no pude', 'Necesito ___ tiempo', 'Ambas', 'Ninguna'], 1, '«Más» con tilde indica cantidad. En la primera significa «pero» y se escribe «mas».'],
    ]),
    lesson('Las dudas de siempre', 6, `# Las dudas de siempre

- **Haber / a ver:** *haber* es verbo (*va a haber examen*); *a ver* es «veamos» (*a ver qué pasa*).
- **Hay / ahí / ay:** *hay* (existe), *ahí* (lugar), *ay* (queja).
- **Porque / por qué / porqué / por que:**
  - *¿Por qué no viniste?* (pregunta)
  - *No vine porque llovió* (causa)
  - *No sé el porqué* (sustantivo: «el motivo»)
- **Echo / hecho:** *echo* de echar (*echo sal*); *hecho* de hacer (*ya está hecho*).`, [
      ['Completa: «Mañana va a ___ simulacro.»', ['a ver', 'haber', 'aver', 'haver'], 1, 'Es el verbo haber: va a haber (va a existir) un simulacro.'],
      ['Completa: «No fui ___ estaba enfermo.»', ['por qué', 'porqué', 'porque', 'por que'], 2, 'Expresa causa, así que va junto y sin tilde.'],
      ['¿Cuál está bien?', ['Ya lo e echo', 'Ya lo he hecho', 'Ya lo he echo', 'Ya lo e hecho'], 1, '«He» del verbo haber y «hecho» del verbo hacer.'],
      ['Completa: «___ unas llaves ___ encima de la mesa.»', ['Ahí / hay', 'Hay / ahí', 'Ay / hay', 'Hay / ay'], 1, '«Hay» indica que existen; «ahí» indica el lugar.'],
    ]),
  ] },
    { title: 'Redacción clara', description: 'Oraciones, párrafos y conectores.', lessons: [
      lesson('Oraciones cortas, ideas claras', 6, `# Oraciones cortas, ideas claras

Una oración clara dice **quién hace qué**. Tres hábitos ayudan mucho:

1. **Una idea por oración.** Si usas tres «y» seguidas, probablemente son dos oraciones.
2. **Sujeto cerca del verbo.** *El comité, después de revisar durante semanas todas las propuestas, aprobó…* se entiende peor que *Después de semanas de revisión, el comité aprobó…*
3. **Quita relleno:** *«en el día de hoy»* → *hoy*; *«realizar una revisión»* → *revisar*.`, [
      ['¿Cuál versión es más clara?', ['En el día de hoy se procedió a realizar la entrega', 'Hoy entregamos', 'Se realizó en el día de hoy la entrega', 'La entrega fue realizada hoy por parte nuestra'], 1, 'Dice lo mismo con menos palabras y con un sujeto claro.'],
      ['«Realizar una investigación» se puede decir mejor como:', ['Investigar', 'Hacer una investigación realizada', 'Proceder a investigar', 'Llevar a cabo la realización'], 0, 'Un verbo directo reemplaza la fórmula «realizar + sustantivo».'],
      ['¿Qué suele indicar que una oración debe dividirse?', ['Que tiene un verbo', 'Que encadena varias ideas con «y»', 'Que empieza con mayúscula', 'Que tiene sujeto'], 1, 'Muchas ideas encadenadas cansan al lector; mejor separarlas.'],
    ]),
    lesson('Párrafos que se entienden', 7, `# Párrafos que se entienden

Un buen párrafo tiene **una idea central** y la desarrolla:

1. **Oración principal:** dice la idea.
2. **Apoyo:** razones, datos o ejemplos.
3. **Cierre:** conecta con lo que sigue o concluye.

**Conectores útiles**

| Para… | Usa |
|---|---|
| Sumar | además, también, asimismo |
| Contrastar | sin embargo, aunque, en cambio |
| Explicar causa | porque, ya que, debido a |
| Concluir | por lo tanto, en conclusión, así que |`, [
      ['¿Qué conector expresa contraste?', ['Además', 'Sin embargo', 'Por lo tanto', 'Ya que'], 1, '«Sin embargo» introduce algo que se opone a lo anterior.'],
      ['Completa: «Estudió mucho; ___, aprobó el examen.»', ['sin embargo', 'por lo tanto', 'aunque', 'en cambio'], 1, 'Aprobar es la consecuencia de estudiar: se usa un conector de conclusión.'],
      ['¿Qué debe tener un buen párrafo?', ['Varias ideas sin relación', 'Una idea central desarrollada', 'Solo una oración', 'Únicamente conectores'], 1, 'Un párrafo gira alrededor de una idea y la desarrolla con apoyo.'],
    ]),
    lesson('Cómo escribir un correo formal', 6, `# Cómo escribir un correo formal

Para un profesor, una universidad o un trabajo:

1. **Asunto concreto:** *Solicitud de revisión de nota – Cálculo I*, no *Hola*.
2. **Saludo:** *Buenos días, profesora Martínez:*
3. **Quién eres y qué necesitas**, en las dos primeras líneas.
4. **Detalle breve** y lo que propones.
5. **Cierre y firma:** *Quedo atenta a su respuesta. Saludos, Laura Gómez – código 2026…*

> Relee antes de enviar: ortografía, nombre correcto y archivos adjuntos.`, [
      ['¿Cuál es el mejor asunto para pedir una cita a un profesor?', ['Hola', 'Urgente!!!', 'Solicitud de asesoría – Química General', 'Una pregunta'], 2, 'Dice qué necesitas y de qué materia, sin tener que abrir el correo.'],
      ['¿Qué va en las primeras líneas del correo?', ['Un chiste para romper el hielo', 'Quién eres y qué necesitas', 'Tu historia de vida', 'La firma'], 1, 'Quien lee debe entender rápido quién escribe y para qué.'],
      ['Antes de enviar conviene:', ['Escribir todo en mayúsculas', 'Releer ortografía, nombres y adjuntos', 'Agregar muchos emojis', 'No poner asunto'], 1, 'Un error en el nombre o un adjunto olvidado dan mala impresión.'],
    ]),
    { title: 'Reto final de escritura', contents: [
      { type: 'quiz', orderIndex: 1, value: quiz('Reto final', [
        ['¿Cuál oración está correctamente escrita?', ['Mañana va a haber reunión porque cambió el horario', 'Mañana va a a ver reunión por que cambió el horario', 'Mañana va haber reunion porqué cambio el horario', 'Mañana va a haver reunión porque cambio el horario'], 0, '«Haber» (existir), «reunión» con tilde (aguda en n), «porque» de causa y «cambió» con tilde (aguda en vocal).'],
        ['«Teléfono» lleva tilde porque es:', ['Aguda terminada en vocal', 'Grave terminada en consonante', 'Esdrújula', 'Monosílaba'], 2, 'Te-LÉ-fo-no: el acento está en la antepenúltima sílaba.'],
        ['Completa: «Llovió toda la noche; ___, el partido se jugó.»', ['por lo tanto', 'además', 'sin embargo', 'ya que'], 2, 'Que se jugara a pesar de la lluvia es un contraste.'],
        ['¿Cuál es la versión más clara?', ['Se procedió a efectuar el pago de la matrícula', 'Pagué la matrícula', 'Fue efectuado por mí el pago de la matrícula', 'Se realizó por mi parte el pago'], 1, 'Sujeto claro y verbo directo.'],
        ['Completa: «No entiendo ___ no contestas.»', ['porque', 'porqué', 'por qué', 'por que'], 2, 'Es una pregunta indirecta: separado y con tilde.'],
      ], 60, 12) },
    ] },
  ] },
  ],
});

// ── 3. Medio largo: matemáticas para la admisión ───────────────────────────
const mates = build({
  title: 'Matemáticas para la admisión: de cero al simulacro',
  description: 'El repaso completo de las matemáticas que más aparecen en los exámenes de admisión y en el Saber 11: números, álgebra, geometría y datos. Cada lección explica, muestra un ejemplo resuelto y termina con práctica.',
  difficulty: 'intermediate', tags: ['Admisión', 'Matemáticas', 'Saber 11', 'Gratis'],
  thumbnailUrl: img('1635070041078-e363dbe005cb'),
  modules: [
    { title: 'Números y operaciones', description: 'Fracciones, potencias y orden de operaciones.', lessons: [
      lesson('Orden de las operaciones', 6, `# Orden de las operaciones

Cuando una expresión mezcla operaciones, se resuelve en este orden:

1. **Paréntesis**
2. **Potencias y raíces**
3. **Multiplicaciones y divisiones**, de izquierda a derecha
4. **Sumas y restas**, de izquierda a derecha

**Ejemplo:** 8 + 2 × (5 − 3)² = 8 + 2 × 2² = 8 + 2 × 4 = 8 + 8 = **16**

> El error más común: sumar antes de multiplicar. 8 + 2 × 4 no es 40.`, [
        ['¿Cuánto es 6 + 4 × 3?', ['30', '18', '24', '13'], 1, 'Primero 4 × 3 = 12; luego 6 + 12 = 18.'],
        ['¿Cuánto es (6 + 4) × 3?', ['30', '18', '22', '13'], 0, 'El paréntesis va primero: 10 × 3 = 30.'],
        ['¿Cuánto es 20 − 12 ÷ 4 × 2?', ['4', '14', '16', '1'], 1, 'De izquierda a derecha: 12 ÷ 4 = 3; 3 × 2 = 6; 20 − 6 = 14.'],
      ]),
      lesson('Fracciones sin enredos', 8, `# Fracciones sin enredos

- **Sumar o restar:** con el mismo denominador, suma los numeradores. Si son distintos, busca un denominador común. 1/2 + 1/3 = 3/6 + 2/6 = **5/6**.
- **Multiplicar:** numerador por numerador y denominador por denominador. 2/3 × 3/4 = 6/12 = **1/2**.
- **Dividir:** multiplica por la fracción invertida. 3/4 ÷ 1/2 = 3/4 × 2/1 = **3/2**.
- **Simplificar:** divide arriba y abajo por el mismo número. 12/18 = **2/3**.

> Una fracción «de» algo es una multiplicación: 3/5 de 40 = 40 × 3 ÷ 5 = 24.`, [
        ['¿Cuánto es 1/4 + 1/2?', ['2/6', '3/4', '1/6', '2/4'], 1, '1/2 = 2/4; 1/4 + 2/4 = 3/4.'],
        ['¿Cuánto es 2/5 de 60?', ['12', '24', '30', '150'], 1, '60 × 2 ÷ 5 = 24.'],
        ['¿Cuánto es 3/4 ÷ 3/8?', ['9/32', '1/2', '2', '6/8'], 2, '3/4 × 8/3 = 24/12 = 2.'],
        ['Simplifica 18/24', ['3/4', '2/3', '9/16', '6/8'], 0, 'Divide arriba y abajo entre 6: 3/4.'],
      ]),
      lesson('Potencias y raíces', 7, `# Potencias y raíces

- aⁿ es **multiplicar a por sí mismo n veces**: 2⁵ = 32.
- Mismo número base: **se suman** los exponentes al multiplicar (2³ × 2² = 2⁵) y **se restan** al dividir (5⁶ ÷ 5⁴ = 5²).
- Todo número (distinto de 0) elevado a la 0 vale **1**.
- La raíz cuadrada pregunta: ¿qué número al cuadrado da esto? √81 = 9.

> Cuidado: (−3)² = 9, pero −3² = −9. El paréntesis cambia todo.`, [
        ['¿Cuánto es 3⁴?', ['12', '64', '81', '27'], 2, '3 × 3 × 3 × 3 = 81.'],
        ['2³ × 2⁴ es igual a:', ['2⁷', '2¹²', '4⁷', '2¹'], 0, 'Misma base: se suman los exponentes, 3 + 4 = 7.'],
        ['¿Cuánto es √144 + 7⁰?', ['12', '13', '19', '145'], 1, '√144 = 12 y 7⁰ = 1; 12 + 1 = 13.'],
      ]),
    ] },
    { title: 'Álgebra', description: 'Ecuaciones, despejes y problemas planteados.', lessons: [
      lesson('Ecuaciones de primer grado', 8, `# Ecuaciones de primer grado

El objetivo es dejar la **x sola**. Lo que haces de un lado, lo haces del otro.

**Ejemplo:** 3x + 7 = 22
1. Resta 7 a ambos lados: 3x = 15
2. Divide entre 3: **x = 5**
3. Comprueba: 3(5) + 7 = 22 ✔

**Con x en ambos lados:** 5x − 4 = 2x + 11 → 5x − 2x = 11 + 4 → 3x = 15 → x = 5.`, [
        ['Resuelve: 2x + 9 = 25', ['x = 8', 'x = 17', 'x = 16', 'x = 7'], 0, '2x = 16, entonces x = 8.'],
        ['Resuelve: 7x − 5 = 4x + 10', ['x = 3', 'x = 5', 'x = 15', 'x = 1'], 1, '3x = 15, entonces x = 5.'],
        ['Resuelve: x/4 + 3 = 8', ['x = 20', 'x = 44', 'x = 5', 'x = 2'], 0, 'x/4 = 5, entonces x = 20.'],
      ]),
      lesson('Del enunciado a la ecuación', 9, `# Del enunciado a la ecuación

La parte difícil no es resolver, es **plantear**. Traduce frase por frase:

| En palabras | En símbolos |
|---|---|
| «un número» | x |
| «el doble de un número» | 2x |
| «tres más que un número» | x + 3 |
| «la mitad de un número» | x/2 |

**Ejemplo:** *La suma de dos números consecutivos es 41.* → x + (x + 1) = 41 → 2x = 40 → x = 20. Los números son **20 y 21**.

> Siempre vuelve al enunciado y responde lo que se pregunta (a veces piden el otro número).`, [
        ['«El triple de un número, menos 4, es 20». ¿Cuál es el número?', ['8', '6', '16/3', '24'], 0, '3x − 4 = 20 → 3x = 24 → x = 8.'],
        ['Tres números consecutivos suman 48. ¿Cuál es el mayor?', ['15', '16', '17', '18'], 2, 'x + (x+1) + (x+2) = 48 → 3x = 45 → x = 15; el mayor es 17.'],
        ['Una boleta cuesta el doble que una gaseosa. Juntas cuestan $18.000. ¿Cuánto cuesta la boleta?', ['$6.000', '$9.000', '$12.000', '$14.000'], 2, 'x + 2x = 18.000 → x = 6.000; la boleta (2x) cuesta 12.000.'],
      ]),
      lesson('Sistemas de dos ecuaciones', 9, `# Sistemas de dos ecuaciones

Dos incógnitas necesitan dos ecuaciones. El método de **reducción** es el más rápido en examen:

x + y = 10
x − y = 4

Suma las dos ecuaciones: 2x = 14 → **x = 7**. Reemplaza: 7 + y = 10 → **y = 3**.

**Por sustitución:** despeja una letra en una ecuación y reemplázala en la otra. Útil cuando una variable ya está casi sola (y = 2x + 1).`, [
        ['Si x + y = 12 y x − y = 2, ¿cuánto vale x?', ['5', '7', '10', '6'], 1, 'Sumando: 2x = 14, x = 7.'],
        ['Si y = 2x y x + y = 15, ¿cuánto vale y?', ['5', '10', '7,5', '30'], 1, 'x + 2x = 15 → x = 5 → y = 10.'],
        ['En una granja hay gallinas y vacas: 10 cabezas y 32 patas. ¿Cuántas vacas hay?', ['4', '5', '6', '8'], 2, 'g + v = 10 y 2g + 4v = 32. Reemplazando g = 10 − v: 20 + 2v = 32 → v = 6.'],
      ]),
    ] },
    { title: 'Geometría y datos', description: 'Áreas, Pitágoras, promedios y probabilidad.', lessons: [
      lesson('Áreas y perímetros', 7, `# Áreas y perímetros

| Figura | Perímetro | Área |
|---|---|---|
| Rectángulo (b × h) | 2b + 2h | b × h |
| Triángulo | suma de lados | (b × h) ÷ 2 |
| Círculo (radio r) | 2πr | πr² |

**Ejemplo:** un rectángulo de 8 m × 5 m tiene perímetro 26 m y área 40 m².

> Si duplicas los lados de una figura, el perímetro se duplica pero el área se **cuadruplica**.`, [
        ['Área de un triángulo de base 10 cm y altura 6 cm:', ['60 cm²', '30 cm²', '16 cm²', '32 cm²'], 1, '(10 × 6) ÷ 2 = 30.'],
        ['Perímetro de un cuadrado de área 49 m²:', ['7 m', '14 m', '28 m', '49 m'], 2, 'Lado = √49 = 7; perímetro = 4 × 7 = 28.'],
        ['Si los lados de un cuadrado se duplican, su área:', ['Se duplica', 'Se triplica', 'Se cuadruplica', 'No cambia'], 2, '(2L)² = 4L².'],
      ]),
      lesson('Teorema de Pitágoras', 7, `# Teorema de Pitágoras

En un triángulo **rectángulo**, el cuadrado de la hipotenusa (el lado más largo, frente al ángulo recto) es igual a la suma de los cuadrados de los catetos:

**c² = a² + b²**

**Ejemplo:** catetos 6 y 8 → c² = 36 + 64 = 100 → **c = 10**.

Ternas que conviene recordar: (3, 4, 5), (5, 12, 13), (6, 8, 10), (8, 15, 17).`, [
        ['Los catetos miden 5 y 12. ¿Cuánto mide la hipotenusa?', ['13', '17', '15', '60'], 0, '25 + 144 = 169; √169 = 13.'],
        ['Una escalera de 10 m se apoya en una pared con la base a 6 m. ¿A qué altura toca la pared?', ['4 m', '8 m', '16 m', '√136 m'], 1, '10² − 6² = 100 − 36 = 64; √64 = 8.'],
        ['¿Cuál de estos triángulos es rectángulo?', ['Lados 2, 3, 4', 'Lados 4, 5, 6', 'Lados 9, 12, 15', 'Lados 5, 5, 8'], 2, '9² + 12² = 81 + 144 = 225 = 15².'],
      ]),
      lesson('Promedio, mediana y moda', 7, `# Promedio, mediana y moda

Con los datos 4, 7, 7, 9, 13:

- **Promedio (media):** suma ÷ cantidad = 40 ÷ 5 = **8**.
- **Mediana:** el del centro con los datos ordenados = **7**. Si la cantidad es par, promedia los dos del centro.
- **Moda:** el que más se repite = **7**.

> Un dato muy extremo mueve el promedio, pero casi no mueve la mediana. Por eso los salarios se suelen reportar con mediana.`, [
        ['Promedio de 6, 8, 10 y 12:', ['8', '9', '10', '36'], 1, '36 ÷ 4 = 9.'],
        ['Mediana de 3, 9, 1, 7, 5:', ['5', '7', '3', '25'], 0, 'Ordenados: 1, 3, 5, 7, 9; el del centro es 5.'],
        ['Tienes notas 3,0; 4,0 y 3,5. ¿Qué nota necesitas en la cuarta para un promedio de 3,8?', ['4,0', '4,5', '4,7', '5,0'], 2, 'Necesitas que las 4 sumen 15,2; ya tienes 10,5, así que te faltan 4,7.'],
      ]),
      lesson('Probabilidad básica', 7, `# Probabilidad básica

**Probabilidad = casos favorables ÷ casos posibles.** Siempre está entre 0 y 1.

- Sacar un 4 con un dado: 1/6.
- Sacar un número par: 3/6 = 1/2.
- **Eventos independientes** (uno no afecta al otro): se multiplican. Dos caras seguidas con una moneda: 1/2 × 1/2 = **1/4**.
- **Complemento:** P(no ocurre) = 1 − P(ocurre).`, [
        ['En una bolsa hay 3 bolas rojas y 5 azules. ¿Probabilidad de sacar una roja?', ['3/5', '3/8', '5/8', '1/3'], 1, '3 favorables de 8 posibles.'],
        ['Probabilidad de sacar dos 6 lanzando dos dados:', ['1/6', '1/12', '1/36', '2/6'], 2, 'Son independientes: 1/6 × 1/6 = 1/36.'],
        ['Si la probabilidad de lluvia es 0,3, ¿cuál es la de que no llueva?', ['0,3', '0,7', '1,3', '0'], 1, '1 − 0,3 = 0,7.'],
      ]),
      { title: 'Simulacro final de matemáticas', contents: [
        { type: 'quiz', orderIndex: 1, value: quiz('Simulacro de matemáticas', [
          ['¿Cuánto es 5 + 3 × (4 − 2)²?', ['17', '37', '64', '20'], 0, '(4 − 2)² = 4; 3 × 4 = 12; 5 + 12 = 17.'],
          ['¿Cuánto es 3/5 de 45?', ['9', '27', '75', '15'], 1, '45 × 3 ÷ 5 = 27.'],
          ['Resuelve: 4x − 7 = 2x + 9', ['x = 1', 'x = 8', 'x = 16', 'x = 4'], 1, '2x = 16 → x = 8.'],
          ['Dos números suman 30 y uno es el cuádruple del otro. ¿Cuál es el mayor?', ['6', '20', '24', '25'], 2, 'x + 4x = 30 → x = 6; el mayor es 24.'],
          ['Hipotenusa de un triángulo con catetos 9 y 12:', ['15', '21', '13', '108'], 0, '81 + 144 = 225; √225 = 15.'],
          ['Área de un círculo de radio 3 (usa π ≈ 3,14):', ['9,42', '18,84', '28,26', '6,28'], 2, '3,14 × 9 = 28,26.'],
          ['Promedio de 2, 5, 5, 8 y 10:', ['5', '6', '7', '30'], 1, '30 ÷ 5 = 6.'],
          ['Se lanza una moneda tres veces. ¿Probabilidad de tres caras?', ['1/3', '1/6', '1/8', '3/8'], 2, '1/2 × 1/2 × 1/2 = 1/8.'],
          ['Un producto de $50.000 sube 10 % y luego baja 10 %. Precio final:', ['$50.000', '$49.500', '$50.500', '$45.000'], 1, '50.000 × 1,1 × 0,9 = 49.500.'],
          ['2⁵ ÷ 2³ es igual a:', ['2', '4', '8', '16'], 1, 'Se restan exponentes: 2² = 4.'],
        ], 60, 20) },
      ] },
    ] },
  ],
});

// ── Publicar ───────────────────────────────────────────────────────────────
const existing = await fetch(`${BASE}/courses?page=1&limit=100&institutionId=${OPEN}`, { headers: H })
  .then(r => r.json()).catch(() => ({ courses: [] }));
const titles = new Set((existing.courses ?? []).map(c => c.title));
for (const c of [finanzas, escritura, mates]) {
  if (titles.has(c.title)) { console.log(`${c.title} -> ya existe`); continue; }
  const r = await fetch(`${BASE}/courses`, { method: 'POST', headers: H, body: JSON.stringify(c) });
  const lessons = c.modules.reduce((n, m) => n + m.lessons.length, 0);
  console.log(`${c.title} (${lessons} lecciones) ->`, r.status, r.ok ? '' : (await r.text()).slice(0, 300));
}
