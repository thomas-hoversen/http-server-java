import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Main {

  private static final int PORT = 4221;
  // scale thread pool size to machine
  private static final int THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
  private static final Logger LOG = Logger.getLogger(Main.class.getName());

  private static String FILES_DIR = "";

  public static void main(String[] args) {
    configureLogging();
    LOG.info("Server starting…");

    // Parse CLI args
    for (int i = 0; i < args.length; i++) {
      if ("--directory".equals(args[i]) && i + 1 < args.length) {
        FILES_DIR = args[i + 1];
        LOG.config(() -> "Files directory set to: " + FILES_DIR);
        i++; // skip value
      } else {
        int finalI = i;
        LOG.config(() -> "Unknown arg[" + finalI + "]: " + args[finalI]);
      }
    }

    ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

    /*  Add a shutdown‑hook so Ctrl‑C (SIGINT) or normal JVM shutdown
    drains the pool instead of leaving threads & sockets hanging.   */
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      LOG.info("Shutdown requested – terminating executor…");
      executor.shutdownNow();
      try {
        if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
          LOG.warning("Executor did not terminate within 30 s");
        }
      } catch (InterruptedException ie) {
        Thread.currentThread().interrupt();
      }
    }));

    try (ServerSocket serverSocket = new ServerSocket(PORT)) {

      // for quick restarts / testing, allows the socket to be bound even though a previous connection is in a timeout state.
      serverSocket.setReuseAddress(true);

      LOG.info(() -> "Listening on port " + PORT);

      while (true) {
        Socket client = serverSocket.accept();
        LOG.fine(() -> "Accepted connection from " + client.getRemoteSocketAddress());

        /*  Wrap each client socket in try‑with‑resources so it ALWAYS closes,
        even if the Server handler throws.  */
        executor.execute(() -> {
          try (Socket c = client) { // auto‑close after run()
            c.setSoTimeout(10_000); // leave socket connection alive
            new Server(c, FILES_DIR).run();
          } catch (IOException e) {
            LOG.log(Level.WARNING, "Error handling client", e);
          }
        });

      }

    } catch (IOException e) {
      LOG.log(Level.SEVERE, "Fatal I/O error", e);
    }
  }

  private static void configureLogging() {
    // Keep the default console handler but bump the default level
    Logger root = Logger.getLogger("");
    root.setLevel(Level.INFO);                 // show INFO, CONFIG, FINE…
    root.getHandlers()[0].setLevel(Level.FINE);
  }
}