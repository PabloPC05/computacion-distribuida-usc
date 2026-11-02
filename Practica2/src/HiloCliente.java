import java.rmi.RemoteException;


public class HiloCliente extends Thread{
    MonteCarlo stub;
    long n;
    long result;
    boolean fallo = false;

    HiloCliente(MonteCarlo stub, long n){
        this.stub = stub;
        this.n = n;
    }

    public void run(){
        try {
            result = stub.conteo(n);
        } catch (RemoteException e){
            System.out.println("Error en el cliente: " + e.getMessage());
            fallo = true;
        }
    }

    public long getResult() {
        return result;
    }

    public boolean getFallo() {
        return fallo;
    }
        
}
