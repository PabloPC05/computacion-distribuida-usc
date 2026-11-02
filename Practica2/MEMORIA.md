# Memoria — Integración numérica distribuida mediante Java RMI

Computación Distribuida. Memoria de la práctica 2.

## 1. Experimentos

En esta práctica se calcula de forma aproximada la integral de e^(−x²) entre 0
y 1 (≈ 0,7468241328) con el método de Monte Carlo. El cálculo se reparte entre
varios servidores RMI. El cliente divide los N puntos entre los servidores: a
cada uno le tocan N/S, y si la división no es exacta, a los primeros les toca
un punto más, para que la suma dé exactamente N. Después lanza un hilo por
servidor para que todas las llamadas se hagan a la vez.

El tiempo se mide desde que se arranca el primer hilo hasta que termina el
último.

Todas las pruebas se han hecho en un portátil (Intel Core i5-1235U con 10
núcleos, 16 GB de RAM, Windows 11 y JDK 24), con cada servidor en un proceso
distinto. Como el tiempo cambia un poco de una ejecución a otra, cada prueba se
ha repetido 5 veces y se ha hecho la media.

## 2. Tiempos, speedup y eficiencia

Con N = 100 000 000 puntos:

| Servidores | Tiempo (s) | Speedup | Eficiencia |
|---:|---:|---:|---:|
| 1 | 5,162 | 1,00 | 1,00 |
| 2 | 2,478 | 2,08 | 1,04 |
| 4 | 1,230 | 4,19 | 1,05 |
| 8 | 0,633 | 8,15 | 1,02 |

## 3. Análisis

El speedup sale prácticamente igual que el ideal: cada vez que se duplican los
servidores, el tiempo baja a la mitad. Algunas eficiencias salen un poco por
encima de 1. No es que vaya mejor que lo ideal: el tiempo con 1 servidor varió
entre 4,8 y 5,8 s según la ejecución, y como todos los speedups se calculan a
partir de ese tiempo, esa variación se nota en todos.

- **Tiempo de cálculo**: cada punto se calcula de forma independiente y los
  servidores no tienen que hablar entre ellos, así que el trabajo se puede
  repartir sin perder nada. Además, el cálculo tarda segundos.
- **Coste de las comunicaciones RMI**: cada servidor recibe una sola llamada en
  la que solo viaja un `long` de ida y otro de vuelta, así que la comunicación
  es muy poco comparada con el cálculo (ver la cuestión 5).
- **Creación y sincronización de los hilos**: crear los hilos y esperar con
  `join()` es muy rápido y casi no se nota.
- **Reparto de carga**: como el reparto es exacto (como mucho hay un punto de
  diferencia), todos los servidores terminan casi a la vez y nadie se queda
  esperando.
- **Diferencias de velocidad entre servidores**: todos los servidores están en
  el mismo ordenador y van más o menos igual de rápido. Si alguno fuera más
  lento, el tiempo total sería el suyo (se ve en la sección 4).

Los resultados salen tan bien porque el ordenador tiene núcleos de sobra para
todos los servidores y no hay que mandar nada por la red. Si hubiera más
servidores que núcleos, o si los servidores estuvieran en otros ordenadores, no
se ganaría tanto al añadir servidores.

## 4. Reparto dinámico

En el cliente dinámico, el trabajo se divide en bloques de tamaño fijo. Hay un
objeto compartido (la «bolsa») con un método `synchronized` que va dando los
bloques. Cada hilo coge un bloque, se lo manda a su servidor y, cuando le
contesta, coge otro, hasta que no quedan. El método tiene que ser
`synchronized` para que dos hilos no puedan coger el mismo bloque a la vez.

Para tener servidores de distinta velocidad, al servidor se le puede pasar un
factor: después de calcular, se queda esperando (factor − 1) veces lo que ha
tardado, así que tarda «factor» veces más que uno normal.

Resultados con 4 servidores, N = 10⁸ y bloques de 10⁶ puntos:

| Servidores | Estático (s) | Dinámico (s) | Bloques hechos por cada servidor |
|---|---:|---:|---|
| Iguales (factores 1, 1, 1, 1) | 1,29 | 1,28 | 25 / 25 / 25 / 25 |
| Distintos (factores 1, 1, 2, 4) | 4,89 | 1,93 | 36 / 36 / 19 / 10 |

Con servidores iguales, los dos repartos tardan lo mismo.

Con servidores distintos, en el estático todos reciben lo mismo y hay que
esperar al más lento, que tarda 4 veces más. Mientras tanto, los rápidos ya han
terminado y no hacen nada. En el dinámico, los servidores rápidos van cogiendo
más bloques (36 frente a 10), así que termina 2,5 veces antes.

**Ventajas del reparto dinámico**: se adapta solo a la velocidad de cada
servidor, sin tener que saberla de antemano.

**Inconvenientes del reparto dinámico**:

- Hace muchas más llamadas RMI (una por bloque).
- Hace falta sincronizar el acceso a la bolsa.
- Hay que elegir bien el tamaño de bloque:
  - Si es muy pequeño, hay demasiadas llamadas. Con bloques de 10⁴ tardó
    1,61 s, frente a 1,26 s con bloques de 10⁶ (servidores iguales).
  - Si es muy grande, se pierde la ventaja. Con 10 bloques para 4 servidores, a
    unos les tocan 3 y a otros 2, y tarda 1,48 s. Con un bloque por servidor es
    igual que el estático.

En estas pruebas fue mejor con bloques de entre 10⁵ y 10⁶ puntos.

## 5. Cuestiones

### 1. ¿Por qué la aproximación mejora al aumentar el número de puntos generados?

Porque la proporción de puntos que caen debajo de la curva se va pareciendo
cada vez más al área real. Con pocos puntos, el azar influye mucho: unos pocos
puntos de más o de menos cambian bastante el resultado. Con muchos puntos, esas
variaciones se compensan. Comprobado con un servidor y distintos N (media de 3
ejecuciones):

| N | Error \|M/N − I\| |
|---:|---:|
| 10³ | 0,0064 |
| 10⁴ | 0,0019 |
| 10⁵ | 0,00030 |
| 10⁶ | 0,00020 |
| 10⁷ | 0,000020 |
| 10⁸ | 0,000018 |

El error baja al aumentar N, aunque despacio: para ganar precisión hacen falta
muchísimos más puntos, y por eso compensa repartir el cálculo entre varios
servidores.

### 2. ¿Por qué es necesario utilizar hilos en el cliente para que las llamadas RMI se realicen en paralelo?

Porque una llamada RMI deja al cliente esperando hasta que el servidor le
devuelve el resultado. Si el cliente llamara él mismo a todos los servidores,
no podría llamar al segundo hasta que respondiera el primero. Con un hilo por
servidor, cada hilo se queda esperando a su servidor, pero todos a la vez, así
que los servidores calculan al mismo tiempo. Para eso hay que hacer primero el
`start()` de todos los hilos y después, en otro bucle, el `join()` de todos.

### 3. ¿Qué ocurriría con el tiempo de ejecución si el cliente realizase las llamadas a los servidores secuencialmente?

El tiempo total sería la suma de lo que tarda cada servidor. Como entre todos
hacen los N puntos, tardaría más o menos lo mismo que un solo servidor (unos
5 s con N = 10⁸), o incluso algo más por las llamadas. Añadir servidores no
serviría de nada: mientras uno calcula, los demás están parados.

### 4. ¿Es de esperar que utilizar el doble de servidores reduzca exactamente a la mitad el tiempo de ejecución?

En general no. Hay partes que no se reparten (leer N, buscar los servidores,
crear los hilos, sumar los resultados), la comunicación y los hilos cuestan más
cuanto más servidores hay, y el tiempo total es el del servidor que más tarde.
Con N = 10⁸ sale casi exactamente la mitad porque todo eso tarda milisegundos y
el cálculo tarda segundos.

Para comprobar que no siempre es así, se repitió el experimento con un N mucho
más pequeño, N = 10⁵ (media de 5 ejecuciones):

| Servidores | Tiempo (ms) | Speedup |
|---:|---:|---:|
| 1 | 10,8 | 1,00 |
| 2 | 7,0 | 1,54 |
| 4 | 5,1 | 2,11 |
| 8 | 8,2 | 1,32 |

Aquí el cálculo de cada servidor dura tan poco que la comunicación y los hilos
(unos pocos milisegundos) pasan a ser una parte importante del tiempo, así que
duplicar los servidores ya no divide el tiempo a la mitad. Con 8 servidores
incluso tarda más que con 4. Tampoco se cumpliría con más servidores que
núcleos o con servidores en otras máquinas.

### 5. ¿Qué parte del tiempo de ejecución puede corresponder a las comunicaciones RMI?

Cada vez que el cliente llama a un servidor, el número de puntos tiene que
viajar hasta el servidor y luego el resultado tiene que volver. Ese ir y volver
es lo que cuesta la comunicación, y es algo que no pasa cuando se llama a un
método normal dentro del mismo programa.

En esta práctica la comunicación es una parte muy pequeña del tiempo total.
Cada servidor recibe una sola llamada, y en ella solo se manda un número de ida
y otro de vuelta. En cambio, el cálculo con N = 10⁸ tarda segundos. Por eso, al
añadir servidores, el tiempo baja casi a la mitad cada vez: si la comunicación
tardara mucho, no se ganaría tanto.

La comunicación sí se notaría más en estos casos:

- **Si N es pequeño**: cada servidor calcula tan poco que el tiempo de enviar y
  recibir ya pesa (se ve en la cuestión 4 con N = 10⁵).
- **Si hay muchas llamadas**: por ejemplo, en el reparto dinámico con bloques
  muy pequeños, porque se hace una llamada por cada bloque.
- **Si los servidores están en otros ordenadores**: los datos tendrían que ir
  por la red de verdad y tardarían más que dentro del mismo ordenador.

### 6. ¿Qué ocurriría si uno de los servidores dejase de responder durante la ejecución?

Si el servidor se cae o se corta la conexión, la llamada lanza una
`RemoteException` en el hilo de ese servidor y sus puntos no se cuentan. Si el
cliente no lo tuviera en cuenta, sumaría un 0 en lugar del resultado de ese
servidor y daría una integral incorrecta. Se probó matando uno de los dos
servidores a mitad del cálculo, y el resultado salió 0,373 en lugar de 0,7468.
Por eso cada hilo apunta si su llamada ha fallado y el cliente, en ese caso,
dice qué servidor ha fallado y avisa de que el resultado no es válido en vez de
mostrarlo. Si el servidor se queda colgado sin llegar a cortar la conexión, la
llamada puede no terminar nunca, el `join()` se queda esperando para siempre y
el programa no acaba.

### 7. ¿Cómo podría modificarse el cliente para detectar que un servidor no responde y evitar que la aplicación quede bloqueada indefinidamente?

Poniendo un tiempo máximo de espera (un *timeout*). Por ejemplo, usando
`join(tiempo)` en vez de `join()` y comprobando después con `isAlive()` si el
hilo sigue esperando. Si sigue, se considera que ese servidor ha fallado.
También habría que hacer algo con el trabajo de ese servidor: mandárselo a otro
servidor o, en el reparto dinámico, devolver su bloque a la bolsa para que lo
coja otro hilo, para que el resultado salga bien aunque falle un servidor
(ahora mismo el programa solo avisa de que el resultado no es válido).

### 8. ¿Qué problemas pueden aparecer si los servidores tienen capacidades de cálculo muy diferentes?

Con el reparto estático, todos reciben el mismo trabajo, así que el tiempo
total lo marca el más lento, y los rápidos se quedan sin hacer nada cuando
terminan su parte. En las pruebas de la sección 4, con un servidor 4 veces más
lento, el estático tardó 4,89 s frente a 1,29 s con todos iguales, aunque los
otros tres servidores eran igual de rápidos que antes. Con el reparto dinámico
se soluciona en gran parte: tardó 1,93 s.

### 9. ¿Por qué un reparto perfectamente equilibrado en número de puntos no garantiza necesariamente un reparto perfectamente equilibrado en tiempo de ejecución?

Porque lo que tarda cada servidor no depende solo de cuántos puntos tiene, sino
también de lo rápido que es el ordenador o el núcleo en el que se ejecuta, de
qué otros programas esté ejecutando y de cuánto tarde la comunicación con él.
Incluso con servidores iguales, el mismo cálculo no tarda siempre lo mismo (en
estas pruebas, entre 4,8 y 5,8 s). Como el cliente tiene que esperar a todos,
basta con que uno tarde más para que se retrase todo. El reparto dinámico lo
evita, porque cada servidor recibe trabajo nuevo cuando termina el anterior.
