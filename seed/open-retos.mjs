// Curso gratis «Casos y acertijos» de GEMS Abierto: los retos del canal «Piénsala Bien» convertidos en un curso
// de verdad (técnica corta + práctica con explicación). Es un curso como cualquier otro: no cambia el modelo del LMS.
// Si ya existe, se actualiza conservando el avance. Uso: DEV_PASSWORD=... node seed/open-retos.mjs
import { build, exam, img, lesson, login, publish } from './lib/open-lib.mjs';

const H = await login();

const retos = build({
  title: 'Casos y acertijos: entrena tu lógica',
  description: 'Casos de detective, trampas que parecen fáciles y preguntas de razonamiento como las de los exámenes de admisión. Cada lección enseña un truco para pensar mejor y luego lo pones a prueba con retos explicados.',
  difficulty: 'beginner', tags: ['Retos', 'Lógica', 'Acertijos', 'Gratis'],
  thumbnailUrl: img('1453733190371-0a9bedd82893'),
  modules: [
    { title: 'Piensa como detective', description: 'Las pistas que casi todos pasan por alto.', lessons: [
      lesson('La pista que nadie mira', `# La pista que nadie mira

En los casos de detective la respuesta casi nunca está en lo dramático (la voz, la noche, el arma). Está en un **detalle que suena normal** y resulta imposible.

## El método en tres pasos

1. **Lee todo dos veces.** La primera para entender la historia; la segunda, solo para buscar datos: fechas, horas, objetos, quién dijo qué.
2. **Pregúntate qué es imposible.** No qué es raro, sino qué **no puede pasar**: una fecha que no existe, una televisión encendida sin luz, una cinta rebobinada por un muerto.
3. **Descarta lo emocional.** Si tu primera sospecha es «la voz no era la suya» o «estaba oscuro», revísala: suele ser la trampa.

## Ejemplo resuelto

*El testigo dice que vio al ladrón «el sábado 31 de junio».*

- Lo dramático: el robo, la noche, el testigo nervioso.
- El dato: **31 de junio**.
- Lo imposible: junio tiene 30 días. El testigo mintió.

## Errores comunes

- Buscar un motivo en vez de una contradicción.
- Saltar el enunciado y leer solo la pregunta.
- Elegir la opción que «suena a película».`, [
        ['El testigo jura que vio al ladrón «el sábado 31 de junio en la noche». ¿Por qué el detective supo que mentía?', ['Esa noche llovió', 'Junio no tiene 31 días', 'Los sábados no abren', 'Estaba muy oscuro'], 1, 'Junio tiene 30 días: el 31 de junio no existe.'],
        ['Dos amigas piden el mismo café helado. Una lo toma rápido y no le pasa nada; la otra lo toma despacio y se envenena. ¿Dónde estaba el veneno?', ['En el café', 'En el pitillo', 'En el hielo', 'En el vaso'], 2, 'En el hielo: la que tomó rápido terminó antes de que se derritiera.'],
        ['Hallan a un hombre con una grabadora: «Ya no aguanto más» y un disparo. El detective dice que no fue lo que parece. ¿Qué lo delató?', ['No había arma', 'La cinta estaba rebobinada al inicio', 'La voz no era la suya', 'Se oyeron dos disparos'], 1, 'Si él grabó y disparó, nadie pudo rebobinar la cinta después: alguien más estuvo ahí.'],
        ['Un hombre aparece muerto en un campo, sin huellas alrededor y con un paquete cerrado a su lado. ¿Qué pasó?', ['Lo atropellaron', 'Le cayó un rayo', 'Saltó de un avión y el paquete era su paracaídas', 'Lo dejaron de noche'], 2, 'El paquete era su paracaídas y no se abrió; por eso no hay huellas.'],
        ['Se va la luz y desaparece el reloj. La cocinera lavaba platos, el chofer veía televisión y la jardinera prendía velas. ¿Quién miente?', ['La cocinera', 'El chofer', 'La jardinera', 'Nadie'], 1, 'Sin luz no hay televisión: la coartada del chofer es imposible.'],
      ]),
    ] },
    { title: 'Trampas que parecen fáciles', description: 'Preguntas cortas que se responden mal por ir rápido.', lessons: [
      lesson('Lee lo que dice, no lo que esperas', `# Lee lo que dice, no lo que esperas

Las trampas rápidas funcionan porque el cerebro **completa** la pregunta con lo que espera leer. «Se mueren todas menos 9» se lee como «se mueren 9».

## Tres hábitos que las desarman

1. **Repite la pregunta con tus palabras.** «Adelanto al segundo» → «me pongo donde estaba él».
2. **Desconfía de la primera respuesta si salió en un segundo.** Si fue demasiado fácil, revísala.
3. **Haz la cuenta completa.** En los problemas de dinero, suma lo que entra y lo que sale, no lo que la historia te propone sumar.

## Ejemplo resuelto

*Había 10 velas encendidas y el viento apagó 3. ¿Cuántas quedan al final?*

- Respuesta rápida: 7.
- Lectura cuidadosa: «al final». Las encendidas se consumen.
- Quedan **3**, las que se apagaron.

## Errores comunes

- Restar por reflejo cuando la frase dice «todas menos».
- Confundir el puesto que ocupas con el que adelantaste.
- Sumar cantidades que ya están incluidas en otras.`, [
        ['En una carrera adelantas al que va de segundo. ¿En qué puesto quedas?', ['Primero', 'Segundo', 'Tercero', 'Depende'], 1, 'Ocupas su lugar: segundo.'],
        ['Había 10 velas encendidas y el viento apagó 3. ¿Cuántas quedan al final?', ['7', '10', '3', '0'], 2, 'Las encendidas se consumen; quedan las 3 apagadas.'],
        ['Un granjero tiene 17 ovejas y se mueren todas menos 9. ¿Cuántas le quedan?', ['8', '9', '17', '26'], 1, '«Todas menos 9» = sobreviven 9.'],
        ['¿Cuántos meses del año tienen 28 días?', ['1', '2', '6', '12'], 3, 'Todos tienen al menos 28 días.'],
        ['Si anteayer fue jueves, ¿qué día será pasado mañana?', ['Domingo', 'Lunes', 'Martes', 'Sábado'], 1, 'Hoy es sábado; pasado mañana, lunes.'],
        ['Ana tiene 4 hermanos y cada uno tiene una sola hermana. ¿Cuántos hijos hay en la familia?', ['5', '6', '8', '9'], 0, 'La hermana de todos es Ana: 4 + 1 = 5.'],
        ['Tres amigos pagan $30.000 por una pizza de $25.000; el mesero se queda $2.000 y devuelve $1.000 a cada uno. 27.000 + 2.000 = 29.000. ¿Dónde están los $1.000?', ['Los tiene el mesero', 'En la caja', 'No falta nada: la suma está mal planteada', 'Se perdieron'], 2, 'Los $27.000 ya incluyen los $2.000 del mesero. Lo correcto es 27.000 + 3.000 = 30.000.'],
      ]),
    ] },
    { title: 'Razonamiento de examen', description: 'Lo mismo, con el formato de las pruebas de admisión.', lessons: [
      lesson('Premisas, series y conclusiones', `# Premisas, series y conclusiones

Los exámenes de admisión usan estas mismas trampas con otro vestido: **silogismos** («todos… algunos…») y **series** de números.

## Silogismos: ¿se puede asegurar?

- Solo concluye lo que las premisas **obligan**. Si una conclusión «podría ser», no se puede asegurar.
- Dibuja: un círculo para cada grupo. Si puedes dibujarlo de dos formas y en una la conclusión falla, no es segura.

## Series: busca la regla de las diferencias

1. Resta cada número del siguiente.
2. Si las diferencias no son iguales, resta las diferencias.
3. Prueba también multiplicar: muchas series son n × (n + 1) o potencias.

## Ejemplo resuelto

*2, 6, 12, 20, 30, …*

- Diferencias: 4, 6, 8, 10 → la siguiente es 12.
- 30 + 12 = **42**. (También 6 × 7.)`, [
        ['Todos los gatos son animales y algunos animales son negros. ¿Se puede asegurar que algunos gatos son negros?', ['Sí, siempre', 'No se puede asegurar', 'No, ninguno', 'Solo los domésticos'], 1, 'Los animales negros podrían no ser gatos.'],
        ['¿Qué número sigue? 2, 6, 12, 20, 30, …', ['36', '40', '42', '44'], 2, 'Las diferencias suben de 2 en 2: +12 → 42.'],
        ['Todos los médicos estudiaron. Juan estudió. Entonces:', ['Juan es médico', 'Juan no es médico', 'No se puede asegurar que sea médico', 'Juan estudió medicina'], 2, 'Estudiar no implica ser médico: la premisa va en un solo sentido.'],
        ['¿Qué número sigue? 3, 6, 12, 24, …', ['36', '48', '30', '42'], 1, 'Cada número se duplica: 24 × 2 = 48.'],
        ['Ningún pez vuela. Algunos animales que nadan son peces. Entonces:', ['Ningún animal que nada vuela', 'Algunos animales que nadan no vuelan', 'Todos los que nadan son peces', 'Algunos peces vuelan'], 1, 'Los que nadan y son peces no vuelan; eso basta para «algunos que nadan no vuelan».'],
      ]),
      exam('Reto final: ¿piensas bien?', 'Reto final', [
        ['El testigo dice que vio el robo «el 31 de abril». ¿Qué lo delata?', ['Abril tiene 30 días', 'Era de noche', 'No había testigos', 'Llovía'], 0, 'Abril tiene 30 días: la fecha no existe.'],
        ['Un tren eléctrico va al norte y el viento sopla al sur. ¿Hacia dónde va el humo?', ['Al norte', 'Al sur', 'Hacia arriba', 'No hay humo'], 3, 'Un tren eléctrico no echa humo.'],
        ['Si un médico te da 3 pastillas y te dice que tomes una cada media hora, ¿cuánto duran?', ['1 hora', '1 hora y media', '2 horas', '30 minutos'], 0, 'Tomas una ya, otra a la media hora y la última a la hora.'],
        ['¿Qué número sigue? 1, 4, 9, 16, …', ['20', '24', '25', '32'], 2, 'Son los cuadrados: 5² = 25.'],
        ['Algunos estudiantes trabajan. Todos los que trabajan madrugan. Entonces:', ['Todos los estudiantes madrugan', 'Algunos estudiantes madrugan', 'Ningún estudiante madruga', 'Todos los que madrugan estudian'], 1, 'Los estudiantes que trabajan madrugan: algunos estudiantes madrugan.'],
        ['En una carrera adelantas al último. ¿En qué puesto quedas?', ['Penúltimo', 'Último', 'Es imposible', 'Primero'], 2, 'Nadie va detrás del último para adelantarlo.'],
      ], 60),
    ] },
  ],
});

await publish(H, retos);
