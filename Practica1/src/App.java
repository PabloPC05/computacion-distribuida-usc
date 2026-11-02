// Traemos la herramienta Scanner, que sirve para leer lo que el usuario escribe en la consola
import java.util.Scanner;

/*
 * Programa principal del chat.
 * Su única tarea es preguntar el apodo al usuario y poner en marcha el chat.
 * Todo lo demás lo hacen las otras clases:
 *   - ChatClient:   envía los mensajes que escribes.
 *   - ChatReceiver: recibe los mensajes de los demás.
 *   - Protocol:     define cómo se escribe y se lee un mensaje.
 */
// Una clase es un "molde" que agrupa código; esta se llama App
public class App {
    // main es el método por el que empieza a ejecutarse cualquier programa Java
    public static void main(String[] args) throws Exception {
        // Creamos el lector de consola. Hacemos uno solo y lo compartimos para no mezclar lecturas
        Scanner scanner = new Scanner(System.in);
        // Pedimos el apodo al usuario y lo guardamos en la variable nickname
        String nickname = getNickname(scanner);
        // Creamos el chat, indicándole el apodo de esta persona
        ChatClient client = new ChatClient(nickname);
        // Arrancamos el chat; el programa se queda aquí hasta que el usuario escribe "exit"
        client.start(scanner);
        // Cuando el chat termina, cerramos el lector de consola
        scanner.close();
    } // fin de main

    // Método que pide el apodo y devuelve uno válido
    public static String getNickname(Scanner scanner) {
        // Mostramos en pantalla la petición
        System.out.println("Introduce tu apodo: ");
        // Leemos lo que escribe el usuario hasta que pulsa Enter
        String nickname = scanner.nextLine();
        // Mientras el apodo NO sea válido (vacío o con ':'), repetimos la petición.
        // Los ':' no se permiten porque separan el apodo del mensaje.
        while (!Protocol.isValidNickname(nickname)) {
            // Explicamos el error y volvemos a pedir el apodo
            System.out.println("El apodo no puede contener ':' y no puede estar vacío. Ingrese su apodo nuevamente: ");
            // Leemos el nuevo intento
            nickname = scanner.nextLine();
        } // fin del while: aquí el apodo ya es válido
        // Devolvemos el apodo válido a quien llamó al método (main)
        return nickname;
    } // fin de getNickname
} // fin de la clase App
