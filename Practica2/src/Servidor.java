import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.RemoteException;

public class Servidor {
    public static  void main(String args[]) {
        try {
            double factor = 1.0;
            if(args.length > 1){
                factor = Double.parseDouble(args[1]);
            }
            MonteCarloImpl montecarlo = new MonteCarloImpl(factor);
            Registry registry = LocateRegistry.getRegistry();
            registry.rebind(args[0], montecarlo);
            System.out.println("Servidor listo.");
            
        } catch (RemoteException e) {
            System.out.println("Error en el servidor: " + e.getMessage());
        }
    }
}
