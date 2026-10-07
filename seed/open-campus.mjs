// Crea la institución pública «GEMS Abierto» (cursos gratis sin registro) y un curso de muestra.
// Idempotente: si ya existen, no los duplica. Uso: DEV_PASSWORD=... node seed/open-campus.mjs
const BASE = process.env.BASE || 'http://localhost:8080/api/v1';
const PASSWORD = process.env.DEV_PASSWORD;
const OPEN = process.env.OPEN_INSTITUTION_ID || 'gems-abierto';
if (!PASSWORD) { console.error('Define DEV_PASSWORD.'); process.exit(1); }

const login = await fetch(`${BASE}/auth/login`, { method: 'POST', headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({ email: 'super@gems.lms', password: PASSWORD }) });
if (!login.ok) { console.error('No se pudo entrar como super@gems.lms', login.status); process.exit(1); }
const { token } = await login.json();
const H = { 'Content-Type': 'application/json', Authorization: `Bearer ${token}` };

const inst = await fetch(`${BASE}/institutions`, { method: 'POST', headers: H, body: JSON.stringify({
  id: OPEN, name: 'GEMS Abierto', type: 'academy', status: 'active',
  metadata: { description: 'Cursos gratis y abiertos para prepararse para entrar a la universidad.', contactEmail: 'hola@gems.lat', maxUsers: 10000000, subscriptionType: 'enterprise' },
  branding: { type: 'logo-text', colorPrimary: '#5B4EC1', colorSecondary: '#1E1B4B', darkMode: true },
}) });
console.log('institución', OPEN, '->', inst.status === 201 ? 'creada' : `ya existía o no se creó (${inst.status})`);

const q = (id, order, question, options, correct, explanation) => ({
  id, type: 'multiple-choice', question, points: 1, order, allowMultiple: false,
  options: options.map((text, i) => ({ id: 'abcd'[i], text })), correctAnswers: [correct], explanation,
});
const quiz = (title, questions, passingScore = 60) => JSON.stringify({
  title, duration: 10, isRequired: true, timeLimit: 0, passingScore, maxAttempts: 0, shuffleQuestions: false, questions,
});
const doc = (title, minutes, markdownContent) => JSON.stringify({ title, duration: minutes, isRequired: true, markdownContent });

const READING = 'Durante el sueño profundo, el cerebro repasa lo aprendido en el día y fortalece las conexiones que considera importantes. Por eso, varios estudios han encontrado que quienes duermen bien la noche anterior a un examen recuerdan más que quienes pasan la noche estudiando. Esto no significa que estudiar sea inútil: sin estudio no hay nada que consolidar. Significa que el descanso es parte del aprendizaje, no una pausa en él.';

const course = {
  title: 'Prepárate para el examen de admisión: razonamiento lógico y lectura',
  description: 'Curso gratis de muestra para practicar las dos habilidades que más piden los exámenes de admisión: razonamiento lógico-matemático y comprensión de lectura. Lecciones cortas, ejercicios con explicación y un simulacro final.',
  status: 'published', difficulty: 'beginner', tags: ['Admisión', 'Razonamiento lógico', 'Lectura crítica', 'Gratis'],
  instructorName: 'Equipo GEMS', institutionId: OPEN,
  thumbnailUrl: 'https://images.unsplash.com/photo-1434030216411-0b793f4b4173?w=800&q=70',
  modules: [
    { title: 'Razonamiento lógico', orderIndex: 1, description: 'Proporciones, porcentajes, secuencias y deducciones.', lessons: [
      { title: 'Antes de empezar', orderIndex: 1, isFree: true, contents: [
        { type: 'document', orderIndex: 1, value: doc('Cómo usar este curso', 3, `# Cómo usar este curso

Este es un curso **gratis y de muestra** hecho por GEMS para practicar razonamiento lógico y lectura, las habilidades que más piden los exámenes de admisión.

- Dedica **10 minutos al día**: una lección y sus ejercicios.
- Lee la explicación de cada respuesta, también de las que aciertas.
- Al final hay un **simulacro** para medir tu avance.

> Las preguntas son originales, construidas para practicar el tipo de razonamiento que evalúan estas pruebas. GEMS no está afiliado a ninguna universidad: confirma siempre la estructura de tu examen en la guía oficial de la universidad.`) },
      ] },
      { title: 'Proporciones y porcentajes', orderIndex: 2, contents: [
        { type: 'document', orderIndex: 1, value: doc('Lo que necesitas saber', 6, `# Proporciones y porcentajes

**Porcentaje** es una fracción de 100: el 15 % de algo es 15/100 de ese algo.

- Descuento del 15 %: pagas el 85 %. Ejemplo: 800.000 × 0,85 = **680.000**.
- Subir 20 % y luego bajar 20 % **no** te deja igual: 1,2 × 0,8 = 0,96, es decir **4 % menos**.

**Proporción inversa**: si más personas hacen el mismo trabajo, tardan menos. Multiplica personas × tiempo para obtener el trabajo total y divide entre las nuevas personas.

> Truco: antes de calcular, estima. Si el descuento es pequeño, la respuesta debe estar cerca del precio original.`) },
        { type: 'quiz', orderIndex: 2, value: quiz('Práctica: proporciones y porcentajes', [
          q('q1', 1, 'Un celular cuesta $800.000 y tiene un descuento del 15 %. ¿Cuánto se paga?', ['$680.000', '$785.000', '$720.000', '$650.000'], 'a', 'Con 15 % de descuento pagas el 85 %: 800.000 × 0,85 = 680.000.'),
          q('q2', 2, 'Si 3 personas pintan una pared en 6 horas, ¿cuánto tardan 9 personas trabajando al mismo ritmo?', ['18 horas', '2 horas', '3 horas', '4 horas'], 'b', 'El trabajo total es 3 × 6 = 18 horas-persona. Con 9 personas: 18 ÷ 9 = 2 horas.'),
          q('q3', 3, 'Un precio sube 20 % y luego baja 20 %. Comparado con el precio original, el precio final es:', ['Igual', '4 % más bajo', '4 % más alto', '20 % más bajo'], 'b', '1,20 × 0,80 = 0,96: queda en el 96 % del original, 4 % más bajo.'),
          q('q4', 4, 'En un grupo hay 12 mujeres y 18 hombres. ¿Qué porcentaje del grupo son mujeres?', ['40 %', '12 %', '66 %', '60 %'], 'a', 'Son 12 de 30 personas: 12 ÷ 30 = 0,4 = 40 %.'),
        ]) },
      ] },
      { title: 'Secuencias y deducciones', orderIndex: 3, contents: [
        { type: 'document', orderIndex: 1, value: doc('Lo que necesitas saber', 6, `# Secuencias y deducciones

**Secuencias**: busca qué cambia de un término al siguiente.

- Si la diferencia es constante, suma siempre lo mismo.
- Si la diferencia **crece** (4, 6, 8…), la siguiente diferencia sigue ese patrón.
- Si cada término se **multiplica** por lo mismo, es una progresión geométrica.

**Deducciones**: dibuja los conjuntos. Si todos los A están dentro de B y B no toca a C, entonces A tampoco toca a C.`) },
        { type: 'quiz', orderIndex: 2, value: quiz('Práctica: secuencias y deducciones', [
          q('q1', 1, '¿Qué número sigue? 2, 6, 12, 20, 30, …', ['40', '42', '44', '36'], 'b', 'Las diferencias son 4, 6, 8, 10; la siguiente es 12: 30 + 12 = 42.'),
          q('q2', 2, '¿Qué número sigue? 3, 6, 12, 24, …', ['36', '30', '48', '42'], 'c', 'Cada término es el doble del anterior: 24 × 2 = 48.'),
          q('q3', 3, '¿Qué número sigue? 1, 4, 9, 16, …', ['20', '25', '24', '32'], 'b', 'Son los cuadrados 1², 2², 3², 4²; sigue 5² = 25.'),
          q('q4', 4, 'Si todos los A son B y ningún B es C, entonces:', ['Ningún A es C', 'Algunos A son C', 'Todos los C son A', 'No se puede saber'], 'a', 'Todo A está dentro de B, y B no comparte nada con C; por eso ningún A puede ser C.'),
        ]) },
      ] },
    ] },
    { title: 'Comprensión de lectura', orderIndex: 2, description: 'Idea principal, inferencias, vocabulario en contexto y propósito del autor.', lessons: [
      { title: 'Idea principal e inferencias', orderIndex: 1, contents: [
        { type: 'document', orderIndex: 1, value: doc('Lee con estas preguntas en mente', 6, `# Idea principal e inferencias

Lee el texto preguntándote:

1. **¿De qué habla?** (tema) y **¿qué dice de eso?** (idea principal).
2. **¿Qué se deduce aunque no lo diga?** (inferencia).
3. **¿Para qué lo escribió el autor?** (propósito).

> La idea principal resume **todo** el texto; si una opción solo cubre un detalle o exagera lo que dice, descártala.

**Texto para practicar**

${READING}`) },
        { type: 'quiz', orderIndex: 2, value: quiz('Práctica: lectura', [
          q('q1', 1, `${READING}\n\n¿Cuál es la idea principal del texto?`, ['El descanso es parte del proceso de aprender', 'Estudiar de noche es inútil', 'Dormir reemplaza al estudio', 'Los exámenes deberían ser en la mañana'], 'a', 'El texto concluye que el descanso es parte del aprendizaje. Las otras opciones exageran o no aparecen en el texto.'),
          q('q2', 2, 'Según el texto, un estudiante que pasa toda la noche estudiando antes de un examen probablemente:', ['Recordará más que quien durmió bien', 'Recordará menos de lo que esperaba', 'No necesitaba estudiar', 'Aprobará con seguridad'], 'b', 'El texto dice que quienes duermen bien recuerdan más que quienes pasan la noche estudiando.'),
          q('q3', 3, 'En el texto, la palabra «consolidar» significa:', ['Olvidar lo aprendido', 'Fijar y reforzar lo aprendido', 'Empezar a estudiar', 'Resumir un texto'], 'b', 'El cerebro fortalece las conexiones de lo aprendido: consolidar es fijarlo y reforzarlo.'),
          q('q4', 4, '¿Cuál es el propósito principal del autor?', ['Explicar por qué el descanso ayuda a aprender', 'Criticar a los estudiantes', 'Vender un método de estudio', 'Describir cómo funciona un examen'], 'a', 'El texto explica, con estudios como apoyo, la relación entre el sueño y la memoria.'),
        ]) },
      ] },
      { title: 'Simulacro final', orderIndex: 2, contents: [
        { type: 'quiz', orderIndex: 1, value: quiz('Simulacro de práctica', [
          q('q1', 1, '¿Cuánto es el 25 % de 80?', ['20', '25', '32', '16'], 'a', '25 % es un cuarto: 80 ÷ 4 = 20.'),
          q('q2', 2, '¿Qué número sigue? 5, 10, 20, 35, …', ['50', '55', '45', '60'], 'b', 'Las diferencias son 5, 10, 15; la siguiente es 20: 35 + 20 = 55.'),
          q('q3', 3, 'Si hoy es martes, ¿qué día será dentro de 10 días?', ['Jueves', 'Viernes', 'Sábado', 'Miércoles'], 'b', 'En 7 días vuelve a ser martes; 3 días más: viernes.'),
          q('q4', 4, 'Ana es mayor que Luis y Luis es mayor que Carla. ¿Quién es la menor?', ['Ana', 'Luis', 'Carla', 'No se puede saber'], 'c', 'Ana > Luis > Carla: la menor es Carla.'),
          q('q5', 5, 'En el texto sobre el sueño, la frase «sin estudio no hay nada que consolidar» implica que:', ['El sueño ayuda solo si antes se estudió', 'Dormir es más importante que estudiar', 'Estudiar impide dormir bien', 'El estudio no sirve'], 'a', 'El sueño refuerza lo aprendido; si no se estudió, no hay nada que reforzar.'),
        ], 60) },
      ] },
    ] },
  ],
};

const existing = await fetch(`${BASE}/courses?page=1&limit=100&institutionId=${OPEN}`, { headers: H }).then(r => r.json()).catch(() => ({ courses: [] }));
if ((existing.courses ?? []).some(c => c.title === course.title)) {
  console.log('curso de muestra -> ya existe');
} else {
  const r = await fetch(`${BASE}/courses`, { method: 'POST', headers: H, body: JSON.stringify(course) });
  console.log('curso de muestra ->', r.status);
}
