# Práctica 2 — Monte Carlo distribuido con Java RMI

Cálculo aproximado de la integral

```
I = ∫₀¹ e^(−x²) dx ≈ 0,7468241328
```

por el método de Monte Carlo, repartiendo los puntos entre varios servidores
RMI. El cliente lanza un hilo por servidor para que las llamadas remotas vayan
en paralelo.

Se implementan **dos repartos distintos** sobre los mismos servidores: uno
estático y uno dinámico, para poder comparar qué pasa cuando los servidores no
van todos a la misma velocidad.

Los experimentos, los tiempos, el *speedup* y el análisis están en
[MEMORIA.md](MEMORIA.md).

## Estructura

| Fichero | Papel |
|---|---|
| `src/MonteCarlo.java` | Interfaz remota. Un solo método: `long conteo(long n)`. |
| `src/MonteCarloImpl.java` | Objeto remoto (`UnicastRemoteObject`). Genera los `n` puntos y cuenta cuántos caen bajo la curva. Admite un factor de lentitud. |
| `src/Servidor.java` | Crea el objeto remoto y lo registra en `rmiregistry` con el nombre que se le pase. |
| `src/Cliente.java` + `src/HiloCliente.java` | Reparto **estático**: N se parte a priori y cada hilo hace una única llamada. |
| `src/ClienteDinamico.java` + `src/HiloClienteDinamico.java` + `src/Bolsa.java` | Reparto **dinámico**: N se parte en bloques y cada hilo pide uno nuevo cada vez que su servidor queda libre. |

## Requisitos

JDK 11 o superior (probado con JDK 24). Todos los comandos se ejecutan desde
esta carpeta, la que contiene `src/`. Los ejemplos son para PowerShell.

## Compilar

```powershell
javac -d bin src/*.java
```

## Ejecutar

Cada proceso va en **su propio terminal**, y en este orden.

### 1. El registro RMI

```powershell
rmiregistry -J-cp -Jbin
```

(Si no está en el PATH: `& "C:\Program Files\Java\jdk-24\bin\rmiregistry.exe" -J-cp -Jbin`.)

No imprime nada: se queda esperando. **El `-J-cp -Jbin` es imprescindible**;
el motivo está en las decisiones de diseño.

### 2. Los servidores

Uno por terminal, cada uno con un nombre distinto:

```powershell
java -cp bin Servidor Servidor1
java -cp bin Servidor Servidor2
java -cp bin Servidor Servidor3
java -cp bin Servidor Servidor4
```

Cada uno imprime `Servidor listo.` y se queda esperando llamadas. Se paran con
Ctrl+C.

Un segundo argumento opcional simula un servidor más lento:

```powershell
java -cp bin Servidor Lento4 4
```

tarda 4 veces más que uno normal. Por defecto es `1`, y **no debe ser menor
que 1**.

### 3a. Cliente con reparto estático

Se le pasan los nombres de los servidores a usar, y pide N por teclado:

```powershell
java -cp bin Cliente Servidor1 Servidor2 Servidor3 Servidor4
```

```
Numero de puntos a generar: 100000000
Conteo: 74682044
Resultado: 0.74682044
Tiempo de ejecucion: 1.2346465 segundos
```

### 3b. Cliente con reparto dinámico

Pide además el tamaño de bloque:

```powershell
java -cp bin ClienteDinamico Servidor1 Servidor2 Servidor3 Servidor4
```

```
Numero de puntos a generar: 100000000
Tam. de bloque: 1000000
Conteo: 74684622
Resultado: 0.74684622
Tiempo de ejecucion: 1.293847 segundos
Bloques procesados por el hilo 1: 25
...
```

El recuento de bloques por hilo es lo interesante: con servidores iguales sale
repartido; con uno lento, el lento procesa muchos menos y los demás absorben su
trabajo.

### Comprobación de que esto funciona

Ejecutado en macOS con JDK 17, tres servidores (`Srv1`, `Srv2` normales y
`Lento` con factor 4) y N = 20 000 000:

| Reparto | Tiempo | Resultado | Bloques por hilo |
|---|---:|---:|---|
| Estático | 0,536 s | 0,74671105 | — |
| Dinámico (bloque 500 000) | 0,179 s | 0,74678910 | 18 / 18 / 4 |

El dinámico es **3 veces más rápido** con exactamente los mismos servidores, y
el motivo se ve en la última columna: al servidor lento solo le tocaron 4
bloques frente a los 18 de cada rápido. Los dos resultados se acercan al valor
real (0,7468241328), que es lo que confirma que el reparto no pierde puntos.

### Para probar el equilibrado

- **Menos servidores**: pasar menos nombres al cliente.
- **Varios hilos contra el mismo servidor**: repetir el nombre.
  `java -cp bin ClienteDinamico Servidor1 Servidor1 Servidor1 Servidor1` crea
  cuatro hilos que atacan al mismo servidor. Funciona porque RMI atiende cada
  llamada en un hilo propio.
- **Un servidor lento**: arrancar uno con factor 4 y comparar el reparto
  estático con el dinámico. Es el caso que justifica toda la práctica.

## Decisiones de diseño llamativas

### El servidor calcula a toda velocidad y *después* duerme

La forma obvia de simular un servidor lento sería ralentizar el bucle. Lo que
hace `MonteCarloImpl.conteo` es otra cosa: mide con `System.nanoTime()` cuánto
ha tardado de verdad, y luego llama a `Thread.sleep((factor - 1) * tiempo)`.
Con `factor = 4` calcula a velocidad normal y luego duerme 3 veces ese tiempo,
de modo que el total es 4×.

Es más fiel que ralentizar el bucle (la proporción sale exacta para cualquier
tamaño de bloque) pero tiene dos consecuencias raras:

- El servidor consume CPU al 100% durante una fracción del tiempo y después
  está completamente ocioso, en lugar de ir lento de forma sostenida.
- **Un factor menor que 1 daría un `sleep` negativo** y una
  `IllegalArgumentException`. No se valida en el código; es la razón de la
  advertencia de arriba.

### La interfaz remota devuelve el conteo, no el resultado

`conteo(n)` devuelve «cuántos de los n puntos cayeron bajo la curva», un
`long`. La división entre N, que es lo que convierte eso en la estimación de la
integral, la hace **el cliente**.

Parece un detalle, pero es lo que hace posible el reparto dinámico: los
conteos de bloques distintos se pueden sumar sin más (`result += ...`), mientras
que promediar resultados parciales exigiría ponderarlos por el tamaño de cada
bloque. Además deja el objeto remoto sin estado, y por eso varios hilos pueden
llamar al mismo servidor a la vez sin ningún tipo de sincronización.

### `Bolsa`: un monitor con el 0 como señal de «ya no hay más»

`Bolsa` es el reparto dinámico entero, y son 20 líneas. Un único método
`synchronized getResto()` que devuelve tres cosas distintas:

- el tamaño de bloque completo, si queda suficiente;
- lo que quede, si queda menos de un bloque (el último reparto es más pequeño);
- **`0`, que significa «se acabó»**.

Los hilos hacen `while ((bloque = bolsa.getResto()) > 0)`: el valor de retorno
es a la vez el trabajo y la condición de parada. No hace falta `wait`/`notify`
ni ningún otro mecanismo de espera, porque **ningún hilo espera nunca**: o hay
trabajo y se lo lleva, o no hay y termina. El `synchronized` está solo para que
dos hilos no se lleven el mismo bloque.

### El reparto estático reparte el resto punto a punto

En `Cliente`, `n / length` pierde puntos por la división entera. En lugar de
asumir esa pérdida, los primeros `n % length` servidores reciben **un punto
más**:

```java
if (i < n % length) hilos[i] = new HiloCliente(stub, n/length + 1);
else                hilos[i] = new HiloCliente(stub, n/length);
```

Así la suma de los repartos es exactamente N, y el denominador de la división
final es correcto. Con N = 100.000.000 da igual; pero con N pequeño y muchos
servidores, no.

### Los resultados se pasan por campos públicos leídos tras el `join()`

Los hilos no devuelven nada: escriben en sus campos (`result`, `num_bloques`,
`fallo`) y el cliente los lee con un *getter* **después** de `hilos[i].join()`.
No hay `synchronized`, ni `volatile`, ni `Future`.

Es correcto, pero por un motivo que merece explicarse: `join()` establece una
relación *happens-before*, así que todo lo que el hilo escribió antes de
terminar es visible para quien lo espera. Si esos mismos campos se leyeran
**sin** haber hecho `join()`, el código estaría mal y podría leer valores
viejos. El orden de las dos operaciones en el bucle del cliente (primero
`join()`, luego `getResult()`) no es cosmético.

### Los hilos heredan de `Thread` en vez de implementar `Runnable`

Lo habitual es `Runnable`, y en la Práctica 1 se hizo así. Aquí se usa
`extends Thread` porque cada hilo tiene que **llevar estado propio y
devolverlo**: el stub de su servidor, su resultado, su contador de bloques y su
marca de fallo. Heredar de `Thread` permite guardar todo eso en el objeto y
leerlo después del `join()` sin estructuras auxiliares.

### Si un servidor falla, se tira el resultado entero

Cuando un hilo recibe una `RemoteException`, marca `fallo = true`. El cliente
suma los conteos igualmente, pero luego comprueba las marcas y, si hay alguna,
imprime `Algun servidor ha fallado: el resultado no es valido` y **no muestra
ningún resultado**.

Es deliberado: la estimación es `conteo / N`, con N fijo. Si un servidor se cae
y sus puntos no se calculan, el numerador se queda corto pero el denominador no,
así que el resultado saldría **sistemáticamente bajo**. Un resultado silencioso
y erróneo es peor que ningún resultado.

En el cliente dinámico esto se nota más: el hilo que falla hace `break` y
abandona, de modo que los bloques que ya se había llevado de la `Bolsa` se
pierden y **nadie los reasigna**. Reintentarlos en otro servidor sería la
mejora natural; la práctica no lo pide.

### El `Scanner` se cierra antes de empezar a trabajar

`sc.close()` está justo después de leer N (y el tamaño de bloque), antes de
tocar RMI. Cerrar un `Scanner` cierra `System.in`, así que esto deja al
programa sin entrada estándar para el resto de la ejecución. Es intencionado:
ya no se va a pedir nada más por teclado.

### El cronómetro empieza después de los `lookup`

En los dos clientes, los `registry.lookup()` se hacen **antes** de
`System.nanoTime()`. El tiempo que se mide es solo el del cálculo en paralelo,
sin el coste de resolver los nombres en el registro, que no tiene nada que ver
con el rendimiento del reparto. Los `start()` de todos los hilos también quedan
dentro de la medición, que es lo correcto: forman parte del trabajo.

### `rmiregistry` necesita `-J-cp -Jbin` y eso sorprende a todo el mundo

No se configura `java.rmi.server.codebase` ni carga dinámica de clases, así que
**el registro tiene que encontrar las clases en su propio classpath**. Si se
arranca `rmiregistry` a secas, el `rebind` del servidor falla con
`ClassNotFoundException`, y el error aparece en la terminal del registro, no en
la del servidor, que es lo que hace difícil de diagnosticar.

Las `-J` son la forma de pasarle opciones a la JVM que `rmiregistry` arranca
por dentro: `-J-cp -Jbin` equivale a arrancar esa JVM con `-cp bin`.

### Todo apunta a `localhost` por defecto

`LocateRegistry.getRegistry()` sin argumentos significa `localhost:1099`. Los
«varios servidores» de la práctica son en realidad varios procesos en la misma
máquina. Para repartir de verdad entre equipos habría que pasar el *host* al
`getRegistry` y resolver la propiedad `java.rmi.server.hostname` en los
servidores.

### `rebind` en vez de `bind`

`Servidor` usa `registry.rebind(args[0], montecarlo)`, que **sobrescribe** una
entrada previa con el mismo nombre en lugar de fallar con
`AlreadyBoundException`. Práctico cuando se reinicia un servidor muchas veces
durante las pruebas, pero significa que arrancar por error dos servidores con
el mismo nombre no avisa: el segundo desplaza al primero sin más.

### Un `Random` nuevo en cada llamada

`conteo` crea un `new Random()` en cada invocación, en lugar de reutilizar uno
por servidor. Al sembrarse del reloj, cada bloque y cada servidor obtienen una
secuencia distinta, que es lo que hace falta para que los conteos sean
independientes y se puedan sumar. El efecto secundario es que **las ejecuciones
no son reproducibles**: no hay semilla fija, y dos ejecuciones con el mismo N
dan resultados ligeramente distintos (lo cual, para un método de Monte Carlo,
es lo esperable).

### `conteo` no declara `throws RemoteException`

La interfaz `MonteCarlo` lo declara, como exige RMI, pero la implementación en
`MonteCarloImpl` no. Es legal en Java: una implementación puede declarar menos
excepciones que su interfaz. Funciona porque los fallos de red los lanza la
infraestructura de RMI desde el *stub*, en el lado del cliente, no el cuerpo
del método en el servidor.

## Errores típicos

| Error | Causa |
|---|---|
| `Connection refused` al arrancar un servidor o el cliente | El registro no está arrancado. |
| `NotBoundException` | El nombre pasado al cliente no coincide con ninguno registrado. |
| `ClassNotFoundException` **en la terminal del registro** | Se arrancó `rmiregistry` sin `-J-cp -Jbin`, o antes de compilar. |
| `IllegalArgumentException: timeout value is negative` | Se arrancó un servidor con factor de lentitud menor que 1. |
