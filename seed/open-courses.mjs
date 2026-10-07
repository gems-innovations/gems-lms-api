// Cursos gratis de «GEMS Abierto»: uno corto, uno mediano y uno medio largo, con contenido original.
// Si un curso ya existe, se actualiza conservando el avance. Requiere haber corrido seed/open-campus.mjs.
// Uso: DEV_PASSWORD=... node seed/open-courses.mjs
import { build, exam, img, lesson, login, publish } from './lib/open-lib.mjs';

const H = await login();

// ── 1. Corto: finanzas personales ──────────────────────────────────────────
const finanzas = build({
  title: 'Tu primer presupuesto: finanzas personales en 3 lecciones',
  description: 'Aprende a saber en qué se va tu plata, a armar un presupuesto que sí se cumple y a empezar un fondo de emergencias. Sin fórmulas raras: ejemplos con pesos colombianos, casos resueltos paso a paso y ejercicios con explicación.',
  difficulty: 'beginner', tags: ['Finanzas personales', 'Ahorro', 'Vida práctica', 'Gratis'],
  thumbnailUrl: img('1554224155-6726b3ff858f'),
  modules: [{ title: 'Ordena tu plata', description: 'Gastos, presupuesto y ahorro.', lessons: [
    lesson('¿En qué se va tu plata?', `# ¿En qué se va tu plata?

Antes de ahorrar hay que **ver**. La mayoría de personas cree saber en qué gasta, pero cuando lo anota descubre que los gastos pequeños suman mucho más de lo que pensaba.

## Paso 1: anota todo durante una semana

Todo: el arriendo, el mercado, el pasaje, el tinto, la recarga, el domicilio del viernes. Sirve una libreta, una nota en el celular o una hoja de cálculo. Lo importante es no dejar nada por fuera, ni siquiera lo que pagas en efectivo.

## Paso 2: clasifica cada gasto

| Tipo | Qué es | Ejemplos |
|---|---|---|
| **Fijo** | Igual todos los meses | Arriendo, plan del celular, cuota de la moto |
| **Variable necesario** | Cambia, pero no puedes evitarlo | Mercado, transporte, servicios |
| **Hormiga** | Pequeño, frecuente y fácil de olvidar | Snacks, domicilios, suscripciones que no usas |

## Paso 3: multiplica para ver el mes

Un gasto de la semana se convierte en mes multiplicando por 4 (o por 30 si es diario).

## Ejemplo resuelto

Camila gasta cada día $4.000 en un café y $3.000 en un paquete de galletas.

1. Al día: 4.000 + 3.000 = **$7.000**.
2. Al mes: 7.000 × 30 = **$210.000**.
3. Al año: 210.000 × 12 = **$2.520.000**.

No se trata de prohibirse el café, sino de **decidir** si vale lo que cuesta. Si Camila lo reduce a la mitad, libera $105.000 al mes para ahorrar.

## Errores comunes

- Anotar solo los gastos grandes. Los hormiga son justamente los que se escapan.
- Olvidar gastos anuales, como el SOAT o la matrícula. Divídelos entre 12 y súmalos al mes.
- Juzgarte en lugar de observar. El objetivo de esta semana es **ver**, no recortar todavía.`, [
      ['¿Cuál de estos es un gasto fijo?', ['El arriendo', 'Un domicilio el viernes', 'Un café en la universidad', 'Una salida a cine'], 0, 'El arriendo se paga igual cada mes; los demás cambian o son ocasionales.'],
      ['Si gastas $5.000 diarios en snacks, ¿cuánto suma aproximadamente en un mes de 30 días?', ['$50.000', '$100.000', '$150.000', '$500.000'], 2, '5.000 × 30 = 150.000. Por eso conviene sumar los gastos hormiga.'],
      ['¿Cuál es el primer paso para organizar tus finanzas?', ['Pedir un préstamo', 'Registrar en qué gastas', 'Cancelar todas las suscripciones', 'Abrir una cuenta de inversión'], 1, 'Sin saber en qué se va la plata no puedes decidir qué recortar.'],
      ['El SOAT de tu moto cuesta $480.000 al año. ¿Cuánto deberías apartar cada mes para pagarlo sin apuros?', ['$24.000', '$40.000', '$48.000', '$80.000'], 1, '480.000 ÷ 12 = 40.000 al mes.'],
      ['¿Cuál de estos es un gasto hormiga?', ['El recibo de la luz', 'La matrícula', 'Una suscripción de streaming que casi no usas', 'El arriendo'], 2, 'Es pequeño, se cobra solo y es fácil de olvidar.'],
    ]),
    lesson('Arma un presupuesto que sí se cumple', `# Arma un presupuesto que sí se cumple

Un presupuesto es **decidir antes de gastar** qué vas a hacer con la plata del mes. No es una camisa de fuerza: es un plan que puedes ajustar.

## La regla 50/30/20

Es un buen punto de partida sobre lo que te entra al mes:

- **50 %** para necesidades: vivienda, comida, transporte, servicios, salud.
- **30 %** para gustos: salidas, ropa, entretenimiento, regalos.
- **20 %** para ahorro o para pagar deudas.

## Ejemplo resuelto

Andrés gana **$1.600.000** al mes.

1. Necesidades: 1.600.000 × 0,50 = **$800.000**.
2. Gustos: 1.600.000 × 0,30 = **$480.000**.
3. Ahorro: 1.600.000 × 0,20 = **$320.000**.

Su arriendo y servicios suman $700.000 y el mercado y transporte $250.000: sus necesidades son $950.000, más del 50 %. ¿Qué hace? **Ajusta los gustos**, no el ahorro: le quedan 1.600.000 − 950.000 − 320.000 = **$330.000** para gustos.

## Págate primero

Apenas te pagan, **separa el ahorro** y pásalo a otra cuenta o bolsillo. Si esperas a ver "lo que sobra" al final del mes, casi nunca sobra nada.

## Cómo hacer que se cumpla

- **Revisa cada semana**, no solo a fin de mes. Así corriges a tiempo.
- **Deja un margen** para imprevistos pequeños.
- **Si tienes deudas caras** (tarjeta de crédito, préstamos gota a gota), el 20 % va primero a pagarlas: ningún ahorro rinde lo que cobran esas deudas.

## Errores comunes

- Hacer un presupuesto perfecto en papel y no volver a mirarlo.
- Recortar el ahorro cuando se aprietan las cuentas, en lugar de los gustos.
- No contar los ingresos irregulares: si un mes ganas menos, presupuesta con lo mínimo que sueles recibir.`, [
      ['Con la regla 50/30/20 y un ingreso de $2.000.000, ¿cuánto va a ahorro?', ['$200.000', '$400.000', '$600.000', '$1.000.000'], 1, 'El 20 % de 2.000.000 es 400.000.'],
      ['Si tu arriendo ya ocupa el 60 % de tu ingreso, lo más sensato es:', ['Dejar de ahorrar', 'Reducir el porcentaje de gustos', 'Endeudarte con tarjeta', 'Ignorar el presupuesto'], 1, 'Se ajustan los gastos flexibles (gustos) para proteger el ahorro.'],
      ['¿Por qué conviene separar el ahorro apenas te pagan?', ['Porque da intereses de inmediato', 'Porque lo que queda a la vista se tiende a gastar', 'Porque el banco lo exige', 'No conviene'], 1, '«Págate primero»: lo que no está disponible es más difícil de gastar.'],
      ['Ganas $1.800.000. Con la regla 50/30/20, ¿cuánto queda para gustos?', ['$360.000', '$540.000', '$900.000', '$600.000'], 1, '1.800.000 × 0,30 = 540.000.'],
      ['Tienes una deuda en tarjeta de crédito con intereses altos. ¿Qué conviene hacer con el 20 %?', ['Invertirlo en acciones', 'Usarlo para pagar la deuda', 'Gastarlo en gustos', 'Guardarlo en efectivo'], 1, 'Ningún ahorro rinde lo que cobra una deuda cara; pagarla primero es lo que más te ahorra.'],
      ['Tus ingresos cambian cada mes entre $1.200.000 y $1.900.000. ¿Con cuánto conviene presupuestar?', ['$1.900.000', '$1.550.000', '$1.200.000', 'No se puede presupuestar'], 2, 'Presupuestar con lo mínimo evita quedarte corto en los meses malos; lo que sobre en los buenos va a ahorro.'],
    ]),
    lesson('Tu fondo de emergencias', `# Tu fondo de emergencias

Es una plata guardada **solo para imprevistos**: una calamidad, quedarte sin trabajo, un arreglo urgente de la casa o una cita médica que no esperabas. Es lo que evita que una mala semana se convierta en una deuda de años.

## ¿Cuánto necesito?

La meta habitual es entre **3 y 6 meses de tus gastos necesarios** (no de tu sueldo completo). Si tu trabajo es estable, 3 meses suele bastar; si es independiente o variable, apunta a 6.

## Ejemplo resuelto

Los gastos necesarios de Laura son $900.000 al mes y puede ahorrar $150.000 mensuales.

1. Meta de 3 meses: 900.000 × 3 = **$2.700.000**.
2. Tiempo para lograrla: 2.700.000 ÷ 150.000 = **18 meses**.
3. Primera meta, más alcanzable: un mes de gastos, $900.000, en **6 meses**.

Dividir la meta en escalones hace que no se sienta imposible.

## ¿Dónde guardarlo?

En un lugar **seguro, separado y fácil de sacar**: una cuenta de ahorro o un bolsillo aparte de tu cuenta del día a día. No en efectivo en la casa, no prestado a un amigo y no en inversiones que tarden semanas en devolverte la plata.

## ¿Qué es una emergencia?

Algo **necesario, urgente e inesperado**. Una oferta del Black Friday, un viaje o un celular nuevo no lo son, aunque se sientan urgentes. Si usas el fondo, tu prioridad es **volver a llenarlo**.

## Errores comunes

- Esperar a ganar más para empezar. Con lo que puedas, aunque sea poco.
- Guardarlo en la misma cuenta de los gastos: termina gastándose sin darte cuenta.
- Usarlo para gustos y quedar sin protección justo cuando llega la emergencia de verdad.`, [
      ['Si tus gastos necesarios son $1.000.000 al mes, un fondo de 3 meses es:', ['$300.000', '$1.000.000', '$3.000.000', '$6.000.000'], 2, '3 meses × 1.000.000 = 3.000.000.'],
      ['¿Cuál de estos SÍ es un buen uso del fondo de emergencias?', ['Un descuento del Black Friday', 'Una cirugía imprevista', 'Unas vacaciones', 'Un celular nuevo'], 1, 'Es para imprevistos necesarios, no para compras planeadas o gustos.'],
      ['¿Dónde conviene guardar el fondo?', ['En efectivo en la billetera', 'En la misma cuenta de los gastos diarios', 'En un lugar aparte, seguro y fácil de sacar', 'Prestado a un amigo'], 2, 'Separado para no gastarlo, pero disponible cuando haga falta.'],
      ['Necesitas $2.400.000 para tu fondo y puedes ahorrar $200.000 al mes. ¿Cuánto tardas?', ['8 meses', '10 meses', '12 meses', '24 meses'], 2, '2.400.000 ÷ 200.000 = 12 meses.'],
      ['Usaste $500.000 del fondo para arreglar la nevera. ¿Qué sigue?', ['Nada, para eso estaba', 'Volver a llenarlo poco a poco', 'Pedir un préstamo para reponerlo', 'Cerrar la cuenta'], 1, 'El fondo solo protege si está lleno: la prioridad es reponerlo con el ahorro de los próximos meses.'],
    ]),
  ] }],
});

// ── 2. Mediano: escribir mejor ─────────────────────────────────────────────
const escritura = build({
  title: 'Escribe mejor: ortografía y redacción que se notan',
  description: 'Para la universidad, el trabajo o un mensaje importante. Repasa las reglas de ortografía que más se fallan, aprende a armar oraciones y párrafos claros y a escribir un correo formal. Cada lección trae ejemplos, errores comunes y ejercicios explicados.',
  difficulty: 'beginner', tags: ['Escritura', 'Ortografía', 'Redacción', 'Gratis'],
  thumbnailUrl: img('1455390582262-044cdead277a'),
  modules: [
    { title: 'Ortografía que más se falla', description: 'Tildes, b/v, c/s/z y las dudas de siempre.', lessons: [
      lesson('Tildes sin miedo', `# Tildes sin miedo

Las tildes no se ponen "a oído": siguen tres reglas y unas pocas excepciones.

## Paso 1: encuentra la sílaba tónica

Es la que suena más fuerte. Truco: di la palabra como si llamaras a alguien desde lejos. *Can-CIÓN*, *ÁR-bol*, *MÚ-si-ca*.

## Paso 2: aplica la regla

| Palabra | Sílaba tónica | Lleva tilde si… | Ejemplos |
|---|---|---|---|
| **Aguda** | Última | Termina en **n, s o vocal** | canción, compás, café |
| **Grave** | Penúltima | **No** termina en n, s o vocal | árbol, lápiz, fácil |
| **Esdrújula** | Antepenúltima | **Siempre** | música, teléfono, rápido |

## Ejemplo resuelto

*¿"Reloj" lleva tilde?*

1. Sílaba tónica: re-**LOJ**, la última. Es aguda.
2. Termina en **j**, no en n, s ni vocal.
3. Entonces **no** lleva tilde.

*¿Y "examen"?* e-**XA**-men: grave terminada en n, así que **no** lleva tilde. Pero su plural, e-**XÁ**-me-nes, es esdrújula y **sí** la lleva.

## Tilde diacrítica

Distingue palabras que se escriben igual pero significan distinto:

- **tú** (pronombre) / **tu** (posesivo): *tú tienes tu cuaderno*.
- **él** / **el**: *él trajo el libro*.
- **más** (cantidad) / **mas** (pero): *quiero más*; *quería ir, mas no pude*.
- **sí** (afirmación) / **si** (condición): *sí voy, si me invitas*.

## Errores comunes

- Poner tilde a todas las palabras que terminan en n o s: solo si son agudas.
- Olvidar que los plurales pueden cambiar la regla (*joven* → *jóvenes*).
- Tildar los monosílabos como *fue*, *dio*, *vio* o *ti*: no llevan tilde.`, [
        ['¿Cuál palabra está bien escrita?', ['Cancion', 'Arbol', 'Música', 'Lapiz'], 2, 'Música es esdrújula y siempre lleva tilde. Las otras deberían ser canción, árbol y lápiz.'],
        ['«Reloj» es aguda y termina en j. Entonces:', ['Lleva tilde', 'No lleva tilde', 'Depende del contexto', 'Lleva tilde en la o'], 1, 'Las agudas solo llevan tilde si terminan en n, s o vocal.'],
        ['Completa: «¿___ trajiste ___ cuaderno?»', ['Tu / tú', 'Tú / tu', 'Tú / tú', 'Tu / tu'], 1, 'El primero es pronombre (tú trajiste) y el segundo, posesivo (tu cuaderno).'],
        ['¿En cuál oración «más» va con tilde?', ['Quería ir, ___ no pude', 'Necesito ___ tiempo', 'Ambas', 'Ninguna'], 1, '«Más» con tilde indica cantidad. En la primera significa «pero» y se escribe «mas».'],
        ['¿Cuál es correcto?', ['Los examenes', 'Los exámenes', 'Los exámenés', 'Los exámen'], 1, 'E-XÁ-me-nes es esdrújula: siempre lleva tilde, aunque «examen» no la lleve.'],
        ['¿Cuál de estas palabras NO lleva tilde?', ['Fue', 'Árbol', 'Café', 'Rápido'], 0, 'Los monosílabos como «fue», «dio» o «vio» no llevan tilde.'],
      ]),
      lesson('Las dudas de siempre', `# Las dudas de siempre

Hay parejas de palabras que suenan igual y se confunden todo el tiempo. Con un truco para cada una, dejan de ser un problema.

## Haber / a ver

- **Haber** es un verbo: *va a haber examen* (= va a existir).
- **A ver** es "veamos": *a ver qué pasa*.
- Truco: si puedes cambiarlo por "veamos", es **a ver**.

## Hay / ahí / ay

- **Hay**: existe. *Hay pan.*
- **Ahí**: un lugar. *Déjalo ahí.*
- **Ay**: una queja. *¡Ay, me pegué!*

## Porque / por qué / porqué / por que

- **¿Por qué?** pregunta: *¿Por qué no viniste?* También en preguntas indirectas: *no sé por qué no viniste*.
- **Porque** da la causa: *no vine porque llovió*.
- **El porqué** es un sustantivo, "el motivo": *no entiendo el porqué*.
- **Por que** es raro: *la razón por (la) que lo hizo*.

## Echo / hecho

- **Echo** viene de echar: *le echo sal*.
- **Hecho** viene de hacer: *ya está hecho*, *he hecho la tarea*.

## Ejemplo resuelto

*«___ unas llaves ___ en la mesa, ¿___ de quién son?»*

1. Existen unas llaves → **Hay**.
2. Señala un lugar → **ahí**.
3. Es una pregunta → **a ver** (veamos de quién son).

Resultado: *Hay unas llaves ahí en la mesa, ¿a ver de quién son?*

## Errores comunes

- Escribir "haber" cuando se quiere decir "veamos".
- Usar "porque" en preguntas indirectas: lo correcto es *no sé por qué*.
- Escribir "echo" en tiempos compuestos: siempre es *he hecho*, *has hecho*.`, [
        ['Completa: «Mañana va a ___ simulacro.»', ['a ver', 'haber', 'aver', 'haver'], 1, 'Es el verbo haber: va a haber (va a existir) un simulacro.'],
        ['Completa: «No fui ___ estaba enfermo.»', ['por qué', 'porqué', 'porque', 'por que'], 2, 'Expresa causa, así que va junto y sin tilde.'],
        ['¿Cuál está bien?', ['Ya lo e echo', 'Ya lo he hecho', 'Ya lo he echo', 'Ya lo e hecho'], 1, '«He» del verbo haber y «hecho» del verbo hacer.'],
        ['Completa: «___ unas llaves ___ encima de la mesa.»', ['Ahí / hay', 'Hay / ahí', 'Ay / hay', 'Hay / ay'], 1, '«Hay» indica que existen; «ahí» indica el lugar.'],
        ['Completa: «No sé ___ cambiaron el horario.»', ['porque', 'por qué', 'porqué', 'por que'], 1, 'Es una pregunta indirecta («¿por qué cambiaron el horario?»): separado y con tilde.'],
        ['Completa: «Nadie entendió el ___ de su renuncia.»', ['por qué', 'porque', 'porqué', 'por que'], 2, 'Significa «el motivo»: es un sustantivo, junto y con tilde.'],
      ]),
    ] },
    { title: 'Redacción clara', description: 'Oraciones, párrafos y conectores.', lessons: [
      lesson('Oraciones cortas, ideas claras', `# Oraciones cortas, ideas claras

Escribir claro no es escribir simple: es hacer que el lector entienda **a la primera**. Una oración clara dice **quién hace qué**.

## Hábito 1: una idea por oración

Si usas tres "y" seguidas, probablemente son dos o tres oraciones.

- Confuso: *Fui a la universidad y no había clase y me devolví y perdí el día.*
- Claro: *Fui a la universidad, pero no había clase. Me devolví y perdí el día.*

## Hábito 2: sujeto cerca del verbo

Cuando entre el sujeto y el verbo hay una frase larga, el lector olvida de quién se hablaba.

- Confuso: *El comité, después de revisar durante semanas todas las propuestas que llegaron, aprobó el proyecto.*
- Claro: *Después de semanas de revisión, el comité aprobó el proyecto.*

## Hábito 3: quita el relleno

| En lugar de… | Escribe |
|---|---|
| en el día de hoy | hoy |
| realizar una revisión | revisar |
| proceder a entregar | entregar |
| a nivel de | en |
| debido al hecho de que | porque |

## Ejemplo resuelto

*Se procedió a realizar la entrega de los documentos en el día de hoy por parte del equipo.*

1. ¿Quién hace la acción? El equipo.
2. ¿Qué hace? Entrega los documentos.
3. ¿Cuándo? Hoy.

Versión clara: **El equipo entregó hoy los documentos.** Pasamos de 16 palabras a 6, sin perder nada.

## Errores comunes

- Creer que más palabras suenan más profesional. En la universidad y el trabajo se valora lo directo.
- Abusar de la voz pasiva ("fue realizado por") cuando se puede decir quién lo hizo.`, [
        ['¿Cuál versión es más clara?', ['En el día de hoy se procedió a realizar la entrega', 'Hoy entregamos', 'Se realizó en el día de hoy la entrega', 'La entrega fue realizada hoy por parte nuestra'], 1, 'Dice lo mismo con menos palabras y con un sujeto claro.'],
        ['«Realizar una investigación» se puede decir mejor como:', ['Investigar', 'Hacer una investigación realizada', 'Proceder a investigar', 'Llevar a cabo la realización'], 0, 'Un verbo directo reemplaza la fórmula «realizar + sustantivo».'],
        ['¿Qué suele indicar que una oración debe dividirse?', ['Que tiene un verbo', 'Que encadena varias ideas con «y»', 'Que empieza con mayúscula', 'Que tiene sujeto'], 1, 'Muchas ideas encadenadas cansan al lector; mejor separarlas.'],
        ['«Debido al hecho de que llovió, se canceló el partido.» ¿Cuál es la mejor versión?', ['Debido a que llovió, el partido fue cancelado por la lluvia', 'Como llovió, cancelaron el partido', 'Por el hecho de la lluvia se procedió a cancelar', 'Debido al hecho de la lluvia'], 1, 'Más corta, con causa clara y sin relleno.'],
        ['¿Cuál oración tiene el sujeto más cerca del verbo?', ['La profesora, que había llegado tarde por el tráfico de la mañana, explicó el tema', 'Por el tráfico, la profesora llegó tarde y explicó el tema', 'Explicó, la profesora, el tema que había llegado tarde', 'El tema, por la profesora, fue explicado'], 1, '«La profesora llegó» queda junto; en la primera hay una frase larga en medio.'],
      ]),
      lesson('Párrafos que se entienden', `# Párrafos que se entienden

Un párrafo es un **bloque con una sola idea central**. Si cambias de idea, cambias de párrafo.

## La estructura que funciona

1. **Oración principal:** dice la idea del párrafo, idealmente al inicio.
2. **Apoyo:** razones, datos o ejemplos que la sostienen.
3. **Cierre:** concluye o conecta con lo que sigue.

## Ejemplo resuelto

> **El transporte público debería tener tarifa diferencial para estudiantes.** Muchos jóvenes gastan buena parte de su dinero en pasajes, lo que los obliga a faltar a clase cuando no les alcanza. En ciudades que ya la aplicaron, la asistencia aumentó. **Por lo tanto,** una tarifa más baja no es un regalo, sino una inversión en educación.

- La primera oración dice la idea.
- Las dos siguientes la apoyan con una razón y un dato.
- La última concluye con un conector.

## Conectores útiles

| Para… | Usa |
|---|---|
| Sumar una idea | además, también, asimismo |
| Contrastar | sin embargo, aunque, en cambio |
| Explicar causa | porque, ya que, debido a |
| Mostrar consecuencia | por lo tanto, así que, en consecuencia |
| Dar un ejemplo | por ejemplo, es el caso de |
| Concluir | en conclusión, en resumen |

## Errores comunes

- Párrafos de una sola oración muy larga, o de diez ideas distintas.
- Usar el conector equivocado: "sin embargo" para sumar o "por lo tanto" para contrastar.
- Repetir "y", "entonces" y "pues" en lugar de variar los conectores.`, [
        ['¿Qué conector expresa contraste?', ['Además', 'Sin embargo', 'Por lo tanto', 'Ya que'], 1, '«Sin embargo» introduce algo que se opone a lo anterior.'],
        ['Completa: «Estudió mucho; ___, aprobó el examen.»', ['sin embargo', 'por lo tanto', 'aunque', 'en cambio'], 1, 'Aprobar es la consecuencia de estudiar: se usa un conector de consecuencia.'],
        ['¿Qué debe tener un buen párrafo?', ['Varias ideas sin relación', 'Una idea central desarrollada', 'Solo una oración', 'Únicamente conectores'], 1, 'Un párrafo gira alrededor de una idea y la desarrolla con apoyo.'],
        ['¿Dónde conviene ubicar la oración principal de un párrafo argumentativo?', ['Al inicio', 'Escondida en la mitad', 'En una nota al pie', 'En otro párrafo'], 0, 'Al inicio orienta al lector sobre lo que viene.'],
        ['Completa: «El parque es pequeño; ___, siempre está lleno.»', ['por lo tanto', 'además', 'sin embargo', 'por ejemplo'], 2, 'Que esté lleno a pesar de ser pequeño es un contraste.'],
      ]),
      lesson('Cómo escribir un correo formal', `# Cómo escribir un correo formal

Para un profesor, una universidad o un trabajo, un buen correo se entiende en **30 segundos**. La estructura:

1. **Asunto concreto:** *Solicitud de revisión de nota – Cálculo I*, no *Hola* ni *Urgente*.
2. **Saludo:** *Buenos días, profesora Martínez:* (con dos puntos, no coma).
3. **Quién eres y qué necesitas**, en las dos primeras líneas.
4. **Detalle breve** y lo que propones.
5. **Cierre y firma:** *Quedo atenta a su respuesta. Cordialmente, Laura Gómez – código 2026…*

## Ejemplo resuelto

> **Asunto:** Solicitud de asesoría – Química General, grupo 3
>
> Buenos días, profesor Ríos:
>
> Soy Laura Gómez, estudiante de su curso de Química General del grupo 3. Le escribo para pedirle una asesoría sobre el tema de estequiometría antes del parcial.
>
> Tengo disponibilidad el martes o el jueves después de las 2:00 p. m. Si le queda mejor otro horario, me ajusto.
>
> Quedo atenta a su respuesta.
>
> Cordialmente,
> Laura Gómez – código 2026104

## Antes de enviar

- **Nombre y cargo correctos** de quien recibe.
- **Ortografía:** un error en el primer renglón da mala impresión.
- **Adjuntos:** si dices "adjunto", verifica que esté adjunto.
- **Tono:** respetuoso, sin mayúsculas sostenidas ni signos de exclamación repetidos.

## Errores comunes

- Escribir desde un correo poco serio (apodos, números raros). Usa el institucional si lo tienes.
- Mandar todo el contexto en un párrafo gigante antes de decir qué necesitas.
- Escribir a las 11 p. m. pidiendo respuesta "urgente" para el día siguiente.`, [
        ['¿Cuál es el mejor asunto para pedir una cita a un profesor?', ['Hola', 'Urgente!!!', 'Solicitud de asesoría – Química General', 'Una pregunta'], 2, 'Dice qué necesitas y de qué materia, sin tener que abrir el correo.'],
        ['¿Qué va en las primeras líneas del correo?', ['Un chiste para romper el hielo', 'Quién eres y qué necesitas', 'Tu historia de vida', 'La firma'], 1, 'Quien lee debe entender rápido quién escribe y para qué.'],
        ['Antes de enviar conviene:', ['Escribir todo en mayúsculas', 'Releer ortografía, nombres y adjuntos', 'Agregar muchos emojis', 'No poner asunto'], 1, 'Un error en el nombre o un adjunto olvidado dan mala impresión.'],
        ['¿Cuál saludo es adecuado para un correo formal?', ['Quiubo profe', 'Buenos días, profesora Martínez:', 'Hey!!', 'Hola a todos los que lean esto'], 1, 'Saludo respetuoso, con nombre y dos puntos.'],
        ['¿Cuál cierre es más apropiado?', ['Respóndame rápido', 'Quedo atenta a su respuesta. Cordialmente, Laura Gómez', 'Bye', 'Chao, gracias x todo'], 1, 'Es cordial, no presiona y deja claro quién firma.'],
      ]),
      exam('Reto final de escritura', 'Reto final', [
        ['¿Cuál oración está correctamente escrita?', ['Mañana va a haber reunión porque cambió el horario', 'Mañana va a a ver reunión por que cambió el horario', 'Mañana va haber reunion porqué cambio el horario', 'Mañana va a haver reunión porque cambio el horario'], 0, '«Haber» (existir), «reunión» con tilde (aguda en n), «porque» de causa y «cambió» con tilde (aguda en vocal).'],
        ['«Teléfono» lleva tilde porque es:', ['Aguda terminada en vocal', 'Grave terminada en consonante', 'Esdrújula', 'Monosílaba'], 2, 'Te-LÉ-fo-no: el acento está en la antepenúltima sílaba.'],
        ['Completa: «Llovió toda la noche; ___, el partido se jugó.»', ['por lo tanto', 'además', 'sin embargo', 'ya que'], 2, 'Que se jugara a pesar de la lluvia es un contraste.'],
        ['¿Cuál es la versión más clara?', ['Se procedió a efectuar el pago de la matrícula', 'Pagué la matrícula', 'Fue efectuado por mí el pago de la matrícula', 'Se realizó por mi parte el pago'], 1, 'Sujeto claro y verbo directo.'],
        ['Completa: «No entiendo ___ no contestas.»', ['porque', 'porqué', 'por qué', 'por que'], 2, 'Es una pregunta indirecta: separado y con tilde.'],
        ['Completa: «___ un error ___ en la segunda página.»', ['Ahí / hay', 'Hay / ahí', 'Ay / ahí', 'Hay / hay'], 1, '«Hay» (existe) un error «ahí» (en ese lugar).'],
        ['¿Cuál es el mejor asunto para enviar una hoja de vida?', ['Hola', 'Hoja de vida – Practicante de Mercadeo – Laura Gómez', 'Urgente', 'Te mando esto'], 1, 'Dice qué es, para qué cargo y de quién.'],
        ['¿Cuál palabra está mal escrita?', ['Lápiz', 'Café', 'Joven', 'Jóven'], 3, '«Joven» es grave terminada en n: no lleva tilde. Su plural «jóvenes» sí.'],
      ]),
    ] },
  ],
});

// ── 3. Medio largo: matemáticas para la admisión ───────────────────────────
const mates = build({
  title: 'Matemáticas para la admisión: de cero al simulacro',
  description: 'El repaso completo de las matemáticas que más aparecen en los exámenes de admisión y en el Saber 11: números, álgebra, geometría y datos. Cada lección explica el tema, resuelve un ejemplo paso a paso, señala los errores típicos y termina con práctica comentada.',
  difficulty: 'intermediate', tags: ['Admisión', 'Matemáticas', 'Saber 11', 'Gratis'],
  thumbnailUrl: img('1635070041078-e363dbe005cb'),
  modules: [
    { title: 'Números y operaciones', description: 'Fracciones, potencias y orden de operaciones.', lessons: [
      lesson('Orden de las operaciones', `# Orden de las operaciones

Cuando una expresión mezcla operaciones, no se resuelve de izquierda a derecha sin más: hay un orden que todo el mundo respeta para que el resultado sea único.

## El orden

1. **Paréntesis** (y corchetes), de adentro hacia afuera.
2. **Potencias y raíces.**
3. **Multiplicaciones y divisiones**, de izquierda a derecha.
4. **Sumas y restas**, de izquierda a derecha.

Multiplicar y dividir tienen la **misma** prioridad: se hacen en el orden en que aparecen. Lo mismo pasa con sumar y restar.

## Ejemplo resuelto 1

8 + 2 × (5 − 3)²

1. Paréntesis: 5 − 3 = 2 → 8 + 2 × 2²
2. Potencia: 2² = 4 → 8 + 2 × 4
3. Multiplicación: 2 × 4 = 8 → 8 + 8
4. Suma: **16**

## Ejemplo resuelto 2

20 − 12 ÷ 4 × 2

1. División y multiplicación de izquierda a derecha: 12 ÷ 4 = 3, luego 3 × 2 = 6.
2. Resta: 20 − 6 = **14**.

Si hubieras multiplicado primero (4 × 2 = 8, 12 ÷ 8 = 1,5), obtendrías 18,5, que es incorrecto.

## Errores comunes

- Sumar antes de multiplicar: 8 + 2 × 4 **no** es 40.
- Creer que la multiplicación siempre va antes que la división: van en el orden en que aparecen.
- Olvidar que un signo menos delante de un paréntesis cambia todos los signos de adentro: 10 − (3 − 5) = 10 − (−2) = 12.`, [
        ['¿Cuánto es 6 + 4 × 3?', ['30', '18', '24', '13'], 1, 'Primero 4 × 3 = 12; luego 6 + 12 = 18.'],
        ['¿Cuánto es (6 + 4) × 3?', ['30', '18', '22', '13'], 0, 'El paréntesis va primero: 10 × 3 = 30.'],
        ['¿Cuánto es 20 − 12 ÷ 4 × 2?', ['4', '14', '16', '1'], 1, 'De izquierda a derecha: 12 ÷ 4 = 3; 3 × 2 = 6; 20 − 6 = 14.'],
        ['¿Cuánto es 10 − (3 − 5)?', ['2', '8', '12', '−12'], 2, '3 − 5 = −2; 10 − (−2) = 12.'],
        ['¿Cuánto es 2 + 3² × 2?', ['20', '50', '22', '13'], 0, 'Potencia: 9; multiplicación: 18; suma: 20.'],
      ]),
      lesson('Fracciones sin enredos', `# Fracciones sin enredos

Una fracción a/b significa "a partes de un total dividido en b partes iguales". Casi todos los problemas de fracciones se resuelven con cuatro operaciones.

## Sumar y restar

- **Mismo denominador:** suma los numeradores. 2/7 + 3/7 = **5/7**.
- **Distinto denominador:** lleva las dos a un denominador común (un múltiplo de ambos). 1/2 + 1/3 = 3/6 + 2/6 = **5/6**.

## Multiplicar

Numerador por numerador y denominador por denominador: 2/3 × 3/4 = 6/12 = **1/2**.

## Dividir

Multiplica por la fracción **invertida**: 3/4 ÷ 1/2 = 3/4 × 2/1 = 6/4 = **3/2**.

## Simplificar

Divide arriba y abajo por el mismo número: 12/18 → (÷6) → **2/3**.

## "Una fracción de algo"

"De" significa multiplicar. 3/5 de 40 = 40 × 3 ÷ 5 = **24**. Es más fácil dividir primero: 40 ÷ 5 = 8, y 8 × 3 = 24.

## Ejemplo resuelto

*Juan gastó 1/4 de su sueldo en arriendo y 1/3 en comida. ¿Qué fracción le queda?*

1. Gastó 1/4 + 1/3 = 3/12 + 4/12 = **7/12**.
2. Le queda 1 − 7/12 = 12/12 − 7/12 = **5/12**.

## Errores comunes

- Sumar numeradores y denominadores: 1/2 + 1/3 **no** es 2/5.
- Olvidar invertir la segunda fracción al dividir.
- No simplificar el resultado: muchas opciones del examen vienen simplificadas.`, [
        ['¿Cuánto es 1/4 + 1/2?', ['2/6', '3/4', '1/6', '2/4'], 1, '1/2 = 2/4; 1/4 + 2/4 = 3/4.'],
        ['¿Cuánto es 2/5 de 60?', ['12', '24', '30', '150'], 1, '60 ÷ 5 = 12; 12 × 2 = 24.'],
        ['¿Cuánto es 3/4 ÷ 3/8?', ['9/32', '1/2', '2', '6/8'], 2, '3/4 × 8/3 = 24/12 = 2.'],
        ['Simplifica 18/24', ['3/4', '2/3', '9/16', '6/8'], 0, 'Divide arriba y abajo entre 6: 3/4.'],
        ['Gastas 1/4 de tu plata en arriendo y 1/3 en comida. ¿Qué fracción te queda?', ['5/12', '7/12', '2/7', '1/12'], 0, 'Gastas 3/12 + 4/12 = 7/12; te quedan 5/12.'],
        ['¿Cuánto es 2/3 × 9/4?', ['18/7', '3/2', '11/7', '6/12'], 1, '18/12 = 3/2.'],
      ]),
      lesson('Potencias y raíces', `# Potencias y raíces

## Qué es una potencia

aⁿ significa **multiplicar a por sí mismo n veces**. 2⁵ = 2 × 2 × 2 × 2 × 2 = 32. La base es 2 y el exponente, 5.

## Las propiedades que más se usan

Con la **misma base**:

- Al **multiplicar**, se suman los exponentes: 2³ × 2² = 2⁵.
- Al **dividir**, se restan: 5⁶ ÷ 5⁴ = 5².
- **Potencia de potencia**, se multiplican: (3²)³ = 3⁶.
- Todo número distinto de 0 elevado a la 0 vale **1**: 7⁰ = 1.
- Exponente negativo es "uno sobre": 2⁻³ = 1/2³ = 1/8.

## Raíces

La raíz cuadrada pregunta: ¿qué número multiplicado por sí mismo da esto? √81 = 9 porque 9 × 9 = 81. Conviene saber de memoria los cuadrados del 1 al 15.

## Ejemplo resuelto

*Calcula (2³ × 2⁴) ÷ 2⁵.*

1. Multiplicación con la misma base: 2³ × 2⁴ = 2⁷.
2. División: 2⁷ ÷ 2⁵ = 2².
3. Resultado: **4**.

Sin las propiedades tendrías que calcular 8 × 16 = 128 y luego 128 ÷ 32 = 4. Llegas igual, pero tardas más.

## Errores comunes

- Confundir 2³ con 2 × 3. 2³ = 8, no 6.
- El signo: (−3)² = 9, pero −3² = −9. El paréntesis cambia todo.
- Sumar las bases: 2³ × 3² **no** es 5⁵.`, [
        ['¿Cuánto es 3⁴?', ['12', '64', '81', '27'], 2, '3 × 3 × 3 × 3 = 81.'],
        ['2³ × 2⁴ es igual a:', ['2⁷', '2¹²', '4⁷', '2¹'], 0, 'Misma base: se suman los exponentes, 3 + 4 = 7.'],
        ['¿Cuánto es √144 + 7⁰?', ['12', '13', '19', '145'], 1, '√144 = 12 y 7⁰ = 1; 12 + 1 = 13.'],
        ['(3²)³ es igual a:', ['3⁵', '3⁶', '9⁵', '3⁹'], 1, 'Potencia de potencia: se multiplican los exponentes, 2 × 3 = 6.'],
        ['¿Cuánto es 2⁻²?', ['−4', '1/4', '4', '−1/4'], 1, 'Exponente negativo: 1/2² = 1/4.'],
        ['¿Cuánto vale −3²?', ['9', '−9', '6', '−6'], 1, 'Sin paréntesis, solo el 3 se eleva al cuadrado: −(3²) = −9.'],
      ]),
    ] },
    { title: 'Álgebra', description: 'Ecuaciones, despejes y problemas planteados.', lessons: [
      lesson('Ecuaciones de primer grado', `# Ecuaciones de primer grado

Una ecuación es una balanza en equilibrio. El objetivo es dejar la **x sola** a un lado, y la regla es una: **lo que haces de un lado, lo haces del otro**.

## Ejemplo resuelto 1

3x + 7 = 22

1. Resta 7 a ambos lados: 3x = 15.
2. Divide ambos lados entre 3: **x = 5**.
3. Comprueba: 3(5) + 7 = 15 + 7 = 22 ✔

## Ejemplo resuelto 2: x en ambos lados

5x − 4 = 2x + 11

1. Pasa las x a un lado: 5x − 2x = 11 + 4.
2. Reduce: 3x = 15.
3. Despeja: **x = 5**.

## Ejemplo resuelto 3: con fracciones

x/4 + 3 = 8

1. Resta 3: x/4 = 5.
2. Multiplica por 4: **x = 20**.

## Ejemplo resuelto 4: con paréntesis

2(x − 3) = 10

1. Distribuye: 2x − 6 = 10.
2. Suma 6: 2x = 16.
3. Divide: **x = 8**.

## Errores comunes

- Cambiar de lado un término sin cambiarle el signo.
- Distribuir mal: 2(x − 3) es 2x − 6, no 2x − 3.
- No comprobar. Reemplazar la respuesta en la ecuación original toma 10 segundos y evita errores tontos.`, [
        ['Resuelve: 2x + 9 = 25', ['x = 8', 'x = 17', 'x = 16', 'x = 7'], 0, '2x = 16, entonces x = 8.'],
        ['Resuelve: 7x − 5 = 4x + 10', ['x = 3', 'x = 5', 'x = 15', 'x = 1'], 1, '3x = 15, entonces x = 5.'],
        ['Resuelve: x/4 + 3 = 8', ['x = 20', 'x = 44', 'x = 5', 'x = 2'], 0, 'x/4 = 5, entonces x = 20.'],
        ['Resuelve: 2(x − 3) = 10', ['x = 6,5', 'x = 8', 'x = 2', 'x = 13'], 1, '2x − 6 = 10 → 2x = 16 → x = 8.'],
        ['Resuelve: 5 − x = 2x − 7', ['x = 4', 'x = −4', 'x = 2/3', 'x = 12'], 0, '5 + 7 = 2x + x → 12 = 3x → x = 4.'],
      ]),
      lesson('Del enunciado a la ecuación', `# Del enunciado a la ecuación

En los exámenes, la parte difícil casi nunca es resolver la ecuación: es **plantearla**. Traduce el enunciado frase por frase.

## Diccionario básico

| En palabras | En símbolos |
|---|---|
| un número | x |
| el doble de un número | 2x |
| el triple, menos 4 | 3x − 4 |
| tres más que un número | x + 3 |
| la mitad de un número | x/2 |
| dos números consecutivos | x y x + 1 |
| el cuadrado de un número | x² |

## Método en 4 pasos

1. **Nombra** con x lo que no sabes (escribe qué es x).
2. **Expresa** las demás cantidades en función de x.
3. **Plantea** la ecuación con la condición del enunciado.
4. **Resuelve y responde lo que se pregunta.**

## Ejemplo resuelto

*Una boleta cuesta el doble que una gaseosa. Juntas cuestan $18.000. ¿Cuánto cuesta la boleta?*

1. x = precio de la gaseosa.
2. Boleta = 2x.
3. x + 2x = 18.000.
4. 3x = 18.000 → x = 6.000. La pregunta es por la **boleta**: 2 × 6.000 = **$12.000**.

## Ejemplo resuelto 2

*La suma de tres números consecutivos es 48. ¿Cuál es el mayor?*

x + (x + 1) + (x + 2) = 48 → 3x + 3 = 48 → x = 15. El mayor es x + 2 = **17**.

## Errores comunes

- Responder el valor de x cuando la pregunta pide otra cantidad.
- Traducir "4 menos que un número" como 4 − x. Es **x − 4**.
- No definir qué representa x y confundirse a mitad del problema.`, [
        ['«El triple de un número, menos 4, es 20». ¿Cuál es el número?', ['8', '6', '16/3', '24'], 0, '3x − 4 = 20 → 3x = 24 → x = 8.'],
        ['Tres números consecutivos suman 48. ¿Cuál es el mayor?', ['15', '16', '17', '18'], 2, 'x + (x+1) + (x+2) = 48 → 3x = 45 → x = 15; el mayor es 17.'],
        ['Una boleta cuesta el doble que una gaseosa. Juntas cuestan $18.000. ¿Cuánto cuesta la boleta?', ['$6.000', '$9.000', '$12.000', '$14.000'], 2, 'x + 2x = 18.000 → x = 6.000; la boleta (2x) cuesta 12.000.'],
        ['«Cinco menos que el doble de un número es 13». ¿Cuál es el número?', ['4', '9', '8', '18'], 1, '2x − 5 = 13 → 2x = 18 → x = 9.'],
        ['Pedro tiene 4 años más que Ana. Entre los dos suman 30 años. ¿Cuántos años tiene Pedro?', ['13', '15', '17', '19'], 2, 'Ana = x, Pedro = x + 4. 2x + 4 = 30 → x = 13; Pedro tiene 17.'],
        ['El perímetro de un rectángulo es 40 cm y el largo es el triple del ancho. ¿Cuánto mide el ancho?', ['5 cm', '10 cm', '15 cm', '20/3 cm'], 0, 'Ancho x, largo 3x: 2x + 6x = 40 → 8x = 40 → x = 5.'],
      ]),
      lesson('Sistemas de dos ecuaciones', `# Sistemas de dos ecuaciones

Cuando hay dos cantidades desconocidas, necesitas **dos ecuaciones**. Hay dos métodos que bastan para casi cualquier examen.

## Reducción (suma o resta las ecuaciones)

Sirve cuando una letra tiene el mismo número con signos opuestos, o se puede igualar fácilmente.

x + y = 10
x − y = 4

1. Suma las dos ecuaciones: 2x = 14 → **x = 7**.
2. Reemplaza en la primera: 7 + y = 10 → **y = 3**.

## Sustitución (despeja y reemplaza)

Útil cuando una letra ya está casi sola.

y = 2x
x + y = 15

1. Reemplaza y por 2x: x + 2x = 15 → x = 5.
2. Entonces y = 2 × 5 = **10**.

## Ejemplo resuelto: el clásico de las patas

*En una granja hay gallinas y vacas: 10 cabezas y 32 patas. ¿Cuántas vacas hay?*

1. g = gallinas, v = vacas.
2. Cabezas: g + v = 10. Patas: 2g + 4v = 32.
3. De la primera, g = 10 − v. Reemplaza: 2(10 − v) + 4v = 32 → 20 + 2v = 32 → v = **6**.
4. Gallinas: 10 − 6 = 4. Comprueba: 4 × 2 + 6 × 4 = 8 + 24 = 32 ✔

## Errores comunes

- Resolver solo una incógnita y olvidar la otra.
- Equivocarse con los signos al restar ecuaciones: resta **todos** los términos.
- No comprobar en **ambas** ecuaciones.`, [
        ['Si x + y = 12 y x − y = 2, ¿cuánto vale x?', ['5', '7', '10', '6'], 1, 'Sumando: 2x = 14, x = 7.'],
        ['Si y = 2x y x + y = 15, ¿cuánto vale y?', ['5', '10', '7,5', '30'], 1, 'x + 2x = 15 → x = 5 → y = 10.'],
        ['En una granja hay gallinas y vacas: 10 cabezas y 32 patas. ¿Cuántas vacas hay?', ['4', '5', '6', '8'], 2, 'g + v = 10 y 2g + 4v = 32. Reemplazando g = 10 − v: 20 + 2v = 32 → v = 6.'],
        ['Dos cuadernos y un lápiz cuestan $9.000; un cuaderno y un lápiz cuestan $5.000. ¿Cuánto cuesta un cuaderno?', ['$3.000', '$4.000', '$4.500', '$2.000'], 1, 'Restando las ecuaciones queda un cuaderno: 9.000 − 5.000 = 4.000.'],
        ['Si 2x + y = 11 y x + y = 7, ¿cuánto vale y?', ['3', '4', '5', '7'], 0, 'Restando: x = 4; entonces y = 7 − 4 = 3.'],
      ]),
    ] },
    { title: 'Geometría y datos', description: 'Áreas, Pitágoras, promedios y probabilidad.', lessons: [
      lesson('Áreas y perímetros', `# Áreas y perímetros

- El **perímetro** es la medida del borde: lo que necesitarías de cerca para rodear un terreno. Se mide en m, cm…
- El **área** es la superficie de adentro: lo que necesitarías de baldosa para cubrirlo. Se mide en m², cm²…

## Fórmulas

| Figura | Perímetro | Área |
|---|---|---|
| Cuadrado (lado L) | 4L | L² |
| Rectángulo (base b, altura h) | 2b + 2h | b × h |
| Triángulo | suma de los lados | (b × h) ÷ 2 |
| Círculo (radio r) | 2πr | πr² |

Usa π ≈ 3,14 si el examen no dice otra cosa.

## Ejemplo resuelto 1

*Un terreno rectangular mide 8 m × 5 m.*

- Perímetro: 2 × 8 + 2 × 5 = **26 m**.
- Área: 8 × 5 = **40 m²**.

## Ejemplo resuelto 2: figura compuesta

*Una pared de 4 m × 3 m tiene una ventana de 1 m × 1,5 m. ¿Cuánto hay que pintar?*

1. Área de la pared: 4 × 3 = 12 m².
2. Área de la ventana: 1 × 1,5 = 1,5 m².
3. A pintar: 12 − 1,5 = **10,5 m²**.

## Escalar una figura

Si duplicas los lados, el perímetro se **duplica**, pero el área se **cuadruplica** (2 × 2). Si los triplicas, el área se multiplica por 9.

## Errores comunes

- Confundir radio con diámetro (el diámetro es el doble del radio).
- Olvidar dividir entre 2 en el área del triángulo.
- Mezclar unidades: no se pueden sumar cm con m sin convertir.`, [
        ['Área de un triángulo de base 10 cm y altura 6 cm:', ['60 cm²', '30 cm²', '16 cm²', '32 cm²'], 1, '(10 × 6) ÷ 2 = 30.'],
        ['Perímetro de un cuadrado de área 49 m²:', ['7 m', '14 m', '28 m', '49 m'], 2, 'Lado = √49 = 7; perímetro = 4 × 7 = 28.'],
        ['Si los lados de un cuadrado se duplican, su área:', ['Se duplica', 'Se triplica', 'Se cuadruplica', 'No cambia'], 2, '(2L)² = 4L².'],
        ['Una pared de 4 m × 3 m tiene una ventana de 1 m × 1,5 m. ¿Cuántos m² hay que pintar?', ['10,5 m²', '12 m²', '13,5 m²', '9 m²'], 0, '12 − 1,5 = 10,5 m².'],
        ['Un círculo tiene diámetro 10 cm. ¿Cuál es su área aproximada (π ≈ 3,14)?', ['31,4 cm²', '78,5 cm²', '314 cm²', '15,7 cm²'], 1, 'El radio es 5: 3,14 × 25 = 78,5.'],
      ]),
      lesson('Teorema de Pitágoras', `# Teorema de Pitágoras

Solo sirve en triángulos **rectángulos** (con un ángulo de 90°). El lado más largo, frente al ángulo recto, es la **hipotenusa**; los otros dos son los **catetos**.

## El teorema

**c² = a² + b²**, donde c es la hipotenusa.

## Ejemplo resuelto 1: hallar la hipotenusa

Catetos 6 y 8:

1. c² = 6² + 8² = 36 + 64 = 100.
2. c = √100 = **10**.

## Ejemplo resuelto 2: hallar un cateto

*Una escalera de 10 m se apoya en una pared y su base está a 6 m de la pared. ¿A qué altura toca la pared?*

1. La escalera es la hipotenusa: 10.
2. a² = 10² − 6² = 100 − 36 = 64.
3. a = √64 = **8 m**.

Para un cateto se **resta**; para la hipotenusa se **suma**.

## Ternas pitagóricas

Conviene reconocerlas porque ahorran cálculos: **(3, 4, 5)**, (5, 12, 13), (8, 15, 17) y sus múltiplos, como (6, 8, 10) o (9, 12, 15).

## ¿Es rectángulo?

Comprueba si el cuadrado del lado mayor es igual a la suma de los cuadrados de los otros dos. Lados 9, 12, 15: 81 + 144 = 225 = 15² → **sí** es rectángulo.

## Errores comunes

- Sumar cuando se busca un cateto.
- Olvidar sacar la raíz al final.
- Aplicarlo a triángulos que no son rectángulos.`, [
        ['Los catetos miden 5 y 12. ¿Cuánto mide la hipotenusa?', ['13', '17', '15', '60'], 0, '25 + 144 = 169; √169 = 13.'],
        ['Una escalera de 10 m se apoya en una pared con la base a 6 m. ¿A qué altura toca la pared?', ['4 m', '8 m', '16 m', '√136 m'], 1, '10² − 6² = 100 − 36 = 64; √64 = 8.'],
        ['¿Cuál de estos triángulos es rectángulo?', ['Lados 2, 3, 4', 'Lados 4, 5, 6', 'Lados 9, 12, 15', 'Lados 5, 5, 8'], 2, '9² + 12² = 81 + 144 = 225 = 15².'],
        ['La diagonal de una pantalla rectangular de 30 cm × 40 cm mide:', ['50 cm', '70 cm', '35 cm', '60 cm'], 0, '900 + 1.600 = 2.500; √2.500 = 50. Es la terna (3, 4, 5) multiplicada por 10.'],
        ['Una persona camina 8 km al norte y luego 15 km al este. ¿A qué distancia en línea recta está del punto de partida?', ['23 km', '17 km', '7 km', '13 km'], 1, '64 + 225 = 289; √289 = 17.'],
      ]),
      lesson('Promedio, mediana y moda', `# Promedio, mediana y moda

Son tres formas de resumir un grupo de datos con un solo número.

## Las definiciones

Con los datos 4, 7, 7, 9, 13:

- **Promedio (media):** suma ÷ cantidad = 40 ÷ 5 = **8**.
- **Mediana:** el del centro, con los datos **ordenados** = **7**. Si la cantidad es par, se promedian los dos del centro.
- **Moda:** el que más se repite = **7**. Puede haber más de una, o ninguna.

## Ejemplo resuelto: la nota que necesitas

*Tienes 3,0; 4,0 y 3,5. ¿Qué nota necesitas en la cuarta evaluación para tener un promedio de 3,8?*

1. Para promediar 3,8 con 4 notas, la suma debe ser 3,8 × 4 = **15,2**.
2. Ya tienes 3,0 + 4,0 + 3,5 = 10,5.
3. Te faltan 15,2 − 10,5 = **4,7**.

## Mediana con cantidad par

Datos 2, 5, 8, 10: los del centro son 5 y 8, así que la mediana es (5 + 8) ÷ 2 = **6,5**.

## ¿Cuál usar?

Un dato muy extremo mueve mucho el promedio, pero casi no mueve la mediana. Si en un barrio cuatro personas ganan $2 millones y una gana $50 millones, el promedio (11,6 millones) no representa a nadie; la mediana (2 millones) sí. Por eso los salarios se suelen reportar con mediana.

## Errores comunes

- Calcular la mediana sin ordenar los datos.
- Dividir entre una cantidad equivocada de datos.
- Creer que el promedio siempre es uno de los datos.`, [
        ['Promedio de 6, 8, 10 y 12:', ['8', '9', '10', '36'], 1, '36 ÷ 4 = 9.'],
        ['Mediana de 3, 9, 1, 7, 5:', ['5', '7', '3', '25'], 0, 'Ordenados: 1, 3, 5, 7, 9; el del centro es 5.'],
        ['Tienes notas 3,0; 4,0 y 3,5. ¿Qué nota necesitas en la cuarta para un promedio de 3,8?', ['4,0', '4,5', '4,7', '5,0'], 2, 'Necesitas que las 4 sumen 15,2; ya tienes 10,5, así que te faltan 4,7.'],
        ['Mediana de 2, 5, 8, 10:', ['5', '6,5', '8', '6,25'], 1, 'Cantidad par: (5 + 8) ÷ 2 = 6,5.'],
        ['Moda de 3, 5, 5, 6, 8, 8, 8, 9:', ['5', '6,5', '8', '9'], 2, 'El 8 aparece tres veces, más que cualquier otro.'],
      ]),
      lesson('Probabilidad básica', `# Probabilidad básica

La probabilidad mide qué tan posible es que algo pase, con un número entre **0** (imposible) y **1** (seguro).

## La fórmula

**Probabilidad = casos favorables ÷ casos posibles**, siempre que todos los casos sean igual de probables.

- Sacar un 4 con un dado: 1 favorable de 6 posibles → **1/6**.
- Sacar un número par: 3 favorables (2, 4, 6) de 6 → 3/6 = **1/2**.

## Eventos independientes

Si uno no afecta al otro, las probabilidades **se multiplican**. Dos caras seguidas con una moneda: 1/2 × 1/2 = **1/4**.

## El complemento

La probabilidad de que algo **no** pase es 1 menos la probabilidad de que pase. Si la de lluvia es 0,3, la de que no llueva es 1 − 0,3 = **0,7**. Es muy útil para preguntas de "al menos uno".

## Ejemplo resuelto

*En una bolsa hay 3 bolas rojas y 5 azules. Se saca una, se devuelve y se saca otra. ¿Cuál es la probabilidad de que ambas sean rojas?*

1. Una roja: 3 de 8 → 3/8.
2. Como se devuelve, la segunda es independiente: otra vez 3/8.
3. Ambas: 3/8 × 3/8 = **9/64**.

*Si NO se devolviera,* la segunda tendría 2 rojas de 7 bolas: 3/8 × 2/7 = 6/56 = **3/28**.

## Errores comunes

- Sumar en lugar de multiplicar eventos seguidos.
- Olvidar que sin reposición cambian los casos posibles.
- Dar una probabilidad mayor que 1: si te pasa, revisa.`, [
        ['En una bolsa hay 3 bolas rojas y 5 azules. ¿Probabilidad de sacar una roja?', ['3/5', '3/8', '5/8', '1/3'], 1, '3 favorables de 8 posibles.'],
        ['Probabilidad de sacar dos 6 lanzando dos dados:', ['1/6', '1/12', '1/36', '2/6'], 2, 'Son independientes: 1/6 × 1/6 = 1/36.'],
        ['Si la probabilidad de lluvia es 0,3, ¿cuál es la de que no llueva?', ['0,3', '0,7', '1,3', '0'], 1, '1 − 0,3 = 0,7.'],
        ['Probabilidad de sacar un número mayor que 4 con un dado:', ['1/6', '1/3', '1/2', '2/3'], 1, 'Favorables: 5 y 6, dos de seis: 2/6 = 1/3.'],
        ['Se lanza una moneda tres veces. ¿Probabilidad de obtener al menos una cara?', ['1/8', '3/8', '7/8', '1/2'], 2, 'Lo contrario es «tres sellos»: 1/8. Entonces 1 − 1/8 = 7/8.'],
      ]),
      exam('Simulacro final de matemáticas', 'Simulacro de matemáticas', [
        ['¿Cuánto es 5 + 3 × (4 − 2)²?', ['17', '37', '64', '20'], 0, '(4 − 2)² = 4; 3 × 4 = 12; 5 + 12 = 17.'],
        ['¿Cuánto es 3/5 de 45?', ['9', '27', '75', '15'], 1, '45 ÷ 5 = 9; 9 × 3 = 27.'],
        ['¿Cuánto es 1/3 + 1/4?', ['2/7', '7/12', '1/12', '1/7'], 1, '4/12 + 3/12 = 7/12.'],
        ['2⁵ ÷ 2³ es igual a:', ['2', '4', '8', '16'], 1, 'Se restan exponentes: 2² = 4.'],
        ['Resuelve: 4x − 7 = 2x + 9', ['x = 1', 'x = 8', 'x = 16', 'x = 4'], 1, '2x = 16 → x = 8.'],
        ['Dos números suman 30 y uno es el cuádruple del otro. ¿Cuál es el mayor?', ['6', '20', '24', '25'], 2, 'x + 4x = 30 → x = 6; el mayor es 24.'],
        ['Si x + y = 9 y x − y = 3, ¿cuánto vale y?', ['3', '6', '4', '12'], 0, 'Sumando: 2x = 12 → x = 6; y = 9 − 6 = 3.'],
        ['Hipotenusa de un triángulo con catetos 9 y 12:', ['15', '21', '13', '108'], 0, '81 + 144 = 225; √225 = 15.'],
        ['Área de un círculo de radio 3 (usa π ≈ 3,14):', ['9,42', '18,84', '28,26', '6,28'], 2, '3,14 × 9 = 28,26.'],
        ['Promedio de 2, 5, 5, 8 y 10:', ['5', '6', '7', '30'], 1, '30 ÷ 5 = 6.'],
        ['Se lanza una moneda tres veces. ¿Probabilidad de tres caras?', ['1/3', '1/6', '1/8', '3/8'], 2, '1/2 × 1/2 × 1/2 = 1/8.'],
        ['Un producto de $50.000 sube 10 % y luego baja 10 %. Precio final:', ['$50.000', '$49.500', '$50.500', '$45.000'], 1, '50.000 × 1,1 × 0,9 = 49.500.'],
      ]),
    ] },
  ],
});

for (const c of [finanzas, escritura, mates]) await publish(H, c);
