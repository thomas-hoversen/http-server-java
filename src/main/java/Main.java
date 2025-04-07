import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Main {

  private final static String HTTP_TYPE = "HTTP/1.1";
  private static final String GOOD_RESPONSE = "200 OK";
  private static final String BAD_RESPONSE = "404 Not Found";

  private static final List<String> endpoints = new ArrayList<>(Arrays.asList("index.html", "echo"));


  public static void main(String[] args) {

     System.out.println("Program starting...");

     try {
       ServerSocket serverSocket = new ServerSocket(4221);

       // Since the tester restarts the program quite often, setting SO_REUSEADDR
       // ensures that the tests don't run into 'Address already in use' errors
       serverSocket.setReuseAddress(true);

       Socket client = serverSocket.accept(); // Wait for connection from client.
       System.out.println("Accepted new connection");

       // reader for the input data
       BufferedReader reader = new BufferedReader(new InputStreamReader(client.getInputStream()));
       String requestLine = reader.readLine();
       System.out.println("requestline: " + requestLine);
       String[] splitUrl = getUrlParts(requestLine);
       System.out.println("split url length: " + splitUrl.length);
       for (String s : splitUrl) System.out.println("index: " + s);

       // if it's a bad request
         // also closes streams
       if (!isGoodUrlPath(splitUrl)) {
           System.out.println("inside bad response path");
           client.getOutputStream().write(buildEmptyBody(BAD_RESPONSE).getBytes());
       } else {
           // echo endpoint
           if (splitUrl.length == 0) {
               client.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
           } else if (Objects.equals(splitUrl[1], endpoints.get(1))) {
               System.out.println("echo endpoint");
               String echoResponse = buildGoodResponseWithBody(splitUrl[2]);
               System.out.println("echo response: " + echoResponse);
               client.getOutputStream().write(echoResponse.getBytes());
           } else {
               client.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
           }
       }
       client.getOutputStream().flush();
       client.close(); // also closes streams
     } catch (IOException e) {
       System.out.println("IOException: " + e.getMessage());
     }
  }

  private static boolean isGoodUrlPath(String[] splitUrl) {
      if (splitUrl.length == 0) return true;
      return splitUrl.length < 2 || endpoints.contains(splitUrl[1]);
  }

  /*
  Split the request line (something like GET /echo/abc HTTP/1.1\r\n) to get the url as an array of strings.
  This program assumes all requests are GET and HTTP/1.1
  */
  private static String[] getUrlParts(String s) {
      String[] parts = s.split(" ");
      String url = parts[1];
      return url.split("/");
  }

  private static String buildGoodResponseWithBody(String body) {
      return HTTP_TYPE + " " + GOOD_RESPONSE + "\r\nContent-Type: text/plain\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
  }

  /*
  Generate a response without headers or a body.
  */
  private static String buildEmptyBody(String response) {
      return HTTP_TYPE + " " + response + "\r\n\r\n";
  }
}
