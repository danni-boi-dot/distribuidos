import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.URL;
import java.math.MathContext;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class ServidorB {

    public static void main(String[] args) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(3);
        ServerHTTPB server = new ServerHTTPB(1025, executorService); // Cambié el puerto a 8081
        server.start();
        System.out.println("Servidor HTTP B iniciado en el puerto 80...");
    }

    static class ServerHTTPB {
        private final int port;
        private final ExecutorService executorService;

        public ServerHTTPB(int port, ExecutorService executorService) {
            this.port = port;
            this.executorService = executorService;
        }

        public void start() throws IOException {
            HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
            server.createContext("/pi", new PiHandler());
            server.setExecutor(executorService);
            server.start();
        }
    }

    static class PiHandler implements HttpHandler {
        private static final String[] IPS_MAQUINAS_VIRTUALES = { "20.81.203.216", "20.81.206.105", "20.81.206.82" };
        private static final int[] PUERTOS_MAQUINAS_VIRTUALES = { 8080, 8081, 8082 };

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Solicitud recibida por el Servidor B.");
            if ("GET".equals(exchange.getRequestMethod())) {
                try {
                    // Dividir el intervalo [0, 3000] en tres intervalos
                    int intervalSize = 1500;
                    BigDecimal sumatoria = BigDecimal.ZERO;
                    for (int i = 0; i < 3; i++) {
                        long kInicial = i * intervalSize;
                        long kFinal = (i + 1) * intervalSize - 1;
                        System.out
                                .println("Solicitando a servidor A" + (i + 1) + ": [" + kInicial + ", " + kFinal + "]");
                        // Construir la URL con los parámetros para el servidor A
                        String url = "http://" + IPS_MAQUINAS_VIRTUALES[i] + ":" + PUERTOS_MAQUINAS_VIRTUALES[i]
                                + "/?kInicial=" + kInicial + "&kFinal=" + kFinal; // Cambiado el endpoint a "/"
                        // Realizar la solicitud HTTP al servidor A
                        BigDecimal resultado = hacerSolicitudHTTP(url);
                        sumatoria = sumatoria.add(resultado);
                        // Manejar el resultado según sea necesario
                        System.out.println("Resultado desde servidor A " + (i + 1) + ": " + resultado);
                    }
                    // Calcular el valor de PI con la fórmula adecuada
                    BigDecimal pi = calcularPi(sumatoria);
                    // Enviar respuesta al cliente
                    String response = pi.toString();
                    exchange.sendResponseHeaders(200, response.length());
                    OutputStream os = exchange.getResponseBody();
                    os.write(response.getBytes());
                    os.close();
                    System.out.println("Respuesta enviada al cliente: " + response);
                } catch (Exception e) {
                    exchange.sendResponseHeaders(500, 0); // Internal Server Error
                    System.out.println("Error interno del servidor B: " + e.getMessage());
                }
            } else {
                exchange.sendResponseHeaders(405, 0); // Method Not Allowed
            }
        }

        private BigDecimal hacerSolicitudHTTP(String url) throws IOException {
            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
            connection.setRequestMethod("GET");
            int responseCode = connection.getResponseCode();
            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                try {
                    // Eliminar espacios en blanco y caracteres no válidos antes de convertir a
                    // BigDecimal
                    String cleanedResponse = response.toString().trim().replaceAll("[^\\d.]", "");
                    return new BigDecimal(cleanedResponse);
                } catch (NumberFormatException e) {
                    throw new IOException("Error al convertir la respuesta a BigDecimal: " + e.getMessage());
                }
            } else {
                throw new IOException("Error en la solicitud HTTP: " + responseCode);
            }
        }

        private BigDecimal calcularPi(BigDecimal sumatoria) {
            BigDecimal factor = new BigDecimal(2).sqrt(new MathContext(100)).multiply(new BigDecimal(2))
                    .divide(new BigDecimal(9801), MathContext.DECIMAL128);
            return BigDecimal.ONE.divide(factor.multiply(sumatoria), MathContext.DECIMAL128);
        }
    }
}
