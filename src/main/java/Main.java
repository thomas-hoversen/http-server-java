import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Main {

  private final static String HTTP_TYPE = "HTTP/1.1";
  private static final String GOOD_RESPONSE = "200 OK";
  private static final String BAD_RESPONSE = "404 Not Found";

  private static final List<String> endpoints = new ArrayList<>(Arrays.asList("index.html", "echo", "user-agent"));


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

       // request line
       String requestLine = "";

       // headers
       Map<String, String> headers = new HashMap<>();

       // body (not needed yet
       String body = "";

       // first line is the request line
       requestLine = reader.readLine();
       System.out.println("requestline: " + requestLine);

       // then extract headers
       getHeaders(reader, headers);

       String[] splitUrl = getUrlParts(requestLine);

       // if it's a bad request
       // also closes streams
       if (!isGoodUrlPath(splitUrl)) {
           System.out.println("inside bad response path");
           client.getOutputStream().write(buildEmptyBody(BAD_RESPONSE).getBytes());
       } else {
           if (splitUrl.length == 0) {
               client.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
           } else {
               switch (splitUrl[1]) {
                   case "echo":
                       System.out.println("echo endpoint");
                       String echoResponse = buildGoodResponseWithBody(splitUrl[2]);
                       System.out.println("echo response: " + echoResponse);
                       client.getOutputStream().write(echoResponse.getBytes());
                       break;
                   case "user-agent":
                       System.out.println("user-agent endpoint");
                       String userAgentResponse = buildGoodResponseWithBody(headers.get("user-agent"));
                       System.out.println("userAgentResponse: " + userAgentResponse);
                       client.getOutputStream().write(userAgentResponse.getBytes());
                       break;
                   default:
                       client.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
                       break;
               }
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

  private static void getHeaders(BufferedReader reader, Map<String, String> headerMap) throws IOException {
      String line;
      while (!(line = reader.readLine()).isEmpty()) {
          if (line.contains(":")) {
              // add header to map
              line = line.toLowerCase();
              String[] splitLine = line.split(":");
              headerMap.put(splitLine[0], splitLine[1].strip());
          }
      }
      System.out.println("headers: " + headerMap.toString());
  }


  private static String buildGoodResponseWithBody(String body) {
      System.out.println("buildGoodResponseWithBody(String body): " + body);
      return HTTP_TYPE + " " + GOOD_RESPONSE + "\r\nContent-Type: text/plain\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
  }

  /*
  Generate a response without headers or a body.
  */
  private static String buildEmptyBody(String response) {
      return HTTP_TYPE + " " + response + "\r\n\r\n";
  }
}
