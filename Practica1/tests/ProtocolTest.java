/*
 * Pruebas automáticas de la clase Protocol (las reglas de los mensajes).
 * Cada prueba comprueba una cosa concreta y avisa si no se cumple.
 * Se ejecuta con main y, si alguna falla, el programa termina con código 1.
 */
public class ProtocolTest {
    // Cuenta cuántas pruebas han fallado
    private static int failures = 0;

    // Punto de entrada: lanza todos los grupos de pruebas
    public static void main(String[] args) {
        // Pruebas de apodos
        nicknameValidation();
        // Pruebas de construir y separar mensajes
        delimiterHandling();
        // Pruebas de reconocer mensajes propios
        selfFiltering();
        // Pruebas del formato en pantalla
        formatting();
        // Si ha fallado alguna...
        if (failures > 0) {
            // ...lo decimos y terminamos con código 1 (indica "error")
            System.out.println(failures + " prueba(s) fallida(s)");
            System.exit(1);
        } // fin del if
        // Si no ha fallado ninguna, lo celebramos
        System.out.println("Todas las pruebas correctas");
    } // fin de main

    // Comprueba qué apodos se aceptan y cuáles se rechazan
    static void nicknameValidation() {
        // Un apodo normal debe ser válido
        check("apodo normal", Protocol.isValidNickname("Ana"));
        // Un apodo con ':' debe rechazarse
        check("apodo con ':'", !Protocol.isValidNickname("A:na"));
        // Un apodo vacío debe rechazarse
        check("apodo vacío", !Protocol.isValidNickname(""));
        // Un apodo solo con espacios debe rechazarse
        check("apodo en blanco", !Protocol.isValidNickname("   "));
        // Un apodo inexistente (null) debe rechazarse
        check("apodo null", !Protocol.isValidNickname(null));
    } // fin de nicknameValidation

    // Comprueba cómo se construyen y se separan los mensajes
    static void delimiterHandling() {
        // Construir "Ana" + "hola" debe dar "Ana:hola"
        check("construir", Protocol.build("Ana", "hola").equals("Ana:hola"));
        // Separar un mensaje sencillo
        Protocol.Message m = Protocol.parse("Ana:hola");
        // Debe salir apodo "Ana" y texto "hola"
        check("parse simple", m != null && m.getNickname().equals("Ana") && m.getText().equals("hola"));
        // Separar un mensaje que lleva más ':' dentro
        m = Protocol.parse("Ana:hora: 10:30");
        // Solo se corta en el primero: el apodo es "Ana" y el texto conserva sus ':'
        check("parse con varios ':'", m != null && m.getNickname().equals("Ana") && m.getText().equals("hora: 10:30"));
        // Separar un mensaje con apodo pero sin texto
        m = Protocol.parse("Ana:");
        // Debe aceptarse, con el texto vacío
        check("mensaje vacío", m != null && m.getText().isEmpty());
        // Un texto sin ':' está mal formado y debe dar null
        check("sin delimitador", Protocol.parse("hola") == null);
        // Un texto que empieza por ':' no tiene apodo y debe dar null
        check("sin apodo", Protocol.parse(":hola") == null);
        // Un texto inexistente (null) debe dar null
        check("null", Protocol.parse(null) == null);
        // Construimos y separamos un mensaje con tildes, ñ y signos especiales
        m = Protocol.parse(Protocol.build("Ñandú", "¿qué tal? áéí"));
        // Debe recuperarse exactamente igual (sin estropear los caracteres)
        check("ida y vuelta con tildes", m != null && m.getNickname().equals("Ñandú") && m.getText().equals("¿qué tal? áéí"));
    } // fin de delimiterHandling

    // Comprueba que el programa reconoce sus propios mensajes y solo esos
    static void selfFiltering() {
        // Un mensaje enviado por "Ana"
        Protocol.Message m = Protocol.parse("Ana:hola");
        // Para el usuario "Ana", es suyo
        check("propio", Protocol.isOwn(m, "Ana"));
        // Para el usuario "Pedro", no es suyo
        check("ajeno", !Protocol.isOwn(m, "Pedro"));
        // "ana" (minúscula) es otro apodo distinto, así que no es suyo
        check("distingue mayúsculas", !Protocol.isOwn(m, "ana"));
        // "An" se parece a "Ana" pero no es igual, así que no es suyo
        check("no es prefijo", !Protocol.isOwn(m, "An"));
    } // fin de selfFiltering

    // Comprueba cómo se muestra un mensaje en pantalla
    static void formatting() {
        // Apodo "Ana" y texto "hola" deben mostrarse como "[Ana] dice: hola"
        check("formato", Protocol.format(new Protocol.Message("Ana", "hola")).equals("[Ana] dice: hola"));
    } // fin de formatting

    // Ayuda: si la condición es falsa, anota el fallo y muestra qué prueba falló
    private static void check(String name, boolean condition) {
        // Si la condición no se cumple...
        if (!condition) {
            // ...sumamos un fallo
            failures++;
            // ...y mostramos el nombre de la prueba que falló
            System.out.println("FALLA: " + name);
        } // fin del if
    } // fin de check
} // fin de la clase ProtocolTest
