// Curso gratis «Minecraft: ¿cuánto sabes?» de GEMS Abierto: la trivia del canal «Quiz de Bloques» convertida en un
// curso corto (dato clave + preguntas con explicación). Es un curso como cualquier otro: no cambia el modelo del LMS.
// Si ya existe, se actualiza conservando el avance. Uso: DEV_PASSWORD=... node seed/open-bloques.mjs
import { pathToFileURL } from 'node:url';
import { build, exam, img, lesson, login, publish } from './lib/open-lib.mjs';

export const bloques = build({
  title: 'Minecraft: ¿cuánto sabes?',
  description: '¿Sabías que los creepers le huyen a los gatos? Cinco módulos de trivia entre jugadores: mobs, Nether y End, crafteos, redstone y supervivencia. Lee el dato, responde y mira cuántas aciertas.',
  difficulty: 'beginner', tags: ['Minecraft', 'Trivia', 'Gratis'],
  thumbnailUrl: img('1550745165-9bc0b252726f'),
  modules: [
    { title: 'Mobs', description: 'Quién ataca, quién huye y quién se quema.', lessons: [
      lesson('Conoce a tus vecinos', `# Conoce a tus vecinos

Cada mob tiene una regla que casi todos aprenden a golpes. Si la sabes de antemano, te ahorras sustos y corazones.

## Datos clave

- **Creepers:** les tienen miedo a los gatos y a los ocelotes. Un gato cerca es el mejor guardaespaldas de tu casa.
- **Enderman:** se enfurece si lo miras a los ojos. Mira a sus pies o ponte una calabaza tallada en la cabeza.
- **Aldeano zombi:** se puede curar con una poción de debilidad y una manzana de oro.
- **Gólem de hierro:** protege la aldea y no te ataca, salvo que lo ataques tú primero.
- **Esqueletos y zombis:** se prenden fuego con la luz del sol.
- **Wither:** se invoca con 4 bloques de arena de almas y 3 cráneos de wither.

## Errores comunes

- Pegarle al gólem «para probar»: después te persigue.
- Mirar de frente a un enderman para «verlo mejor».
- Salir de día esperando que los zombis ya no estén: los que están bajo techo no se queman.`, [
        ['¿De qué mob huyen los creepers?', ['De las vacas', 'De los gatos y ocelotes', 'De los lobos', 'De las gallinas'], 1, 'Los creepers se alejan de gatos y ocelotes. Un gato en casa te protege.'],
        ['¿Qué pasa si miras a los ojos a un enderman?', ['Se teletransporta lejos', 'Se vuelve amigable', 'Se enfurece y te ataca', 'Te da una perla'], 2, 'Mirarlo a los ojos lo enfurece. Si te toca verlo, mira hacia abajo.'],
        ['¿Cómo se cura a un aldeano zombi?', ['Con pan y agua', 'Con una poción de debilidad y una manzana de oro', 'Con una poción de curación y un hueso', 'Dejándolo al sol'], 1, 'Se le lanza una poción de debilidad y luego se le da una manzana de oro.'],
        ['¿Cuándo ataca un gólem de hierro al jugador?', ['Siempre que lo ve', 'Solo de noche', 'Solo si tú lo atacas', 'Cuando llevas hierro'], 2, 'El gólem defiende la aldea y no ataca al jugador salvo que lo ataques.'],
        ['¿Qué necesitas para invocar al wither?', ['4 bloques de arena de almas y 3 cráneos de wither', '4 bloques de hierro y una calabaza', '3 bloques de oro y 4 cráneos', '9 lingotes de netherite'], 0, 'Se arma en forma de T con 4 bloques de arena de almas y 3 cráneos de wither.'],
      ]),
    ] },
    { title: 'Nether y End', description: 'Portales, piglin, ghast y el dragón.', lessons: [
      lesson('Viaje a otras dimensiones', `# Viaje a otras dimensiones

El Nether y el End tienen sus propias reglas. Conocerlas te salva el inventario.

## Datos clave

- **Portal al Nether:** necesita como mínimo **10 bloques de obsidiana** (un marco de 4 por 5 sin las esquinas) y algo con qué encenderlo.
- **Distancia:** 1 bloque en el Nether equivale a 8 en el mundo normal. Por eso se viaja rápido por allá.
- **Piglin:** te atacan si no llevas puesta al menos una pieza de armadura de oro.
- **Ghast:** se vence devolviéndole su bola de fuego.
- **Portal del End:** su marco tiene 12 bloques; hay que poner un ojo de ender en cada uno.
- **Dragón:** los cristales del End lo curan, así que se destruyen primero.
- **Perla de ender:** al lanzarla te teletransporta, pero te cuesta 5 de daño (2,5 corazones).

## Dato curioso

Los élitros, las alas para planear, solo se encuentran en las ciudades del End.

## Errores comunes

- Llevar una cama al Nether o al End: allí explota.
- Confiar en la brújula en el Nether: gira sin rumbo.`, [
        ['¿Cuántos bloques de obsidiana necesitas como mínimo para un portal al Nether?', ['4', '10', '14', '20'], 1, 'El marco mínimo, sin las esquinas, usa 10 bloques de obsidiana.'],
        ['¿Qué pasa si usas una cama en el Nether o en el End?', ['Cambia la hora', 'Explota', 'Te cura', 'Guarda tu punto de reaparición'], 1, 'En esas dimensiones la cama explota al intentar dormir.'],
        ['¿Cuántos bloques del mundo normal equivale a 1 bloque en el Nether?', ['2', '4', '8', '16'], 2, '1 bloque en el Nether = 8 en el mundo normal.'],
        ['¿Cómo se vence a un ghast con astucia?', ['Escondiéndote bajo tierra', 'Devolviéndole su bola de fuego', 'Dándole oro', 'Mirándolo a los ojos'], 1, 'Pegarle a la bola de fuego la manda de vuelta y puede derrotarlo.'],
        ['¿Dónde se encuentran los élitros?', ['En las aldeas', 'En los templos del desierto', 'En las ciudades del End', 'En las fortalezas del Nether'], 2, 'Los élitros están en las ciudades del End.'],
      ]),
    ] },
    { title: 'Crafteos y recursos', description: 'Cuántos materiales necesitas de verdad.', lessons: [
      lesson('Recetas que debes saberte', `# Recetas que debes saberte

Casi todo en Minecraft empieza con madera. Saber las cantidades de memoria te ahorra viajes.

## Datos clave

- **1 tronco** da **4 tablones**.
- **Mesa de crafteo:** 4 tablones.
- **Cofre:** 8 tablones.
- **Antorcha:** 1 carbón y 1 palo.
- **Horno:** 8 piedras labradas (adoquines).
- **Cama:** 3 lanas y 3 tablones.
- **Bloque de hierro:** 9 lingotes de hierro.
- **Pico de diamante:** 3 diamantes y 2 palos.

## Dato curioso

Un lingote de netherite se hace con 4 chatarras de netherite y 4 lingotes de oro. Y el equipo de diamante se mejora a netherite en la mesa de herrería.

## Errores comunes

- Gastar los 3 diamantes sin tener los palos listos.
- Creer que el netherite se craftea desde cero: parte de equipo de diamante.`, [
        ['¿Cuántos tablones da 1 tronco?', ['1', '2', '4', '8'], 2, 'Un tronco rinde 4 tablones.'],
        ['¿Con cuántos tablones se hace un cofre?', ['4', '6', '8', '9'], 2, 'El cofre lleva 8 tablones alrededor de un hueco vacío.'],
        ['¿Qué lleva una antorcha?', ['Carbón y un palo', 'Hierro y un palo', 'Piedra y un palo', 'Madera y lana'], 0, 'Un carbón arriba y un palo debajo.'],
        ['¿Cuántos lingotes de hierro hacen un bloque de hierro?', ['3', '6', '9', '12'], 2, 'Se llena toda la mesa de crafteo: 9 lingotes.'],
        ['¿Qué lleva un pico de diamante?', ['2 diamantes y 3 palos', '3 diamantes y 2 palos', '3 diamantes y 1 palo', '5 diamantes'], 1, 'Tres diamantes arriba y dos palos en el centro.'],
      ]),
    ] },
    { title: 'Redstone', description: 'Pistones, señales y relojes.', lessons: [
      lesson('La electricidad del juego', `# La electricidad del juego

La redstone es el cableado de Minecraft. Con unas pocas reglas ya haces puertas secretas y granjas.

## Datos clave

- **Alcance:** la señal de redstone viaja 15 bloques. Para ir más lejos se refuerza con un **repetidor**.
- **Pistón:** empuja hasta 12 bloques a la vez.
- **Pistón pegajoso:** lleva una bola de slime y además jala el bloque al retraerse.
- **Observador:** detecta cuando cambia el bloque que mira y emite un pulso.
- **Antorcha de redstone:** invierte la señal; encendida pasa a apagada y al revés.

## Dato curioso

Con observadores y pistones se hacen granjas automáticas de cultivos, porque el observador avisa apenas algo cambia.

## Errores comunes

- Hacer una línea de redstone de 20 bloques sin repetidor: la señal muere.
- Mover 13 bloques con un solo pistón: no se mueve nada.`, [
        ['¿Cuántos bloques puede empujar un pistón?', ['6', '9', '12', '16'], 2, 'El límite de un pistón es de 12 bloques.'],
        ['¿Qué lleva el pistón pegajoso además de un pistón?', ['Una bola de slime', 'Un lingote de hierro', 'Un cubo de agua', 'Pólvora'], 0, 'Se hace con un pistón y una bola de slime.'],
        ['¿Qué hace el observador?', ['Detecta cambios en el bloque que mira', 'Empuja bloques', 'Cuenta mobs cercanos', 'Mide la hora'], 0, 'Emite un pulso cada vez que el bloque observado cambia.'],
        ['¿Cuántos bloques recorre la señal de redstone antes de apagarse?', ['5', '10', '15', '30'], 2, 'La señal llega a 15 bloques; el repetidor la refuerza.'],
        ['¿Qué hace una antorcha de redstone en un circuito?', ['Amplifica la señal', 'Invierte la señal', 'Mide distancia', 'Cuenta pulsos'], 1, 'Si recibe señal se apaga, y si no la recibe, se enciende.'],
      ]),
    ] },
    { title: 'Supervivencia', description: 'Sobrevive la primera semana.', lessons: [
      lesson('Reglas para no morir', `# Reglas para no morir

La supervivencia se gana con hábitos, no con suerte. Estos son los que más vidas salvan.

## Datos clave

- **El día** dura 20 minutos reales. Aprovecha la luz para minar y construir.
- **Phantoms:** aparecen si pasas 3 noches sin dormir. Una cama los evita.
- **Agua:** dos fuentes de agua dejan una fuente infinita.
- **Lava y agua:** si el agua toca lava fuente, se forma obsidiana; si toca lava que corre, se forma piedra labrada.
- **Caídas:** caer sobre agua amortigua el golpe.

## Errores comunes

- **Cavar justo debajo de ti:** puedes caer en una cueva o en lava.
- Tapar un bloque de lava sin saber si es fuente o corriente.
- Dejar de dormir «porque no hace falta»: a la tercera noche llegan los phantoms.`, [
        ['¿Cuánto dura un día completo de Minecraft en tiempo real?', ['10 minutos', '20 minutos', '30 minutos', '1 hora'], 1, 'Un ciclo de día y noche dura 20 minutos reales.'],
        ['¿Qué pasa si pasas 3 noches sin dormir?', ['Pierdes hambre', 'Aparecen phantoms', 'Se acaba el día', 'Los creepers te ignoran'], 1, 'Tras 3 noches sin dormir, los phantoms empiezan a atacarte desde el cielo.'],
        ['¿Cómo se hace una fuente de agua infinita?', ['Con dos fuentes de agua juntas', 'Con un balde de lava', 'Con tres antorchas', 'Con un cubo y arena'], 0, 'Dos fuentes de agua dejan un hueco en medio que se rellena solo.'],
        ['¿Qué se forma cuando el agua toca lava fuente?', ['Piedra labrada', 'Obsidiana', 'Vidrio', 'Arena'], 1, 'Lava fuente más agua da obsidiana; lava que corre da piedra labrada.'],
        ['¿Qué te conviene hacer si caes desde muy alto y ves agua abajo?', ['Nada, igual mueres', 'Caer sobre el agua para amortiguar', 'Poner una cama', 'Saltar de lado'], 1, 'El agua amortigua la caída: apunta a ella.'],
      ]),
      exam('Reto final: ¿eres un crack?', 'Reto final', [
        ['Los creepers huyen de:', ['Los lobos', 'Los gatos', 'Las ovejas', 'Los cerdos'], 1, 'Gatos y ocelotes los espantan.'],
        ['¿Qué ocurre si te acercas a un piglin sin llevar nada de oro?', ['Te ofrece comida', 'Te ataca', 'Te ignora', 'Se vuelve aldeano'], 1, 'Si no llevas al menos una pieza de armadura de oro, te atacan.'],
        ['¿Cuál es el mínimo de bloques para un portal al Nether?', ['10 de obsidiana', '4 de obsidiana', '14 de piedra', '12 de hierro'], 0, 'Mínimo 10 bloques de obsidiana.'],
        ['¿Qué lleva una mesa de crafteo?', ['2 tablones', '4 tablones', '6 tablones', '8 tablones'], 1, 'Una mesa de crafteo se hace con 4 tablones.'],
        ['¿Qué hace el repetidor de redstone?', ['Refuerza la señal', 'Empuja bloques', 'Invierte la señal', 'Detecta cambios'], 0, 'La señal llega a 15 bloques; el repetidor la refuerza para ir más lejos.'],
        ['¿Qué haces para no encontrarte con phantoms?', ['Dormir de vez en cuando', 'Cavar profundo', 'Llevar oro', 'Tener un gato'], 0, 'Dormir reinicia el conteo de noches sin descanso.'],
        ['¿Qué hace la brújula en el Nether?', ['Apunta al portal', 'Gira sin rumbo', 'Apunta al norte', 'Se rompe'], 1, 'En el Nether la brújula gira sin rumbo fijo.'],
        ['¿Cuántos marcos tiene el portal del End?', ['8', '10', '12', '16'], 2, 'Son 12 marcos y cada uno lleva un ojo de ender.'],
      ], 60),
    ] },
  ],
});

// Solo publica si se ejecuta directamente (así se puede importar `bloques` para validarlo sin tocar la API).
if (process.argv[1] && import.meta.url === pathToFileURL(process.argv[1]).href) {
  const H = await login();
  await publish(H, bloques);
}
