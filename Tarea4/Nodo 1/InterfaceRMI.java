import java.rmi.RemoteException;
import java.rmi.Remote;

public interface InterfaceRMI extends Remote {

    public long[][] multiplica_matriz(long[][] A, long[][] B, int N, int M) throws RemoteException;

}