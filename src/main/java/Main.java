import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  private static final int PORT = 4221;

  private static final int THREAD_POOL_SIZE = 10;

  private static String filesDir = "";

  public static void main(String[] args) {
      System.out.println("Program starting...");

      // log any command line arguments
      if (args.length > 0) {
          System.out.println("Command line arguments:");
          for (int i = 0; i < args.length; i++) {
              System.out.println(args[i]);
              if ("--directory".equals(args[i])) {
                  System.out.println("files directory for files endpoint is present: " + args[i+1]);
                  filesDir = args[i+1];
              }
          }
      }


      ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

     try {
       ServerSocket serverSocket = new ServerSocket(PORT);

       // Since the tester restarts the program quite often, setting SO_REUSEADDR
       // ensures that the tests don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       while (true) {
           Socket client = serverSocket.accept(); // Wait for connection from client.
           System.out.println("Accepted new connection");
           executor.execute(new Server(client, filesDir));
       }


     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
