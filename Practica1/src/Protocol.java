/*
 * Define cómo se escriben y se leen los mensajes que viajan por la red.
 * Es como un "idioma común" que todos los participantes tienen que respetar.
 *
 * Cada mensaje se envía así:   APODO:MENSAJE
 * Por ejemplo:                 Ana:hola a todos
 *
 * Esta clase solo trabaja con texto: no lee de la consola ni usa la red.
 * Por eso es sencilla de probar por separado (ver tests/ProtocolTest.java).
 */
// "final" significa que nadie puede crear clases derivadas de esta
public final class Protocol {
    // El carácter que separa el apodo del mensaje
    public static final char DELIMITER = ':';

    // Un mensaje ya separado en sus dos partes: quién lo envía y qué dice
    public static class Message {
        // El apodo de quien envió el mensaje
        private final String nickname;
        // El texto del mensaje
        private final String text;

        // Constructor: crea un Message con su apodo y su texto
        public Message(String nickname, String text) {
            // Guardamos el apodo
            this.nickname = nickname;
            // Guardamos el texto
            this.text = text;
        } // fin del constructor

        // Devuelve el apodo (para poder consultarlo desde fuera)
        public String getNickname() {
            return nickname;
        } // fin de getNickname

        // Devuelve el texto del mensaje
        public String getText() {
            return text;
        } // fin de getText
    } // fin de la clase Message

    // Constructor privado: impide crear objetos Protocol, porque solo se usa para llamar a sus métodos
    private Protocol() {
    } // fin del constructor

    // Dice si un apodo es válido
    public static boolean isValidNickname(String nickname) {
        // Es válido si: no es null (inexistente), no está vacío ni solo con espacios,
        // y no contiene ':' (indexOf devuelve un número negativo si no lo encuentra)
        return nickname != null && !nickname.isBlank() && nickname.indexOf(DELIMITER) < 0;
    } // fin de isValidNickname

    // Prepara el texto que se va a enviar
    public static String build(String nickname, String text) {
        // Unimos apodo + ':' + mensaje. Ejemplo: "Ana" y "hola" dan "Ana:hola"
        return nickname + DELIMITER + text;
    } // fin de build

    // Hace lo contrario que build: separa el texto recibido en apodo y mensaje.
    // Corta solo en el PRIMER ':', así el mensaje sí puede llevar ':' dentro
    // (por ejemplo "Ana:la hora es 10:30" da apodo "Ana" y mensaje "la hora es 10:30").
    public static Message parse(String raw) {
        // Si no hay texto (null), no hay nada que separar
        if (raw == null) {
            // Devolvemos null para indicar "mensaje no válido"
            return null;
        } // fin del if de null
        // Buscamos en qué posición está el primer ':' (se empieza a contar desde 0)
        int index = raw.indexOf(DELIMITER);
        // Si no hay ':' (devuelve -1) o está en la posición 0 (no habría apodo)...
        if (index <= 0) {
            // ...el mensaje está mal formado y devolvemos null
            return null;
        } // fin del if de formato
        // Lo anterior al ':' es el apodo y lo posterior es el mensaje
        return new Message(raw.substring(0, index), raw.substring(index + 1));
    } // fin de parse

    // Dice si un mensaje es nuestro
    public static boolean isOwn(Message message, String localNickname) {
        // Es nuestro si el apodo del mensaje es exactamente igual al nuestro
        // (distingue mayúsculas: "Ana" y "ana" son apodos distintos)
        return message.getNickname().equals(localNickname);
    } // fin de isOwn

    // Prepara el texto para mostrarlo en pantalla
    public static String format(Message message) {
        // Ejemplo: apodo "Ana" y texto "hola" dan "[Ana] dice: hola"
        return "[" + message.getNickname() + "] dice: " + message.getText();
    } // fin de format
} // fin de la clase Protocol
