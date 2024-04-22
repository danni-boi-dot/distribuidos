import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

// Definición de la clase ClaseRMI que implementa la interfaz InterfaceRMI y extiende UnicastRemoteObject
public class ClaseRMI extends UnicastRemoteObject implements InterfaceRMI {
    
    // Constructor de la clase ClaseRMI que llama al constructor de la superclase y puede lanzar una RemoteException
    public ClaseRMI() throws RemoteException {
        super();
    }

    // Implementación del método remoto multiplica_matriz definido en la interfaz InterfaceRMI
    public long[][] multiplica_matriz(long[][] A, long[][] BT, int N, int M) {
        // Declaración de variables para dimensiones de las matrices
        int rows = N / 6; // Número de filas en cada bloque de la matriz resultante C
        
        // Creación de la matriz resultante C que almacenará el resultado de la multiplicación de las matrices A y BT
        long[][] C = new long[rows][rows];

        // Algoritmo de multiplicación de matrices
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < rows; j++) {
                for (int k = 0; k < M; k++) {
                    // Multiplicación de cada elemento de las matrices A y BT y suma del resultado en la matriz C
                    C[i][j] += A[i][k] * BT[j][k];
                }
            }
        }
        
        // Devolución de la matriz resultante C
        return C;
    }
}
