import javax.net.ssl.SSLServerSocketFactory;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ServerADT {

    static class Worker extends Thread {
        Socket connection;
        static int counter = 0;

        Worker(Socket connection) {
            this.connection = connection;
        }

        public void run() {
            BufferedReader input = null;
            PrintWriter output = null;
            BufferedOutputStream dataOut = null;
            try {
                input = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                output = new PrintWriter(connection.getOutputStream());
                dataOut = new BufferedOutputStream(connection.getOutputStream());

                String req = input.readLine();
                System.out.println(req);

                String ifModifiedSinceHeader = "";

                for (;;) {
                    String header = input.readLine();
                    if (header == null || header.equals(""))
                        break;
                    if (header.startsWith("If-Modified-Since"))
                        ifModifiedSinceHeader = header.split(":")[1].trim();
                }

                SimpleDateFormat rfc1123 = new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z", Locale.US);

                if (req.startsWith("GET")) {
                    String[] requestParts = req.split(" ");
                    String requestedFile = requestParts[1];

                    if (requestedFile.equals("/")) {
                        requestedFile = "/index.html";
                    }

                    File file = new File("." + requestedFile);
                    if (file.exists() && !file.isDirectory()) {
                        long lastModified = file.lastModified();
                        Date lastModifiedDate = new Date(lastModified);
                        String lastModifiedRFC1123 = rfc1123.format(lastModifiedDate);

                        Date ifModifiedSinceDate = null;
                        if (!ifModifiedSinceHeader.isEmpty()) {
                            ifModifiedSinceDate = rfc1123.parse(ifModifiedSinceHeader);
                        }
                        // Comparar la hora actual con la hora proporcionada por el cliente
                        if (ifModifiedSinceDate != null && lastModifiedDate.equals(ifModifiedSinceDate)) {
                            // Si las horas coinciden, enviar el estado 304 (Not Modified)
                            System.out.println("Response: 304 Not Modified");
                            output.println("HTTP/1.1 304 Not Modified");
                            output.println("Date: " + new Date());
                            output.println();
                            output.flush();
                        } else {
                            // Si las horas no coinciden o no se proporcionó una hora, enviar el archivo con
                            // el estado 200 (OK)
                            System.out.println("Response: 200 OK");
                            output.println("HTTP/1.1 200 OK");
                            output.println("Content-Type: " + getContentType(file.getName()));
                            output.println("Content-Length: " + file.length());
                            output.println("Date: " + new Date());
                            output.println("Last-Modified: " + lastModifiedRFC1123);
                            output.println("Content-Disposition: attachment; filename=\"" + file.getName() + "\"");
                            output.println();
                            output.flush();

                            // Envío del archivo al cliente
                            FileInputStream fileIn = new FileInputStream(file);
                            byte[] buffer = new byte[1024];
                            int bytesRead;
                            while ((bytesRead = fileIn.read(buffer)) != -1) {
                                dataOut.write(buffer, 0, bytesRead);
                            }
                            fileIn.close();
                            dataOut.flush();
                        }
                    } else {
                        // Si el archivo no existe, devolver 404 Not Found
                        System.out.println("Response: 404 Not Found");
                        output.println("HTTP/1.1 404 Not Found");
                        output.println();
                        output.println("404 Not Found");
                        output.flush();
                    }
                }

            } catch (FileNotFoundException e) {
                // Manejar la excepción de archivo no encontrado
                System.err.println("404 - Archivo no encontrado: " + e.getMessage());
                output.println("HTTP/1.1 404 Not Found");
                output.flush();
            } catch (Exception e) {
                // Manejar otras excepciones
                System.err.println("Error en la solicitud: " + e.getMessage());
            } finally {
                try {
                    if (input != null) {
                        input.close();
                    }
                    if (output != null) {
                        output.close();
                    }
                    if (dataOut != null) {
                        dataOut.close();
                    }
                    connection.close();
                } catch (Exception e2) {
                    System.err.println(e2.getMessage());
                }
            }
        }

        private String getContentType(String fileName) {
            if (fileName.endsWith(".html") || fileName.endsWith(".htm")) {
                return "text/html";
            } else if (fileName.endsWith(".css")) {
                return "text/css";
            } else if (fileName.endsWith(".js")) {
                return "application/javascript";
            } else if (fileName.endsWith(".png")) {
                return "image/png";
            } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                return "image/jpeg";
            } else if (fileName.endsWith(".gif")) {
                return "image/gif";
            } else if (fileName.endsWith(".pdf")) {
                return "application/pdf";
            } else if (fileName.endsWith(".txt")) {
                return "text/plain";
            } else {
                return "application/octet-stream";
            }
        }
    }

    public static void main(String[] args) throws IOException {
        // Configurar SSL para el servidor
        System.setProperty("javax.net.ssl.keyStore", "keystore_server.jks");
        System.setProperty("javax.net.ssl.keyStorePassword", "1234567");
        SSLServerSocketFactory socket_factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
        ServerSocket server = socket_factory.createServerSocket(8443);

        System.out.println("HTTPS server running on port: " + 8443);

        while (true) {
            Socket connection = server.accept();
            Worker w = new Worker(connection);
            w.start();
        }
    }
}
