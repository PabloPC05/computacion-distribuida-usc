// IOException: representa los fallos de comunicación
import java.io.IOException;
// DatagramPacket: el "paquete" donde llega cada mensaje
import java.net.DatagramPacket;
// MulticastSocket: el canal de comunicación con el grupo
import java.net.MulticastSocket;
// StandardCharsets: para convertir los bytes recibidos en texto (UTF-8)
import java.nio.charset.StandardCharsets;

/*
 * Es la parte del chat que RECIBE mensajes.
 *
 * Funciona como un "oyente" que trabaja en segundo plano (en su propio hilo):
 * se queda esperando a que llegue un mensaje, lo abre, mira quién lo envía y,
 * si es de otra persona, lo muestra en pantalla. Después vuelve a esperar.
 * Como trabaja por su cuenta, no interrumpe a la persona que está escribiendo.
 */
// "implements Runnable" indica que esta clase contiene el trabajo que hará un hilo (en run)
public class ChatReceiver implements Runnable {
    // Tamaño máximo de un mensaje, medido en bytes
    private static final int MAX_PACKET_SIZE = 1024;

    // El canal por el que llegan los mensajes (se lo da ChatClient)
    private final MulticastSocket socket;
    // El apodo de esta persona, para reconocer sus propios mensajes
    private final String nickname;

    // Constructor: recibe el canal y el apodo al crear el receptor
    public ChatReceiver(MulticastSocket socket, String nickname) {
        // Guardamos el canal
        this.socket = socket;
        // Guardamos el apodo
        this.nickname = nickname;
    } // fin del constructor

    // run es lo que hace el hilo desde que arranca hasta que termina
    public void run() {
        // Reservamos un espacio de memoria donde se guardarán los bytes de cada mensaje recibido
        byte[] buffer = new byte[MAX_PACKET_SIZE];
        // Mientras el canal siga abierto, seguimos escuchando
        while (!socket.isClosed()) {
            // Preparamos un paquete vacío que se rellenará con el mensaje que llegue
            DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
            // Intentamos recibir
            try {
                // Aquí el hilo se queda parado esperando hasta que llega un mensaje
                socket.receive(packet);
            // Si falla la recepción (o se cierra el canal)...
            } catch (IOException e) {
                // Cuando el usuario sale, el canal se cierra y la espera termina: eso es normal.
                // Por eso solo avisamos si el canal sigue abierto (entonces sí es un error).
                if (!socket.isClosed()) {
                    // Mostramos el motivo del error
                    System.err.println("Error de recepción: " + e.getMessage());
                } // fin del if del error
                // Salimos de run: el hilo termina
                return;
            } // fin del try de la recepción
            // Convertimos en texto solo los bytes que han llegado de verdad (packet.getLength())
            String raw = new String(packet.getData(), 0, packet.getLength(), StandardCharsets.UTF_8);
            // Separamos el texto en apodo y mensaje (formato APODO:MENSAJE)
            Protocol.Message message = Protocol.parse(raw);
            // Si no tiene el formato correcto (null), o si es un mensaje nuestro...
            // (todos los del grupo reciben cada mensaje, incluido quien lo envía,
            //  y no queremos ver los nuestros repetidos)
            if (message == null || Protocol.isOwn(message, nickname)) {
                // ...lo ignoramos y volvemos a esperar el siguiente
                continue;
            } // fin del if de ignorar
            // Mostramos en pantalla el mensaje de otra persona: [APODO] dice: MENSAJE
            System.out.println(Protocol.format(message));
        } // fin del while
    } // fin de run
} // fin de la clase ChatReceiver
