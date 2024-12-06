import java.io.*;
import java.net.*;
import java.util.*;
import java.util.Base64;

public class servidorSecundario {
    private static final int PUERTO = 5001;
    private static Map<String, String> archivos = new HashMap<>();
    private static Set<String> archivosRecibidos = new HashSet<>(); // Para controlar los archivos ya recibidos
    private static final String RUTA_ARCHIVOS = "C:\\Users\\juani\\OneDrive\\Documentos\\ESCUELA\\SEPTIMO SEMESTRE\\SISTEMAS OPERATIVOS II\\UNIDAD 6\\";
    private static final String SERVER_PRINCIPAL_IP = "localhost"; // Dirección IP del servidor principal
    private static final int SERVER_PRINCIPAL_PORT = 5000; // Puerto del servidor principal

    public static void main(String[] args) {
        // Cargar archivos desde el disco al iniciar el servidor
        cargarArchivosDesdeDisco();

        try (ServerSocket serverSocket = new ServerSocket(PUERTO)) {
            System.out.println("Servidor Secundario iniciado en el puerto " + PUERTO + "...");

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Cliente conectado: " + socket);
                new Thread(new ManejadorCliente(socket)).start();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para cargar archivos desde el disco al iniciar el servidor
    private static void cargarArchivosDesdeDisco() {
        File dir = new File(RUTA_ARCHIVOS);
        if (dir.exists() && dir.isDirectory()) {
            // Cargar archivos desde disco
            for (File archivo : dir.listFiles()) {
                if (archivo.isFile()) {
                    try (BufferedReader reader = new BufferedReader(new FileReader(archivo))) {
                        String contenido = reader.readLine(); // Leer el contenido del archivo
                        archivos.put(archivo.getName(), contenido); // Guardarlo en la estructura de datos
                    } catch (IOException e) {
                        System.out.println("Error al cargar archivo desde disco: " + e.getMessage());
                    }
                }
            }
        }
    }

    private static class ManejadorCliente implements Runnable {
        private Socket socket;

        public ManejadorCliente(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (BufferedReader entrada = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter salida = new PrintWriter(socket.getOutputStream(), true)) {

                String mensaje;
                while ((mensaje = entrada.readLine()) != null) {
                    System.out.println("Mensaje recibido del cliente: " + mensaje);

                    String[] partes = mensaje.split(":", 3);
                    String operacion = partes[0];

                    if (operacion.equals("WRITE") && partes.length == 3) {
                        manejarEscritura(partes[1], partes[2], salida);
                    } else if (operacion.equals("READ") && partes.length == 2) {
                        manejarLectura(partes[1], salida);
                    } else {
                        salida.println("Operación no reconocida o mensaje mal formado.");
                    }
                }
            } catch (IOException e) {
                System.out.println("Error en la comunicación con el cliente: " + e.getMessage());
                e.printStackTrace();
            }
        }

        private void manejarEscritura(String nombreArchivo, String contenido, PrintWriter salida) {
            if (!archivosRecibidos.contains(nombreArchivo)) {
                archivos.put(nombreArchivo, contenido);
                archivosRecibidos.add(nombreArchivo);

                System.out.println("Archivo '" + nombreArchivo + "' guardado en el servidor secundario.");
                guardarArchivoEnDisco(nombreArchivo, contenido);

                // Replicar el archivo al servidor principal, enviando el contenido encriptado
                String contenidoEncriptado = encriptar(contenido);
                replicarEnServidorPrincipal(nombreArchivo, contenidoEncriptado);
            }
            salida.println("Archivo '" + nombreArchivo + "' guardado exitosamente en el servidor secundario.");
        }

        private void manejarLectura(String nombreArchivo, PrintWriter salida) {
            if (archivos.containsKey(nombreArchivo)) {
                String contenido = archivos.get(nombreArchivo);
                salida.println(contenido);  // El contenido se envía tal cual (sin encriptación)
            } else {
                salida.println("Archivo no encontrado.");
            }
        }

        private void guardarArchivoEnDisco(String nombreArchivo, String contenido) {
            try {
                File dir = new File(RUTA_ARCHIVOS);
                if (!dir.exists()) {
                    dir.mkdirs();
                }

                try (BufferedWriter writer = new BufferedWriter(new FileWriter(RUTA_ARCHIVOS + nombreArchivo))) {
                    writer.write(contenido);  // Guardar el contenido como texto plano
                }
            } catch (IOException e) {
                System.out.println("Error al guardar archivo en disco: " + e.getMessage());
            }
        }

        // Método para replicar archivo al servidor principal
        private void replicarEnServidorPrincipal(String nombreArchivo, String contenidoEncriptado) {
            try (Socket socketPrincipal = new Socket(SERVER_PRINCIPAL_IP, SERVER_PRINCIPAL_PORT);
                 PrintWriter salidaPrincipal = new PrintWriter(socketPrincipal.getOutputStream(), true)) {

                salidaPrincipal.println("WRITE:" + nombreArchivo + ":" + contenidoEncriptado);  // Enviamos el contenido encriptado
                System.out.println("Archivo '" + nombreArchivo + "' replicado en el servidor principal.");
            } catch (IOException e) {
                System.out.println("Error al replicar en el servidor principal.");
                e.printStackTrace();
            }
        }

        // Método para encriptar contenido
        private String encriptar(String contenido) {
            return Base64.getEncoder().encodeToString(contenido.getBytes());
        }
    }
}