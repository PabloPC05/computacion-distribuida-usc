# Computación Distribuida — USC 2026/2027

Prácticas de Computación Distribuida del curso 2026/2027 en la Universidade de
Santiago de Compostela (asignatura cursada durante una movilidad SICUE).

Cada práctica es un proyecto de Java independiente, sin Maven ni Gradle: solo
`javac` y `java`. No hay dependencias externas de ningún tipo.

## Prácticas

| Práctica | Tema | Qué hace |
|---|---|---|
| [Práctica 1](Practica1/) | Sockets UDP multicast e hilos | Chat de grupo en red local, descentralizado y sin servidor. |
| [Práctica 2](Practica2/) | Objetos distribuidos con Java RMI | Integración numérica por Monte Carlo repartida entre varios servidores, con reparto estático y dinámico. Incluye [memoria con los experimentos](Practica2/MEMORIA.md). |

Las prácticas 3 a 5 se irán añadiendo cuando estén corregidas.

## Requisitos comunes

JDK 11 o superior. Las dos prácticas se desarrollaron con **JDK 24.0.2** en
Windows, y los ejemplos de los README están escritos para **PowerShell**, que
es el terminal por defecto de VS Code en Windows. En Linux o macOS los comandos
son los mismos cambiando `\` por `/` en las rutas y `;` por `:` en los
*classpath*.

Ambas se han vuelto a compilar y ejecutar con **JDK 17 en macOS** (Apple
Silicon) sin cambiar una línea: compilan sin avisos, las pruebas pasan y las
dos funcionan. Es decir, el código no depende de Windows ni de JDK 24.

## Qué no está en este repositorio

Por decisión deliberada se han dejado fuera:

- **Los enunciados y las transparencias de la asignatura**: son material del
  profesorado, no trabajo propio.
- **Las carpetas de entrega y los ZIP**: son el envase administrativo de la
  entrega, no el proyecto. La memoria de la Práctica 2 sí está, pasada a
  Markdown para que se lea en GitHub.
- **Los `.class` compilados** y la configuración local de VS Code.

Lo que queda es solo el código fuente, las pruebas y la documentación.

## Aviso académico

Este repositorio es trabajo propio y se publica como registro personal de lo
aprendido. Si cursas esta asignatura, copiarlo no te va a servir de nada: las
prácticas se defienden en persona.
