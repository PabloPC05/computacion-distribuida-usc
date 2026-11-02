import java.rmi.RemoteException;
public class HiloClienteDinamico extends Thread{
    MonteCarlo stub;
    Bolsa bolsa;
    long result;
    long num_bloques;
    boolean fallo = false;
    HiloClienteDinamico(MonteCarlo stub, Bolsa bolsa){
        this.stub = stub;
        this.bolsa = bolsa;
        this.result = 0;
        num_bloques = 0;
    }
    public void run(){
        long bloque;
        while((bloque = bolsa.getResto()) > 0){
            try {
                result += stub.conteo(bloque);
                num_bloques++;
            } catch (RemoteException e){
                System.out.println("Error en el cliente: " + e.getMessage());
                fallo = true;
                break;
            }
        }
    }

    public boolean getFallo() {
        return fallo;
    }

    public long getResult() {
        return result;
    }

    public long getNum_bloques() {
        return num_bloques;
    }
}
