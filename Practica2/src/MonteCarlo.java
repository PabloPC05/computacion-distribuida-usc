import java.rmi.Remote;
import java.rmi.RemoteException;
public interface MonteCarlo extends Remote{
    public long conteo(long n) throws RemoteException;
}