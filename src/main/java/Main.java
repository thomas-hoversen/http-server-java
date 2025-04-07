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
  private static final String BAD_RESPONSE = "HTTP/1.1 404 Not Found\r\n\r\n";

  public static void main(String[] args) {

    System.out.println("Logs from your program will appear here!");

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts your program quite often, setting SO_REUSEADDR
       // ensures that we don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       Socket client = serverSocket.accept(); // Wait for connection from client.
       System.out.println("accepted new connection");

       // todo get inputStream bytes, convert to string, then parse request:
       // GET /index.html HTTP/1.1\r\nHost: localhost:4221\r\nUser-Agent: curl/7.64.1\r\nAccept: */*\r\n\r\n
       String message = client.getInputStream().readAllBytes().toString();
       String[] parts = message.split("\r\n");
       String requestLine = "";
       String headers = "";
       String body = "";
       String urlPath = "";
       if (parts.length == 0) {
           client.getOutputStream().write(BAD_RESPONSE.getBytes());
       } else {
           requestLine = parts[0];
       }
       if (parts.length > 1) {
           headers = parts[1];
       }
       if (parts.length > 2) {
           body = parts[2];
       }
       if (!isGoodUrlPath(requestLine)) {
           client.getOutputStream().write(BAD_RESPONSE.getBytes());
       }
       client.getOutputStream().write(GOOD_RESPONSE.getBytes());
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  private static boolean isGoodUrlPath(String requestLine) {
      String[] parts = requestLine.split(" ");
      String url = parts[1];
      String[] splitUrl = url.split("/");
      return splitUrl.length <= 2;
  }
}
