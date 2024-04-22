import java.rmi.Naming;

public class ClienteRMI {
    // Declaración de variables estáticas para las dimensiones de las matrices
    static int N = 12;
    static int M = 4;

    // Método principal
    public static void main(String[] args) throws Exception {

        // Iniciar matrices A y B
        long[][] A = iniciaMatrizA(N, M);
        long[][] B = iniciaMatrizB(M, N);

        // Separación de la matriz A en submatrices
        long[][][] Anew = new long[6][N / 6][M];
        for (int i = 0; i < 6; i++) {
            Anew[i] = separa_matriz(A, i * N / 6);
        }

        // Obtención de la transpuesta de la matriz B
        long[][] Bt = obtener_Transpuesta(B);

        // Separación de la matriz transpuesta B en submatrices
        long[][][] Bnew = new long[6][N / 6][M];
        for (int i = 0; i < 6; i++) {
            Bnew[i] = separa_matriz(Bt, i * N / 6);
        }

        // Creación de la matriz resultante C
        long[][][] Cnew = new long[72][N / 6][N / 6];

        // Búsqueda de los objetos remotos para realizar la multiplicación
        String url_1 = "rmi://10.0.0.4/dani";
        String url_2 = "rmi://10.0.1.4/dani";
        String url_3 = "rmi://10.0.1.5/dani";

        InterfaceRMI r_1 = (InterfaceRMI) Naming.lookup(url_1);
        InterfaceRMI r_2 = (InterfaceRMI) Naming.lookup(url_2);
        InterfaceRMI r_3 = (InterfaceRMI) Naming.lookup(url_3);

        // Creación de los hilos
        Thread[] threads = new Thread[6];
        for (int i = 0; i < 6; i++) {
            final int index = i;
            threads[i] = new Thread(() -> {
                try {
                    // Multiplicar las submatrices y almacenar el resultado en Cnew
                    for (int j = 0; j < 6; j++) {
                        if (index < 12) {
                            Cnew[index * 6 + j] = r_1.multiplica_matriz(Anew[index], Bnew[j], N, M);
                        } else if (index < 24) {
                            Cnew[index * 6 + j] = r_2.multiplica_matriz(Anew[index], Bnew[j], N, M);
                        } else {
                            Cnew[index * 6 + j] = r_3.multiplica_matriz(Anew[index], Bnew[j], N, M);
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            threads[i].start();
        }

        // Esperar a que todos los hilos terminen
        for (Thread thread : threads) {
            thread.join();
        }

        // Acomodación de las submatrices en la matriz resultante C
        long[][] C = new long[N][N];
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 6; j++) {
                acomoda_matriz(C, Cnew[i * 6 + j], (N / 6) * i, (N / 6) * j);
            }
        }

        System.out.println("++++++++++++++++++++++++++++++++++++");
        System.out.println("Matriz A");
        // imprimirMatriz(A);
        System.out.println("++++++++++++++++++++++++++++++++++++");
        System.out.println("Matriz Bt");
        // imprimirMatriz(Bt);
        System.out.println("++++++++++++++++++++++++++++++++++++");
        System.out.println("Matriz C");
        System.out.println("++++++++++++++++++++++++++++++++++++");
        // imprimirMatriz(C);

        // Cálculo del checksum de la matriz resultante C
        long checksum = calcular_Checksum(C);
        System.out.println("CheckSum: " + checksum);
    }

    public static long[][] iniciaMatrizA(int N, int M) {
        long[][] A = new long[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                A[i][j] = i + 2 * j;
            }
        }
        return A;
    }

    public static long[][] iniciaMatrizB(int N, int M) {
        long[][] B = new long[N][M];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < M; j++) {
                B[i][j] = 3 * i - j;
            }
        }
        return B;
    }

    // Método para obtener la transpuesta de una matriz
    public static long[][] obtener_Transpuesta(long[][] matriz) {
        int filas = matriz.length;
        int columnas = matriz[0].length;
        long[][] transpuesta = new long[columnas][filas];
        for (int i = 0; i < filas; i++) {
            for (int j = 0; j < columnas; j++) {
                transpuesta[j][i] = matriz[i][j];
            }
        }
        return transpuesta;
    }

    // Método para separar una matriz en submatrices
    static long[][] separa_matriz(long[][] A, int inicio) {
        int filasSubmatriz = N / 6;
        long[][] Ma = new long[filasSubmatriz][M];
        for (int i = 0; i < filasSubmatriz; i++) {
            for (int j = 0; j < M; j++) {
                if (i + inicio < A.length) { // Verifica que el índice no exceda los límites de la matriz
                    Ma[i][j] = A[i + inicio][j];
                } else {
                    // Si el índice excede los límites de la matriz, asigna un valor predeterminado
                    Ma[i][j] = 0; // O cualquier valor predeterminado que desees
                }
            }
        }
        return Ma;
    }

    static long[][] acomoda_matriz(long[][] C, long[][] A, int renglon, int columna) {
        int filasA = A.length;
        int columnasA = A[0].length;

        for (int i = 0; i < filasA; i++) {
            for (int j = 0; j < columnasA; j++) {
                C[renglon + i][columna + j] = A[i][j];
            }
        }
        return C;
    }

    // Método para calcular el checksum de una matriz
    public static long calcular_Checksum(long[][] matrix) {
        long checksum = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                checksum += matrix[i][j];
            }
        }
        return checksum;
    }

    // Método para imprimir una matriz en la consola
    public static void imprimirMatriz(long[][] matriz) {
        for (int i = 0; i < matriz.length; i++) {
            for (int j = 0; j < matriz[i].length; j++) {
                System.out.print(matriz[i][j] + "\t");
            }
            System.out.println();
        }
    }
}
