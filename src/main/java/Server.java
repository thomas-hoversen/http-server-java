import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.util.*;

public class Server implements Runnable {

    private final String HTTP_TYPE = "HTTP/1.1";

    private final String GOOD_RESPONSE = "200 OK";

    private final String BAD_RESPONSE = "404 Not Found";

    private final List<String> endpoints = new ArrayList<>(Arrays.asList("index.html", "echo", "user-agent"));

    private final Socket socket;

    // constructor
    public Server(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        // reader for the input data
        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // headers
            Map<String, String> headers = new HashMap<>();

            // body (not needed yet
            String body = "";

            // first line is the request line
            Optional<String> requestLine = Optional.ofNullable(reader.readLine());
            System.out.println("requestline: " + requestLine);

            if (requestLine.isEmpty()) {
                System.out.println("Empty request line detected");
                return;
            }

            // then extract headers
            getHeaders(reader, headers);

            String[] splitUrl = getUrlParts(requestLine.get());

            // if it's a bad request
            // also closes streams
            if (!isGoodUrlPath(splitUrl)) {
                System.out.println("inside bad response path");
                socket.getOutputStream().write(buildEmptyBody(BAD_RESPONSE).getBytes());
            } else {
                if (splitUrl.length == 0) {
                    socket.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
                } else {
                    switch (splitUrl[1]) {
                        case "echo":
                            System.out.println("echo endpoint");
                            String echoResponse = buildGoodResponseWithBody(splitUrl[2]);
                            System.out.println("echo response: " + echoResponse);
                            socket.getOutputStream().write(echoResponse.getBytes());
                            break;
                        case "user-agent":
                            System.out.println("user-agent endpoint");
                            String userAgentResponse = buildGoodResponseWithBody(headers.get("user-agent"));
                            System.out.println("userAgentResponse: " + userAgentResponse);
                            socket.getOutputStream().write(userAgentResponse.getBytes());
                            break;
                        default:
                            socket.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
                            break;
                    }
                }
            }
            socket.getOutputStream().flush();
            socket.close(); // also closes streams
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isGoodUrlPath(String[] splitUrl) {
        if (splitUrl.length == 0) return true;
        return splitUrl.length < 2 || endpoints.contains(splitUrl[1]);
    }

    /*
    Split the request line (something like GET /echo/abc HTTP/1.1\r\n) to get the url as an array of strings.
    This program assumes all requests are GET and HTTP/1.1
    */
    private String[] getUrlParts(String s) {
        String[] parts = s.split(" ");
        String url = parts[1];
        return url.split("/");
    }

    private void getHeaders(BufferedReader reader, Map<String, String> headerMap) throws IOException {
        //todo wrap line with optional?
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


    private String buildGoodResponseWithBody(String body) {
        System.out.println("buildGoodResponseWithBody(String body): " + body);
        return HTTP_TYPE + " " + GOOD_RESPONSE + "\r\nContent-Type: text/plain\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
    }

    /*
    Generate a response without headers or a body.
    */
    private String buildEmptyBody(String response) {
        return HTTP_TYPE + " " + response + "\r\n\r\n";
    }
}
