// Herramientas que necesitamos (cada import trae una pieza que usaremos más abajo):
// IOException: representa los fallos de comunicación (por ejemplo, no hay red)
import java.io.IOException;
// DatagramPacket: el "paquete" que contiene un mensaje para enviarlo o recibirlo
import java.net.DatagramPacket;
// InetAddress: representa una dirección de red (en nuestro caso, la del grupo)
import java.net.InetAddress;
// InetSocketAddress: una dirección de red junto con su puerto (dirección + puerto del grupo)
import java.net.InetSocketAddress;
// MulticastSocket: el "canal de comunicación" para hablar con el grupo
import java.net.MulticastSocket;
// SocketAddress: tipo general para guardar una dirección con puerto
import java.net.SocketAddress;
// StandardCharsets: nos permite indicar cómo se convierte el texto en bytes (UTF-8)
import java.nio.charset.StandardCharsets;
// Scanner: lee lo que el usuario escribe en la consola
import java.util.Scanner;

/*
 * Es la parte del chat que ENVÍA mensajes y que organiza todo lo demás.
 *
 * Idea general: el chat funciona como una "emisora de radio" a la que se conectan
 * todos los participantes. Lo que envía uno lo oyen todos los que están sintonizados
 * (esa sintonía se llama grupo multicast y se identifica con una dirección y un puerto).
 * No hay servidor en medio: cada programa habla directamente con los demás.
 *
 * Cada programa hace dos cosas A LA VEZ, cada una en su propio hilo:
 *   - Hilo principal (esta clase):  lee lo que escribes y lo envía.
 *   - Hilo de recepción (ChatReceiver): escucha y muestra lo que envían los demás.
 * Así puedes seguir recibiendo mensajes mientras estás escribiendo.
 */
public class ChatClient {
    // Dirección del grupo (la "emisora"). Es fija porque lo pide el enunciado.
    // "final" significa que su valor no se puede cambiar; "static" que es común a toda la clase
    private static final String GROUP_ADDRESS = "239.1.2.3";
    // Puerto del grupo (como un "canal" dentro de la dirección). También fijo por el enunciado
    private static final int PORT = 6000;
    // Tamaño máximo de un mensaje, medido en bytes (aprox. 1 letra = 1 byte)
    private static final int MAX_PACKET_SIZE = 1024;

    // Aquí guardamos el apodo de esta persona; no cambia mientras el programa funciona
    private final String nickname;

    // Constructor: se ejecuta al crear un ChatClient y le entrega el apodo
    public ChatClient(String nickname) {
        // Guardamos el apodo recibido en la variable de la clase
        this.nickname = nickname;
    } // fin del constructor

    // Método que pone en marcha todo el chat
    public void start(Scanner scanner) {
        // El canal de comunicación; empieza vacío (null) porque aún no está creado
        MulticastSocket socket = null;
        // El hilo que escuchará; también empieza vacío
        Thread receiver = null;
        // "try": intentamos hacer lo siguiente; si algo falla, saltamos al "catch"
        try {
            // Convertimos el texto "239.1.2.3" en una dirección de red que Java entiende
            InetAddress group = InetAddress.getByName(GROUP_ADDRESS);
            // Abrimos el canal de comunicación en el puerto 6000
            socket = new MulticastSocket(PORT);
            // Juntamos la dirección y el puerto del grupo en un solo objeto
            SocketAddress groupAddress = new InetSocketAddress(group, PORT);
            // Nos unimos al grupo: es como sintonizar la emisora para poder oír a los demás.
            // El segundo dato es la tarjeta de red a usar; null significa "la que use el sistema por defecto"
            socket.joinGroup(groupAddress, null);

            // Creamos el hilo que escuchará (le damos el canal y nuestro apodo) y lo llamamos "Receptor"
            receiver = new Thread(new ChatReceiver(socket, nickname), "Receptor");
            // Lo arrancamos: desde aquí escuchar y escribir ocurren al mismo tiempo
            receiver.start();

            // Avisamos de que el chat ya está en marcha
            System.out.println("Cliente de chat iniciado con el apodo: " + nickname);
            // Explicamos cómo usarlo
            System.out.println("Escribe un mensaje y pulsa Enter. 'exit' para salir.");
            // Entramos en el bucle que lee lo que escribes y lo envía (termina con "exit")
            sendLoop(scanner, socket, group);

            // Al salir del chat, nos damos de baja del grupo
            socket.leaveGroup(groupAddress, null);
        // "catch": se ejecuta solo si algo del try falló por un problema de comunicación
        } catch (IOException e) {
            // Mostramos el motivo del fallo como mensaje de error
            System.err.println("No se pudo iniciar el chat: " + e.getMessage());
        // "finally": se ejecuta SIEMPRE al final, haya ido todo bien o mal
        } finally {
            // Si el canal llegó a crearse...
            if (socket != null) {
                // ...lo cerramos. Esto también hace que el hilo que escucha deje de esperar y termine
                socket.close();
            } // fin del if del canal
            // Si el hilo de escucha llegó a crearse...
            if (receiver != null) {
                // Intentamos esperar...
                try {
                    // ...a que el hilo de escucha termine por completo antes de acabar el programa
                    receiver.join();
                // Si esa espera es interrumpida, no hacemos nada especial (por eso queda vacío)
                } catch (InterruptedException e) {
                } // fin del try de la espera
            } // fin del if del hilo
        } // fin del try/catch/finally
    } // fin de start

    // Método que lee lo que escribes y lo envía, una y otra vez, hasta que escribas "exit"
    private void sendLoop(Scanner scanner, MulticastSocket socket, InetAddress group) {
        // Mientras haya otra línea para leer, repetimos
        while (scanner.hasNextLine()) {
            // Leemos lo que el usuario escribe hasta que pulsa Enter
            String message = scanner.nextLine();
            // Si ha escrito "exit" (da igual mayúsculas o minúsculas)...
            if (message.equalsIgnoreCase("exit")) {
                // ...nos despedimos
                System.out.println("Saliendo del chat...");
                // ...y salimos del bucle; "exit" no se envía a nadie
                break;
            } // fin del if de exit
            // Si ha pulsado Enter sin escribir nada (o solo espacios)...
            if (message.isBlank()) {
                // ...no enviamos nada y volvemos a leer
                continue;
            } // fin del if de mensaje vacío
            // Preparamos el texto con el formato APODO:MENSAJE y lo convertimos en bytes
            // (lo que realmente viaja por la red). UTF-8 permite tildes y ñ.
            byte[] data = Protocol.build(nickname, message).getBytes(StandardCharsets.UTF_8);
            // Si el mensaje ocupa más de lo permitido...
            if (data.length > MAX_PACKET_SIZE) {
                // ...avisamos del problema
                System.err.println("Mensaje demasiado largo (máximo " + MAX_PACKET_SIZE + " bytes en total).");
                // ...y no lo enviamos
                continue;
            } // fin del if de mensaje largo
            // Intentamos enviar el mensaje
            try {
                // Metemos los bytes en un paquete dirigido al grupo y lo enviamos; lo recibirán todos
                socket.send(new DatagramPacket(data, data.length, group, PORT));
            // Si el envío falla (por ejemplo, se cae la red)...
            } catch (IOException e) {
                // ...mostramos el motivo y seguimos, para poder intentarlo con el siguiente mensaje
                System.err.println("No se pudo enviar el mensaje: " + e.getMessage());
            } // fin del try del envío
        } // fin del while
    } // fin de sendLoop
} // fin de la clase ChatClient
