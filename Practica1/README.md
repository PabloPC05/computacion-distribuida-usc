# Práctica 1 — Chat multicast (UDP + hilos)

Chat de grupo para una red local, **descentralizado y sin servidor**: cada
programa envía y recibe directamente del grupo multicast `239.1.2.3`, puerto
`6000` (ambos fijados por el enunciado).

Funciona como una emisora de radio: lo que emite uno lo oyen todos los que
están sintonizados en la misma dirección y puerto.

## Requisitos

JDK 11 o superior. `String.isBlank()` es lo que marca ese mínimo. Desarrollado
y probado con JDK 24.0.2, sin avisos de compilación.

## Estructura

| Fichero | Papel |
|---|---|
| `src/App.java` | Punto de entrada. Pide el apodo y valida que no esté vacío ni contenga `:`. |
| `src/ChatClient.java` | Abre el socket, se une al grupo, lanza el hilo receptor y ejecuta el bucle de envío (hilo principal). |
| `src/ChatReceiver.java` | `implements Runnable`. Hilo secundario: recibe, filtra y muestra. |
| `src/Protocol.java` | El formato `NICKNAME:MENSAJE`: construir, validar, separar y formatear. No toca ni consola ni red. |
| `tests/ProtocolTest.java` | Pruebas del protocolo, sin framework. |

## Compilar

Desde esta carpeta (`Practica1`):

```powershell
javac -encoding UTF-8 -d bin src\*.java
```

El `-encoding UTF-8` no es opcional: los fuentes llevan tildes y `ñ` en los
comentarios y en los literales.

## Ejecutar

Una terminal por participante, cada uno con un apodo distinto:

```powershell
java -cp bin App
```

```
Introduce tu apodo:
Ana
Cliente de chat iniciado con el apodo: Ana
Escribe un mensaje y pulsa Enter. 'exit' para salir.
```

Lo que escriba Ana le aparecerá a los demás como `[Ana] dice: hola`, y **no**
se le repetirá a ella en su propia pantalla.

Para salir, escribir `exit`. Si la consola de Windows muestra mal las tildes o
la `ñ`, ejecutar antes `chcp 65001`.

## Pruebas

```powershell
javac -encoding UTF-8 -cp bin -d bin-tests tests\ProtocolTest.java
java -cp "bin;bin-tests" ProtocolTest
```

Imprime `Todas las pruebas correctas`, o el nombre de cada prueba fallida y
termina con código de salida 1.

**Prueba manual**: tres terminales con apodos Ana, Pedro y Lucas. Cada mensaje
debe aparecer en las otras dos y no duplicarse en quien lo envía.

### Resultados obtenidos

- `ProtocolTest`: todas correctas (apodos, delimitador, filtrado propio,
  formato, tildes).
- Tres participantes en la misma máquina: cada mensaje llegó a los otros dos,
  no se mostró al emisor, se conservaron `:` y `ñ`, se recibió mientras los
  demás esperaban entrada, y los tres procesos terminaron con `exit`.
- Repetido en **macOS con JDK 17** (tres procesos: Ana, Pedro y Lucas):
  compila sin avisos, `ProtocolTest` pasa, cada mensaje llegó a los otros dos,
  no se repitió en el emisor, se conservaron los `:` dentro del mensaje
  (`[Ana] dice: hola: son las 10:30`) y la `ñ` con tildes
  (`[Pedro] dice: qué tal, Ñandú`), y los tres terminaron con `exit`.
- **No probado entre equipos distintos de la red.** Si hay varias interfaces
  (VPN, adaptadores virtuales), puede hacer falta elegir la interfaz
  explícitamente en `joinGroup` (ver más abajo).

## Decisiones de diseño llamativas

Estas son las decisiones que no son obvias al leer el código, o que resultan
raras hasta que se conoce el motivo.

### El socket se cierra para matar el hilo receptor

No hay ninguna bandera `volatile boolean running`, ni `interrupt()`, ni hilos
*daemon*. El hilo receptor está bloqueado en `socket.receive()`, que no se
desbloquea con un `interrupt()`. La única forma de sacarlo de ahí es **cerrar
el socket por debajo**: eso hace que `receive()` lance `IOException` y el hilo
termine.

Por eso el `finally` de `ChatClient.start()` cierra el socket *antes* del
`join()`. Y por eso `ChatReceiver` comprueba `socket.isClosed()` dentro del
`catch`: si el socket está cerrado, esa excepción es la salida normal del
usuario y hay que callarse; si sigue abierto, entonces sí es un error de red de
verdad y se avisa. Sin esa comprobación, salir del chat imprimiría siempre un
error falso.

### Los mensajes propios se filtran por apodo, no por dirección de origen

En multicast, quien envía también recibe su propio datagrama. Hay que
descartarlo, y el enunciado pide hacerlo comparando el apodo. La comparación es
**exacta y distingue mayúsculas**: `Ana` y `ana` son dos personas distintas.

Esto tiene una consecuencia incómoda que conviene saber: el apodo no es una
identidad. Si dos participantes eligen el mismo apodo, **se silencian
mutuamente** (cada uno cree que los mensajes del otro son suyos). Y cualquiera
puede escribir mensajes haciéndose pasar por otro. Filtrar por dirección IP y
puerto de origen sería más robusto, pero el enunciado pide el apodo.

### El delimitador es asimétrico: el apodo no puede llevar `:`, el mensaje sí

El formato es `NICKNAME:MENSAJE` y `Protocol.parse` corta **solo en el primer
`:`**. Así `Ana:la hora es 10:30` da apodo `Ana` y mensaje `la hora es 10:30`,
con sus dos puntos intactos.

El precio es que el apodo no puede contener `:`, y por eso se valida al
introducirlo (`Protocol.isValidNickname`). Es la única restricción que se le
impone al usuario, y existe solo por el formato del protocolo.

### Los paquetes mal formados se descartan en silencio

`Protocol.parse` devuelve `null` en lugar de lanzar una excepción cuando el
texto no tiene el formato esperado (sin `:`, o empezando por `:`, que dejaría
el apodo vacío). El receptor trata ese `null` como «no es para mí» y sigue
escuchando.

Es deliberado: el puerto 6000 de un grupo multicast está abierto a cualquiera,
y un datagrama basura de otro programa no debe tirar el hilo receptor ni llenar
la pantalla de errores.

### Se usa el `joinGroup` moderno, y no el de la documentación de la asignatura

El material de la asignatura cita `joinGroup(InetAddress)`, que está
**obsoleto desde Java 14**. Aquí se usa
`joinGroup(SocketAddress, NetworkInterface)`, que es el vigente, para compilar
sin avisos en JDK 24.

El segundo parámetro va a `null`, que significa «la interfaz que elija el
sistema». Funciona en una máquina normal, pero es justo el punto frágil si hay
VPN o adaptadores virtuales: ahí puede hacer falta pasar la interfaz concreta.

### Varios procesos pueden compartir el puerto 6000 en la misma máquina

Esto normalmente daría `BindException`. Funciona porque `MulticastSocket`
activa `SO_REUSEADDR` por su cuenta, y es lo que permite probar la práctica
abriendo tres terminales en el mismo ordenador en lugar de necesitar tres
equipos.

### El límite de 1024 son bytes, no caracteres

El tamaño se comprueba **después** de codificar a UTF-8, sobre el total de
`apodo + ':' + mensaje`. Una tilde o una `ñ` ocupan 2 bytes, así que un mensaje
con acentos cabe menos de lo que parece contando letras. Se comprueba antes de
enviar y, si no cabe, se avisa y no se envía, en lugar de truncarlo a medias
(un corte en mitad de un carácter UTF-8 llegaría ilegible al otro lado).

### Un solo `Scanner` compartido para todo el programa

Se crea uno en `main` y se pasa como parámetro a `getNickname` y a `start`.
Dos `Scanner` sobre `System.in` tendrían búferes independientes y uno podría
leer por adelantado líneas que el otro ya no encontraría. También hay que tener
en cuenta que cerrar un `Scanner` cierra `System.in`, así que solo se cierra
una vez, en `main`, y al final.

### Java deliberadamente «antiguo»

No se usan lambdas, `record`, try-with-resources, hilos *daemon* ni `volatile`,
aunque en algún sitio quedarían más cortos. El criterio fue no usar nada que no
apareciese en el material de la asignatura, para poder defender cada línea.

### `exit` no se envía a nadie

Es una palabra que se interpreta en local: corta el bucle y no viaja por la
red. No lo pide el enunciado; venía del código de partida y se mantuvo porque
permite el cierre ordenado (cerrar socket, esperar al hilo con `join()`) en
lugar de matar el proceso con Ctrl+C. Las líneas vacías tampoco se envían.

### `Protocol` es una clase de utilidades aislada a propósito

Es `final`, tiene el constructor `private` y todos sus métodos son `static`:
nadie puede instanciarla ni heredar de ella. Lo relevante es que **no depende
ni de la consola ni de la red**, y eso es lo que permite probarla entera con
`ProtocolTest` sin levantar ningún socket.

### Las pruebas no usan framework

`ProtocolTest` es un `main` con un método `check(nombre, condición)` que cuenta
fallos y termina con `System.exit(1)` si hay alguno. No se añadió JUnit para no
meter la primera dependencia externa del proyecto, que habría obligado a
gestionar un JAR a mano con `-cp`.

## Limitaciones conocidas

- Sin probar entre equipos distintos de la red local.
- El `Scanner` solo se cierra en el camino normal de salida, no si se produce
  una excepción (`main` lo cierra después de `start`).
- La `InterruptedException` del `join()` se captura y se ignora con un `catch`
  vacío: el programa está terminando de todas formas.
- No hay lista de participantes, ni aviso de quién entra o sale, ni histórico:
  el enunciado no los pide.
