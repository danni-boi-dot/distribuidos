import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.net.InetSocketAddress;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

public class ServidorA1 {

    public static void main(String[] args) throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress(8081), 0);
        ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(10);
        server.createContext("/", new MyHandler());
        server.setExecutor(threadPoolExecutor);
        server.start();
        System.out.println("Servidor HTTP iniciado en el puerto 8081...");
    }

    static class MyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            System.out.println("Solicitud recibida por el Servidor A1.");
            if ("GET".equals(exchange.getRequestMethod())) {
                String query = exchange.getRequestURI().getQuery();
                String[] params = query.split("&");
                long kInicial = 0;
                long kFinal = 0;
                for (String param : params) {
                    String[] keyValue = param.split("=");
                    if (keyValue.length == 2) {
                        String key = keyValue[0];
                        String value = keyValue[1];
                        if ("kInicial".equals(key)) {
                            try {
                                kInicial = Long.parseLong(value);
                            } catch (NumberFormatException e) {
                                exchange.sendResponseHeaders(400, 0); // Bad Request
                                return;
                            }
                        } else if ("kFinal".equals(key)) {
                            try {
                                kFinal = Long.parseLong(value);
                            } catch (NumberFormatException e) {
                                exchange.sendResponseHeaders(400, 0); // Bad Request
                                return;
                            }
                        }
                    }
                }
                BigDecimal resultado = calcularSumatoriaRamanujan(kInicial, kFinal);
                String response = resultado.toString();
                exchange.sendResponseHeaders(200, response.length());
                OutputStream os = exchange.getResponseBody();
                os.write(response.getBytes());
                os.close();
                System.out.println("Respuesta enviada: " + response);
            } else {
                exchange.sendResponseHeaders(405, 0); // Method Not Allowed
                System.out.println("Error: Método HTTP no permitido.");
            }
            exchange.close();
            System.out.println("Conexión cerrada.");
        }

        private BigDecimal calcularSumatoriaRamanujan(long kInicial, long kFinal) {
            MathContext mc = new MathContext(10, RoundingMode.HALF_UP);
            BigDecimal sumatoria = BigDecimal.ZERO;
            for (long k = kInicial; k <= kFinal; k++) {
                BigDecimal term = factorial(4 * k).multiply(new BigDecimal(1103 + 26390 * k))
                        .divide(factorial(k).pow(4).multiply(new BigDecimal(396).pow((int) (4 * k))), mc);
                sumatoria = sumatoria.add(term);
            }
            return sumatoria;
        }

        private BigDecimal factorial(long n) {
            if (n == 0) return BigDecimal.ONE;
            BigDecimal result = BigDecimal.ONE;
            for (long i = 1; i <= n; i++) {
                result = result.multiply(new BigDecimal(i));
            }
            return result;
        }
    }
}
