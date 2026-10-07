// Crea la institución pública «GEMS Abierto» (cursos gratis sin registro) y el curso de admisión.
// Idempotente: si ya existen, actualiza el curso conservando el avance. Uso: DEV_PASSWORD=... node seed/open-campus.mjs
import { BASE, OPEN, build, exam, img, lesson, login, publish } from './lib/open-lib.mjs';

const H = await login();

const inst = await fetch(`${BASE}/institutions`, { method: 'POST', headers: H, body: JSON.stringify({
  id: OPEN, name: 'GEMS Abierto', type: 'academy', status: 'active',
  metadata: { description: 'Cursos gratis y abiertos para aprender lo que necesitas, a tu ritmo.', contactEmail: 'info@gemsinnovations.com', maxUsers: 10000000, subscriptionType: 'enterprise' },
  branding: { type: 'logo-text', colorPrimary: '#5B4EC1', colorSecondary: '#1E1B4B', darkMode: true },
}) });
console.log('institución', OPEN, '->', inst.status === 201 ? 'creada' : `ya existía (${inst.status})`);

const SUENO = 'Durante el sueño profundo, el cerebro repasa lo aprendido en el día y fortalece las conexiones que considera importantes. Por eso, varios estudios han encontrado que quienes duermen bien la noche anterior a un examen recuerdan más que quienes pasan la noche estudiando. Esto no significa que estudiar sea inútil: sin estudio no hay nada que consolidar. Significa que el descanso es parte del aprendizaje, no una pausa en él.';

const BICIS = 'En muchas ciudades, las bicicletas compartidas se presentaron como la solución al tráfico. La idea es atractiva: cada viaje en bicicleta sería un carro menos en la calle. Sin embargo, los datos de varios sistemas muestran que buena parte de los usuarios no dejó el carro, sino el bus o la caminata. Esto no vuelve inútiles a las bicicletas: reducen la contaminación de los trayectos cortos y mejoran la salud de quienes las usan. Pero sugiere que, por sí solas, difícilmente desatascan una ciudad. Para que el tráfico baje, la bicicleta tiene que ir acompañada de transporte público frecuente y de calles pensadas para quien no maneja.';

const course = build({
  title: 'Prepárate para el examen de admisión: razonamiento lógico y lectura',
  description: 'Practica las dos habilidades que más piden los exámenes de admisión a la universidad: razonamiento lógico-matemático y comprensión de lectura. Cada lección explica el tema, resuelve un ejemplo paso a paso, señala los errores más comunes y termina con ejercicios comentados. Al final, un simulacro para medir tu avance.',
  difficulty: 'beginner', tags: ['Admisión', 'Razonamiento lógico', 'Lectura crítica', 'Gratis'],
  thumbnailUrl: img('1434030216411-0b793f4b4173'),
  modules: [
    { title: 'Razonamiento lógico', description: 'Porcentajes, proporciones, secuencias y deducciones.', lessons: [
      lesson('Antes de empezar', `# Cómo sacarle provecho a este curso

Los exámenes de admisión no miden cuánto memorizaste, sino **qué tan bien razonas con poca información y poco tiempo**. Por eso este curso combina dos bloques:

1. **Razonamiento lógico-matemático:** porcentajes, proporciones, secuencias y deducciones. Son problemas que se resuelven con aritmética básica y orden, no con fórmulas avanzadas.
2. **Comprensión de lectura:** identificar la idea principal, hacer inferencias, entender palabras por el contexto y evaluar argumentos.

## Cómo estudiar

- **Una lección al día, unos 15 minutos.** La constancia vale más que una maratón el fin de semana.
- **Lee la explicación de cada respuesta**, también de las que aciertas: a veces se acierta por intuición y conviene saber por qué.
- **Haz los ejercicios sin calculadora.** En la mayoría de exámenes no la dejan usar.
- **Si fallas una práctica, repítela al día siguiente.** Puedes intentarlo las veces que quieras.

## Qué vas a lograr

Al terminar sabrás atacar los tipos de pregunta más frecuentes, tendrás un método para cada uno y habrás hecho un simulacro completo con retroalimentación.

> Las preguntas son originales, construidas para practicar el tipo de razonamiento que evalúan estas pruebas. GEMS no está afiliado a ninguna universidad: confirma siempre la estructura de tu examen en la guía oficial de la institución a la que te presentas.`, null, 'Cómo usar este curso'),

      lesson('Porcentajes y descuentos', `# Porcentajes y descuentos

Un **porcentaje** es una fracción con denominador 100. El 15 % de algo es 15/100 de ese algo, es decir, **0,15 veces** ese algo.

## Tres atajos que ahorran tiempo

- **10 %:** corre la coma un lugar. El 10 % de 360 es 36.
- **5 %:** la mitad del 10 %. El 5 % de 360 es 18.
- **25 %, 50 %, 75 %:** un cuarto, la mitad y tres cuartos.

Con esos bloques armas casi todo: el 15 % de 360 es 36 + 18 = **54**.

## Ejemplo resuelto

*Un celular cuesta $800.000 y tiene 15 % de descuento. ¿Cuánto se paga?*

1. Si descuentan el 15 %, pagas el **85 %**.
2. 800.000 × 0,85 = **680.000**.

Pensar en "lo que queda" (85 %) evita un paso: no hay que calcular el descuento y luego restarlo.

## Aumentos y descuentos seguidos

Los porcentajes seguidos **se multiplican, no se suman**. Si un precio sube 20 % y luego baja 20 %:

1,20 × 0,80 = 0,96 → el precio final es el **96 %** del original, es decir, **4 % menos**.

## ¿Qué porcentaje es?

Para saber qué porcentaje es una parte del total: **parte ÷ total × 100**. Si 12 de 30 estudiantes son mujeres: 12 ÷ 30 = 0,4 = **40 %**.

## Errores comunes

- Sumar porcentajes seguidos ("sube 20 y baja 20, queda igual"). Falso: se multiplican.
- Calcular el porcentaje sobre el número equivocado. "¿Qué porcentaje de 30 es 12?": el total es 30.
- Olvidar estimar. Si el descuento es pequeño, la respuesta debe estar cerca del precio original.`, [
        ['Un celular cuesta $800.000 y tiene un descuento del 15 %. ¿Cuánto se paga?', ['$680.000', '$785.000', '$720.000', '$650.000'], 0, 'Con 15 % de descuento pagas el 85 %: 800.000 × 0,85 = 680.000.'],
        ['¿Cuánto es el 15 % de 360?', ['36', '54', '45', '60'], 1, '10 % = 36 y 5 % = 18; juntos, 54.'],
        ['Un precio sube 20 % y luego baja 20 %. Comparado con el original, el precio final es:', ['Igual', '4 % más bajo', '4 % más alto', '20 % más bajo'], 1, '1,20 × 0,80 = 0,96: queda en el 96 % del original, 4 % más bajo.'],
        ['En un grupo hay 12 mujeres y 18 hombres. ¿Qué porcentaje del grupo son mujeres?', ['40 %', '12 %', '66 %', '60 %'], 0, 'Son 12 de 30 personas: 12 ÷ 30 = 0,4 = 40 %.'],
        ['Una camisa con 30 % de descuento cuesta $56.000. ¿Cuál era su precio original?', ['$72.800', '$80.000', '$86.000', '$73.000'], 1, '$56.000 es el 70 % del precio: 56.000 ÷ 0,70 = 80.000.'],
        ['Un salario de $2.000.000 sube 10 % un año y 10 % el siguiente. ¿Cuánto queda?', ['$2.400.000', '$2.420.000', '$2.200.000', '$2.410.000'], 1, '2.000.000 × 1,1 × 1,1 = 2.420.000. El segundo aumento se calcula sobre el salario ya aumentado.'],
      ]),

      lesson('Proporciones y regla de tres', `# Proporciones y regla de tres

Dos cantidades son **proporcionales** cuando cambian juntas a un ritmo fijo. Antes de calcular, pregúntate siempre: **si una sube, ¿la otra sube o baja?**

## Proporción directa: suben juntas

Más kilos de arroz cuestan más; más horas trabajadas, más pago.

*Si 3 kilos cuestan $10.500, ¿cuánto cuestan 5 kilos?*

1. Un kilo cuesta 10.500 ÷ 3 = 3.500.
2. Cinco kilos: 3.500 × 5 = **17.500**.

Reducir a la unidad (cuánto vale **uno**) es el método más seguro y no requiere recordar dónde va cada número.

## Proporción inversa: una sube, la otra baja

Más personas haciendo el mismo trabajo tardan menos; más velocidad, menos tiempo.

*Si 3 personas pintan una pared en 6 horas, ¿cuánto tardan 9 personas?*

1. El trabajo total es 3 × 6 = **18 horas-persona**.
2. Con 9 personas: 18 ÷ 9 = **2 horas**.

En la inversa se **multiplica** primero para hallar el total y luego se divide.

## Escalas

Un mapa a escala 1:50.000 significa que 1 cm en el mapa son 50.000 cm reales, es decir, **500 m**. Así, 4 cm en el mapa son 2 km.

## Errores comunes

- Aplicar regla de tres directa a un problema inverso. Si la respuesta dice que 9 personas tardan 18 horas, algo salió mal: más gente debería tardar menos.
- Mezclar unidades: minutos con horas, metros con kilómetros. Conviértelas antes de operar.
- No verificar con sentido común al final.`, [
        ['Si 3 kilos de arroz cuestan $10.500, ¿cuánto cuestan 5 kilos?', ['$15.500', '$17.500', '$16.000', '$21.000'], 1, 'Un kilo cuesta 3.500; cinco kilos, 17.500.'],
        ['Si 3 personas pintan una pared en 6 horas, ¿cuánto tardan 9 personas trabajando al mismo ritmo?', ['18 horas', '2 horas', '3 horas', '4 horas'], 1, 'El trabajo total es 3 × 6 = 18 horas-persona. Con 9 personas: 18 ÷ 9 = 2 horas.'],
        ['Un carro a 60 km/h tarda 4 horas en un trayecto. ¿Cuánto tarda a 80 km/h?', ['3 horas', '5 horas', '3,5 horas', '5,3 horas'], 0, 'La distancia es 60 × 4 = 240 km; a 80 km/h: 240 ÷ 80 = 3 horas. Es proporción inversa.'],
        ['En un mapa a escala 1:50.000, dos pueblos están a 6 cm. ¿Cuál es la distancia real?', ['300 m', '3 km', '30 km', '6 km'], 1, '6 × 50.000 = 300.000 cm = 3.000 m = 3 km.'],
        ['Una receta para 4 personas lleva 300 g de harina. ¿Cuánta harina se necesita para 10 personas?', ['600 g', '700 g', '750 g', '1.200 g'], 2, 'Por persona son 75 g; para 10, 750 g. Es proporción directa.'],
        ['Una llave llena un tanque en 12 horas. ¿Cuánto tardan 3 llaves iguales abiertas a la vez?', ['36 horas', '4 horas', '6 horas', '9 horas'], 1, 'Más llaves, menos tiempo: 12 ÷ 3 = 4 horas.'],
      ]),

      lesson('Secuencias numéricas', `# Secuencias numéricas

En una secuencia, cada término sigue una regla. Tu trabajo es **descubrir qué cambia de un término al siguiente**. Prueba en este orden:

1. **Diferencias:** resta cada término del siguiente.
2. **Cocientes:** divide cada término entre el anterior.
3. **Patrones conocidos:** cuadrados (1, 4, 9, 16…), cubos (1, 8, 27…), primos (2, 3, 5, 7, 11…).
4. **Dos secuencias intercaladas:** mira los términos en posiciones pares e impares por separado.

## Ejemplo resuelto 1: diferencias que crecen

*2, 6, 12, 20, 30, …*

- Diferencias: 4, 6, 8, 10. Crecen de 2 en 2.
- La siguiente diferencia es 12: 30 + 12 = **42**.

## Ejemplo resuelto 2: se multiplica

*3, 6, 12, 24, …*

- Las diferencias (3, 6, 12) no son constantes, pero cada término es el **doble** del anterior.
- Sigue 24 × 2 = **48**.

## Ejemplo resuelto 3: intercaladas

*1, 10, 2, 20, 3, 30, …*

- Posiciones impares: 1, 2, 3. Posiciones pares: 10, 20, 30.
- El siguiente término es impar: **4**.

## Secuencias con letras

Convierte cada letra en su posición del alfabeto (A = 1, B = 2…) y busca la regla con números. En A, C, F, J… las posiciones son 1, 3, 6, 10: las diferencias crecen 2, 3, 4, así que sigue 15, que es la **O**.

## Errores comunes

- Quedarse con la primera regla que parece funcionar sin comprobarla con **todos** los términos.
- Olvidar que puede haber dos reglas intercaladas.
- Contar mal el alfabeto: en estos ejercicios no se suele usar la Ñ, pero confírmalo en el enunciado.`, [
        ['¿Qué número sigue? 2, 6, 12, 20, 30, …', ['40', '42', '44', '36'], 1, 'Las diferencias son 4, 6, 8, 10; la siguiente es 12: 30 + 12 = 42.'],
        ['¿Qué número sigue? 3, 6, 12, 24, …', ['36', '30', '48', '42'], 2, 'Cada término es el doble del anterior: 24 × 2 = 48.'],
        ['¿Qué número sigue? 1, 4, 9, 16, …', ['20', '25', '24', '32'], 1, 'Son los cuadrados 1², 2², 3², 4²; sigue 5² = 25.'],
        ['¿Qué número sigue? 1, 10, 2, 20, 3, 30, …', ['40', '4', '31', '5'], 1, 'Son dos secuencias intercaladas: 1, 2, 3… y 10, 20, 30…; toca la primera: 4.'],
        ['¿Qué número sigue? 81, 27, 9, 3, …', ['0', '1', '1,5', '6'], 1, 'Cada término es la tercera parte del anterior: 3 ÷ 3 = 1.'],
        ['¿Qué letra sigue? A, C, F, J, …', ['M', 'N', 'O', 'P'], 2, 'Posiciones 1, 3, 6, 10: diferencias 2, 3, 4; la siguiente es 5, posición 15 = O.'],
      ]),

      lesson('Deducciones y orden lógico', `# Deducciones y orden lógico

En estas preguntas no hay que calcular, sino **sacar conclusiones seguras** a partir de lo que dice el enunciado, ni más ni menos.

## Conjuntos: dibuja

Las frases "todos", "algunos" y "ningún" se entienden mejor con círculos:

- **Todos los A son B:** el círculo A está dentro del círculo B.
- **Ningún B es C:** los círculos B y C no se tocan.
- **Algunos A son C:** los círculos A y C se cruzan.

*Si todos los A son B y ningún B es C, entonces…* A está dentro de B, y B no toca a C. Por tanto, **ningún A es C**.

## Orden: haz una fila

*Ana es mayor que Luis. Carla es menor que Luis. Diego es mayor que Ana.*

Ordena de mayor a menor: **Diego > Ana > Luis > Carla**. La menor es Carla; el mayor, Diego.

## Calendario

Los días se repiten cada 7. Para saber qué día será dentro de N días, divide N entre 7 y usa el **residuo**. Dentro de 10 días: 10 = 7 + 3, así que avanzas 3 días.

## "Si… entonces…"

"Si llueve, la calle se moja" **no** permite concluir que "si la calle está mojada, llovió": pudo ser una manguera. Lo que sí se puede concluir es que "si la calle no está mojada, no llovió".

## Errores comunes

- Concluir algo posible como si fuera seguro. "Algunos" no es "todos".
- Agregar información que no está en el enunciado.
- Invertir un "si… entonces…".`, [
        ['Si todos los A son B y ningún B es C, entonces:', ['Ningún A es C', 'Algunos A son C', 'Todos los C son A', 'No se puede saber'], 0, 'Todo A está dentro de B, y B no comparte nada con C; por eso ningún A puede ser C.'],
        ['Ana es mayor que Luis, Carla es menor que Luis y Diego es mayor que Ana. ¿Quién es el mayor?', ['Ana', 'Luis', 'Diego', 'No se puede saber'], 2, 'De mayor a menor: Diego, Ana, Luis, Carla.'],
        ['Si hoy es martes, ¿qué día será dentro de 10 días?', ['Jueves', 'Viernes', 'Sábado', 'Miércoles'], 1, 'En 7 días vuelve a ser martes; 3 días más: viernes.'],
        ['"Todos los estudiantes de grado 11 presentan el examen. Sara presenta el examen." ¿Qué se puede concluir?', ['Sara está en grado 11', 'Sara no está en grado 11', 'No se puede saber si Sara está en grado 11', 'Nadie más presenta el examen'], 2, 'También pueden presentarlo personas que no están en grado 11; no hay información suficiente.'],
        ['"Si estudio, apruebo." Sabemos que no aprobé. Entonces:', ['Estudié', 'No estudié', 'Pude haber estudiado o no', 'El examen era difícil'], 1, 'Si hubiera estudiado, habría aprobado. Como no aprobé, no estudié.'],
        ['Algunos músicos son profesores y todos los profesores son pacientes. ¿Qué es seguro?', ['Todos los músicos son pacientes', 'Algunos músicos son pacientes', 'Ningún músico es paciente', 'Todos los pacientes son músicos'], 1, 'Los músicos que son profesores también son pacientes, así que al menos algunos músicos lo son.'],
      ]),
    ] },

    { title: 'Comprensión de lectura', description: 'Idea principal, inferencias, vocabulario en contexto y argumentos.', lessons: [
      lesson('Idea principal e inferencias', `# Idea principal e inferencias

Casi todas las preguntas de lectura se responden con tres preguntas que conviene hacerse **mientras** lees, no después:

1. **¿De qué habla?** (el tema) y **¿qué dice de eso?** (la idea principal).
2. **¿Qué se deduce aunque no lo diga?** (la inferencia).
3. **¿Para qué lo escribió el autor?** (el propósito: explicar, convencer, narrar, criticar).

## Cómo reconocer la idea principal

La idea principal **resume todo el texto**. Para comprobarlo, pregúntate si cada párrafo apoya esa idea. Descarta las opciones que:

- solo mencionan un **detalle**;
- **exageran** lo que dice el texto ("siempre", "nunca", "es inútil");
- dicen algo verdadero pero que **no está en el texto**.

## Qué es una buena inferencia

Una inferencia es una conclusión que **se sigue necesariamente** de lo leído. Si el texto dice que quienes duermen bien recuerdan más, se infiere que quien no durmió recordará menos de lo esperado. No se infiere que dormir reemplace al estudio.

## Texto para practicar

${SUENO}

## Ejemplo resuelto

*¿Cuál es la idea principal?* La última frase lo resume: el descanso es parte del aprendizaje. Las opciones "estudiar de noche es inútil" o "dormir reemplaza al estudio" exageran; el mismo texto las descarta con "esto no significa que estudiar sea inútil".`, [
        [`${SUENO}\n\n¿Cuál es la idea principal del texto?`, ['El descanso es parte del proceso de aprender', 'Estudiar de noche es inútil', 'Dormir reemplaza al estudio', 'Los exámenes deberían ser en la mañana'], 0, 'El texto concluye que el descanso es parte del aprendizaje. Las otras opciones exageran o no aparecen en el texto.'],
        ['Según el texto del sueño, un estudiante que pasa toda la noche estudiando antes de un examen probablemente:', ['Recordará más que quien durmió bien', 'Recordará menos de lo que esperaba', 'No necesitaba estudiar', 'Aprobará con seguridad'], 1, 'El texto dice que quienes duermen bien recuerdan más que quienes pasan la noche estudiando.'],
        ['En el texto, la palabra «consolidar» significa:', ['Olvidar lo aprendido', 'Fijar y reforzar lo aprendido', 'Empezar a estudiar', 'Resumir un texto'], 1, 'El cerebro fortalece las conexiones de lo aprendido: consolidar es fijarlo y reforzarlo.'],
        ['¿Cuál es el propósito principal del autor?', ['Explicar por qué el descanso ayuda a aprender', 'Criticar a los estudiantes', 'Vender un método de estudio', 'Describir cómo funciona un examen'], 0, 'El texto explica, con estudios como apoyo, la relación entre el sueño y la memoria.'],
        ['La frase «sin estudio no hay nada que consolidar» implica que:', ['El sueño ayuda solo si antes se estudió', 'Dormir es más importante que estudiar', 'Estudiar impide dormir bien', 'El estudio no sirve'], 0, 'El sueño refuerza lo aprendido; si no se estudió, no hay nada que reforzar.'],
      ]),

      lesson('Vocabulario en contexto y postura del autor', `# Vocabulario en contexto y postura del autor

## Palabras que no conoces

En el examen no hay diccionario, pero el texto casi siempre da pistas. Para adivinar el significado de una palabra:

1. Lee la oración completa y la siguiente.
2. Busca **contrastes** ("sin embargo", "pero") o **explicaciones** ("es decir", "por eso").
3. **Reemplaza** la palabra por cada opción y quédate con la que mantiene el sentido.

## La postura del autor

Pregúntate si el autor está **a favor**, **en contra** o si es **matizado** (reconoce ventajas y límites). Las palabras de contraste ("sin embargo", "pero", "esto no significa que") suelen marcar dónde está su opinión.

## Texto para practicar

${BICIS}

## Ejemplo resuelto

*¿Qué significa "desatascan" en el texto?* La oración dice que las bicicletas "por sí solas, difícilmente desatascan una ciudad", justo después de hablar del tráfico. Si reemplazamos por "descongestionan", el sentido se mantiene: **liberan el tráfico**.

*¿Cuál es la postura del autor?* No dice que las bicicletas sean inútiles (lo aclara explícitamente) ni que sean la solución. Es una postura **matizada**: sirven, pero necesitan transporte público y calles adecuadas.

## Errores comunes

- Elegir el significado más común de una palabra en lugar del que tiene **en ese texto**.
- Confundir lo que dicen otros ("se presentaron como la solución") con lo que piensa el autor.`, [
        [`${BICIS}\n\nEn el texto, «desatascan» significa:`, ['Descongestionan', 'Ensucian', 'Construyen', 'Recorren'], 0, 'Habla del tráfico: desatascar una ciudad es descongestionarla.'],
        ['¿Cuál es la postura del autor sobre las bicicletas compartidas?', ['Son inútiles', 'Son la única solución al tráfico', 'Ayudan, pero no bastan por sí solas', 'Deberían prohibirse'], 2, 'Reconoce sus beneficios y aclara que solas difícilmente reducen el tráfico.'],
        ['Según el texto, muchos usuarios de bicicletas compartidas antes:', ['Manejaban carro', 'Usaban bus o caminaban', 'No salían de casa', 'Tenían bicicleta propia'], 1, 'Los datos muestran que buena parte dejó el bus o la caminata, no el carro.'],
        ['La expresión «Sin embargo» en la tercera oración introduce:', ['Un ejemplo', 'Una idea que contrasta con la anterior', 'Una conclusión final', 'Una definición'], 1, 'Contrasta la idea atractiva (menos carros) con lo que muestran los datos.'],
        ['¿Qué propone el autor para reducir el tráfico?', ['Más bicicletas compartidas', 'Bicicletas junto con buen transporte público y calles adecuadas', 'Subir el precio de la gasolina', 'Prohibir los carros'], 1, 'La última oración dice que la bicicleta debe ir acompañada de transporte público frecuente y calles pensadas para quien no maneja.'],
      ]),

      lesson('Argumentos y conclusiones', `# Argumentos y conclusiones

Un **argumento** tiene una **conclusión** (lo que se quiere demostrar) y **premisas** (las razones que la apoyan). Muchas preguntas piden identificar cuál es cuál o qué lo fortalece y qué lo debilita.

## Cómo encontrar la conclusión

Busca palabras como **"por tanto", "en consecuencia", "así que", "por eso"**. Lo que viene después suele ser la conclusión. Las razones suelen ir después de **"porque", "ya que", "dado que"**.

*"El colegio debería abrir la biblioteca los sábados, ya que muchos estudiantes no tienen un lugar tranquilo para estudiar en casa."*

- Conclusión: el colegio debería abrir la biblioteca los sábados.
- Premisa: muchos estudiantes no tienen un lugar tranquilo en casa.

## Fortalecer y debilitar

- **Fortalece** un dato que hace más probable la conclusión: "una encuesta muestra que 7 de cada 10 estudiantes irían".
- **Debilita** un dato que la hace menos probable: "la mayoría de estudiantes trabaja los sábados".

## Errores de razonamiento frecuentes

- **Generalizar** con pocos casos: "mis dos primos odian las matemáticas, así que a los jóvenes no les gustan".
- **Atacar a la persona** y no a su idea: "no le creas, él ni siquiera terminó el colegio".
- **Falsa causa:** "desde que compré esta camiseta gano los partidos".
- **Falso dilema:** presentar solo dos opciones cuando hay más.

## Errores comunes en el examen

- Elegir una opción que es verdadera pero **no afecta** al argumento.
- Confundir la conclusión con una de las premisas.`, [
        ['«Deberíamos dormir más antes de los exámenes, porque el sueño ayuda a fijar lo aprendido.» ¿Cuál es la conclusión?', ['El sueño ayuda a fijar lo aprendido', 'Deberíamos dormir más antes de los exámenes', 'Los exámenes son difíciles', 'Aprender requiere esfuerzo'], 1, 'Lo que se quiere demostrar va antes de «porque»; lo que sigue es la razón.'],
        ['«El colegio debería abrir la biblioteca los sábados, ya que muchos estudiantes no tienen un lugar tranquilo para estudiar.» ¿Qué dato fortalece el argumento?', ['La biblioteca tiene pocos libros', 'Una encuesta muestra que la mayoría de estudiantes iría los sábados', 'Los sábados hay partidos de fútbol', 'El colegio tiene cafetería'], 1, 'Muestra que abrir los sábados sí tendría uso, lo que apoya la conclusión.'],
        ['Para el mismo argumento de la biblioteca, ¿qué dato lo debilita?', ['La mayoría de estudiantes trabaja los sábados', 'Muchos estudiantes viven lejos', 'La biblioteca es cómoda', 'Hay exámenes pronto'], 0, 'Si casi nadie puede ir los sábados, abrir ese día tendría poco sentido.'],
        ['«Mis dos vecinos compraron ese carro y se les dañó. Esa marca es mala.» ¿Qué error comete?', ['Falso dilema', 'Generalizar con pocos casos', 'Atacar a la persona', 'Ninguno'], 1, 'Dos casos no bastan para concluir sobre toda una marca.'],
        ['«No le hagas caso a su propuesta de reciclaje: ni siquiera tiene carro.» ¿Qué error comete?', ['Atacar a la persona en vez de a la idea', 'Falsa causa', 'Generalizar', 'Ninguno, es un buen argumento'], 0, 'No discute la propuesta; descalifica a quien la hace con algo que no tiene relación.'],
      ]),
    ] },

    { title: 'Simulacro', description: 'Mide tu avance con preguntas de todo el curso.', lessons: [
      exam('Simulacro final', 'Simulacro de práctica', [
        ['¿Cuánto es el 25 % de 80?', ['20', '25', '32', '16'], 0, '25 % es un cuarto: 80 ÷ 4 = 20.'],
        ['Unos tenis de $240.000 tienen 25 % de descuento. ¿Cuánto se paga?', ['$180.000', '$215.000', '$190.000', '$60.000'], 0, 'Pagas el 75 %: 240.000 × 0,75 = 180.000.'],
        ['Si 4 obreros construyen un muro en 9 días, ¿cuánto tardan 6 obreros al mismo ritmo?', ['13,5 días', '6 días', '7 días', '5 días'], 1, 'Trabajo total: 4 × 9 = 36 días-obrero; con 6 obreros: 36 ÷ 6 = 6 días.'],
        ['¿Qué número sigue? 5, 10, 20, 35, …', ['50', '55', '45', '60'], 1, 'Las diferencias son 5, 10, 15; la siguiente es 20: 35 + 20 = 55.'],
        ['¿Qué número sigue? 2, 5, 11, 23, …', ['46', '47', '35', '44'], 1, 'Cada término es el doble del anterior más 1: 23 × 2 + 1 = 47.'],
        ['Ana es mayor que Luis y Luis es mayor que Carla. ¿Quién es la menor?', ['Ana', 'Luis', 'Carla', 'No se puede saber'], 2, 'Ana > Luis > Carla: la menor es Carla.'],
        ['Si hoy es jueves, ¿qué día será dentro de 15 días?', ['Jueves', 'Viernes', 'Sábado', 'Miércoles'], 1, '15 = 14 + 1: dos semanas exactas y un día más, viernes.'],
        ['Ningún reptil tiene plumas y todas las serpientes son reptiles. Entonces:', ['Algunas serpientes tienen plumas', 'Ninguna serpiente tiene plumas', 'Todos los reptiles son serpientes', 'No se puede saber'], 1, 'Las serpientes están dentro de los reptiles, y ningún reptil tiene plumas.'],
        ['En el texto sobre el sueño, la frase «sin estudio no hay nada que consolidar» implica que:', ['El sueño ayuda solo si antes se estudió', 'Dormir es más importante que estudiar', 'Estudiar impide dormir bien', 'El estudio no sirve'], 0, 'El sueño refuerza lo aprendido; si no se estudió, no hay nada que reforzar.'],
        ['En el texto sobre las bicicletas, el autor considera que las bicicletas compartidas:', ['No sirven para nada', 'Resuelven solas el tráfico', 'Tienen beneficios, pero no bastan solas', 'Solo sirven para hacer ejercicio'], 2, 'Reconoce que reducen contaminación y mejoran la salud, pero dice que solas difícilmente desatascan una ciudad.'],
        ['«Todos mis amigos aprobaron con ese profesor, así que es el mejor del colegio.» El argumento es débil porque:', ['Ataca a la persona', 'Generaliza a partir de pocos casos', 'Es un falso dilema', 'No tiene conclusión'], 1, 'Unos pocos amigos no bastan para afirmar que es el mejor de todo el colegio.'],
        ['En un mapa a escala 1:100.000, una carretera mide 7 cm. ¿Cuánto mide en realidad?', ['700 m', '7 km', '70 km', '0,7 km'], 1, '7 × 100.000 = 700.000 cm = 7.000 m = 7 km.'],
      ]),
    ] },
  ],
});

// Lecciones que cambiaron de nombre: conservan su id (y el avance de quien ya las hizo).
await publish(H, course, { renamed: {
  'Porcentajes y descuentos': 'Proporciones y porcentajes',
  'Secuencias numéricas': 'Secuencias y deducciones',
} });
