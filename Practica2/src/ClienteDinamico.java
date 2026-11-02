import java.util.*;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;

public class ClienteDinamico {
    public static void main(String args[]){ 
        long n = 0, count = 0;
        Scanner sc = new Scanner(System.in);
        System.out.print("Numero de puntos a generar: ");
        n = sc.nextLong();
        System.out.print("Tam. de bloque: ");
        long bloque = sc.nextLong();
        sc.close();
        try {
            Registry registry = LocateRegistry.getRegistry();
            int length = args.length;
            double tiempo = 0;
            // Crear un arreglo de hilos para almacenar los hilos del cliente
            HiloClienteDinamico[] hilos = new HiloClienteDinamico[length];
            Bolsa bolsa = new Bolsa(n, bloque);

            for(int i = 0; i < length; i++){
                hilos[i] = new HiloClienteDinamico((MonteCarlo) registry.lookup(args[i]), bolsa);
            }
            
            long inicio = System.nanoTime();

            for(int i = 0; i < length; i++){
                hilos[i].start();
            }

            boolean fallo = false;
            for(int i = 0; i < length; i++){
                hilos[i].join();
                count += hilos[i].getResult();
                if(hilos[i].getFallo()){
                    System.out.println("El servidor " + args[i] + " ha fallado");
                    fallo = true;
                }
            }

            long fin = System.nanoTime();

            if(fallo){
                System.out.println("Algun servidor ha fallado: el resultado no es valido");
                return;
            }

            tiempo += (double) (fin - inicio) / 1e9;
            System.out.println("Conteo: " + count);
            double result = (double) count / n;
            System.out.println("Resultado: " + result);
            System.out.println("Tiempo de ejecucion: " + tiempo + " segundos");
            for(int i = 0; i < length; i++){
                // Mostrar el número de bloques procesados por cada hilo
                System.out.println("Bloques procesados por el hilo " + (i+1) + ": " + hilos[i].getNum_bloques());
            }
        } catch (Exception e) {
            System.out.println("Error en el cliente: " + e.getMessage());
        }
    }
}
