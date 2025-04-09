import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Main {

  private static final int PORT = 4221;

  private static final int THREAD_POOL_SIZE = 10;

  public static void main(String[] args) {
      System.out.println("Program starting...");

      ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);

     try {
       ServerSocket serverSocket = new ServerSocket(PORT);

       // Since the tester restarts the program quite often, setting SO_REUSEADDR
       // ensures that the tests don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       while (true) {
           Socket client = serverSocket.accept(); // Wait for connection from client.
           System.out.println("Accepted new connection");
           executor.execute(new Server(client));
       }


     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
