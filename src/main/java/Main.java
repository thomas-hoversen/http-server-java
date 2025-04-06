import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class Main {

  /*
  Source: https://datatracker.ietf.org/doc/html/rfc7230#section-3
  HTTP-message   = start-line
                      *( header-field CRLF )
                      CRLF
                      [ message-body ]
   */
  private static final String GOOD_RESPONSE = "HTTP/1.1 200 OK\r\n\r\n";

  public static void main(String[] args) {

    System.out.println("Logs from your program will appear here!");

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       Socket client = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");
       client.getOutputStream().write(GOOD_RESPONSE.getBytes());
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }
}
