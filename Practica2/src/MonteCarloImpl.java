import java.util.Random;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

public class MonteCarloImpl extends UnicastRemoteObject implements MonteCarlo {
    double factor;
    MonteCarloImpl(double factor) throws RemoteException {
        super();
        this.factor = factor;
    }
    public long conteo(long n)  {

        long count = 0; 
        Random rand = new Random();
        long inicio = System.nanoTime();
        for (long i = 0; i < n; i++) {
            double x = rand.nextDouble();
            double y = rand.nextDouble();
            if (y <= Math.exp(-x*x)) {
                count++;
            }
        }
        long fin = System.nanoTime();
        try {
            Thread.sleep((long)((factor -1 ) * ((fin - inicio) / 1e9) * 1000));
        } catch (InterruptedException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
        return count;
    }
    
}
