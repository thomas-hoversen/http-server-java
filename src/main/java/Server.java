import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Server implements Runnable {

    private final String HTTP_TYPE = "HTTP/1.1";

    private final String PLAINTEXT_CONTENT_TYPE = "text/plain";

    private final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private final String GOOD_RESPONSE = "200 OK";

    private final String BAD_RESPONSE = "404 Not Found";

    private final List<String> endpoints = new ArrayList<>(Arrays.asList("index.html", "echo", "user-agent", "files"));

    private final Socket socket;

    private final String filesDir;

    // constructor
    public Server(Socket socket, String filesDir) {
        this.socket = socket;
        this.filesDir = filesDir;
    }

    @Override
    public void run() {
        // reader for the input data
        BufferedReader reader = null;
        try {

            reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            // headers
            Map<String, String> headers = new HashMap<>();

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
                    String endpoint = splitUrl[1];
                    String argument;
                    switch (endpoint) {
                        case "echo":
                            System.out.println("echo endpoint");
                            argument = splitUrl[2];
                            String echoResponse = buildGoodResponseWithBody(argument, PLAINTEXT_CONTENT_TYPE);
                            System.out.println("echo response: " + echoResponse);
                            socket.getOutputStream().write(echoResponse.getBytes());
                            break;
                        case "user-agent":
                            System.out.println("user-agent endpoint");
                            String userAgentResponse = buildGoodResponseWithBody(headers.get("user-agent"), PLAINTEXT_CONTENT_TYPE);
                            System.out.println("userAgentResponse: " + userAgentResponse);
                            socket.getOutputStream().write(userAgentResponse.getBytes());
                            break;
                        case "files":
                            System.out.println("files endpoint");
                            argument = splitUrl[2];
                            Path path = Path.of(filesDir + "/" + argument); // directory/filename
                            String response = buildFilesResponse(path);
                            socket.getOutputStream().write(response.getBytes());
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

    private String buildFilesResponse(Path path) {
        try {
            // read the content as a string
            String content = Files.readString(path);
            // build and return the response
            return buildGoodResponseWithBody(content, OCTET_STREAM_CONTENT_TYPE);
        } catch (Exception e) {
            System.out.println("Error trying to read from path: " + path);
            return buildEmptyBody(BAD_RESPONSE);
        }
    }

    private boolean isGoodUrlPath(String[] splitUrl) {
        // sample url: []
        // sample url: [,apple] // random
        // sample url: [,echo, apple] // echo random
        // sample url: [,files, hello.txt] // files endpoint and filename
        System.out.println(splitUrl.length);
        for (String s : splitUrl) System.out.print(s + " ");
        // todo validates files endpoint has filename
        if (splitUrl.length < 2) return true;
        if (endpoints.contains(splitUrl[1])) {
            // ensure files endpoint comes with a filename
            if ("files".equals(splitUrl[1]) || "echo".equals(splitUrl[1])) {
                boolean valid = splitUrl.length == 3; // sample url: [,files, hello.txt] // files endpoint and filename
                if (!valid) System.out.println("Request is missing a required argument (filename or content to echo)");
                return valid;
            }
            return true;

        } else return false;
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


    private String buildGoodResponseWithBody(String body, String contentType) {
        System.out.println("buildGoodResponseWithBody(String body): " + body);
        return HTTP_TYPE + " " + GOOD_RESPONSE + "\r\nContent-Type: " + contentType + "\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
    }

    /*
    Generate a response without headers or a body.
    */
    private String buildEmptyBody(String response) {
        return HTTP_TYPE + " " + response + "\r\n\r\n";
    }
}
