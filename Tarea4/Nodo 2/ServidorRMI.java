    import java.rmi.Naming;

    public class ServidorRMI {
        public static void main(String[] args) throws Exception {
            // Definición de la URL de registro RMI donde se vinculará el objeto remoto
            String url = "rmi://localhost/dani";

            // Creación de una instancia de la clase ClaseRMI que implementa el objeto remoto
            ClaseRMI obj = new ClaseRMI();

            // Vinculación del objeto remoto al registro RMI utilizando la URL especificada
            Naming.rebind(url, obj);
        }
    }
