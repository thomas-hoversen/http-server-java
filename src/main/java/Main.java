import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

    private static final int  PORT             = 4221;
    private static final int  THREAD_POOL_SIZE = 10;
    private static final Logger LOG            = Logger.getLogger(Main.class.getName());

    private static String filesDir = "";

    public static void main(String[] args) {
        configureLogging();
        LOG.info("Server starting…");

        // Parse CLI args
        for (int i = 0; i < args.length; i++) {
            if ("--directory".equals(args[i]) && i + 1 < args.length) {
                filesDir = args[i + 1];
                LOG.config(() -> "Files directory set to: " + filesDir);
                i++; // skip value
            } else {
                int finalI = i;
                LOG.config(() -> "Unknown arg[" + finalI + "]: " + args[finalI]);
            }
        }

        ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            serverSocket.setReuseAddress(true);
            LOG.info(() -> "Listening on port " + PORT);

            while (true) {
                Socket client = serverSocket.accept();
                LOG.fine(() -> "Accepted connection from " + client.getRemoteSocketAddress());
                executor.execute(new Server(client, filesDir));
            }

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Fatal I/O error", e);
        }
    }

    private static void configureLogging() {
        // Keep the default console handler but bump the default level
        Logger root = Logger.getLogger("");
        root.setLevel(Level.FINE);                 // show INFO, CONFIG, FINE…
        root.getHandlers()[0].setLevel(Level.FINE);
    }
}