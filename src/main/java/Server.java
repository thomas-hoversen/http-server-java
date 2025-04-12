import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Server implements Runnable {

    private final String HTTP_TYPE = "HTTP/1.1";

    private final String PLAINTEXT_CONTENT_TYPE = "text/plain";

    private final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private final String GOOD_RESPONSE = "200 OK";

    private final String CREATED_RESPONSE = "201 Created";

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

            // first line is the request line
            Optional<String> requestLine = Optional.ofNullable(reader.readLine());
            System.out.println("requestline: " + requestLine);

            if (requestLine.isEmpty()) {
                System.out.println("Empty request line detected");
                return;
            }

            // headers
            Map<String, String> headers = new HashMap<>();
            getHeaders(reader, headers);

            String[] splitUrl = getUrlParts(requestLine.get());

            // GET or POST
            String methodType = getHttpMethodType(requestLine.get());

            if (!isGoodUrlPath(splitUrl)) {
                System.out.println("invalid url: " + Arrays.toString(splitUrl));
                socket.getOutputStream().write(buildEmptyBody(BAD_RESPONSE).getBytes());
            } else {
                if (splitUrl.length == 0) {
                    socket.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
                } else {
                    String endpoint = splitUrl[1];
                    switch (endpoint) {
                        case "echo":
                            System.out.println("echo endpoint");
                            String echoText = splitUrl[2];
                            String echoResponse = buildGoodResponseWithBody(echoText, PLAINTEXT_CONTENT_TYPE, GOOD_RESPONSE);
                            System.out.println("echo response: " + echoResponse);
                            socket.getOutputStream().write(echoResponse.getBytes());
                            break;
                        case "user-agent":
                            System.out.println("user-agent endpoint");
                            String userAgentResponse = buildGoodResponseWithBody(headers.get("user-agent"), PLAINTEXT_CONTENT_TYPE, GOOD_RESPONSE);
                            System.out.println("userAgentResponse: " + userAgentResponse);
                            socket.getOutputStream().write(userAgentResponse.getBytes());
                            break;
                        case "files":
                            System.out.println("files endpoint");
                            System.out.println("methodType: " + methodType);
                            String response;
                            String filename = splitUrl[2]; // filename
                            String directory = filesDir + filename;
                            if (methodType.equals("GET")) {
                                Path path = Path.of(directory); // directory/filename
                                response = buildGETFilesResponse(path);
                            } else {
                                // POST
                                String body = getBody(reader, headers);
                                Boolean isSaved = saveFile(directory, body);
                                response = isSaved ? buildEmptyBody(CREATED_RESPONSE) : buildEmptyBody(BAD_RESPONSE);
                            }
                            socket.getOutputStream().write(response.getBytes());
                            break;
                        default:
                            socket.getOutputStream().write(buildEmptyBody(GOOD_RESPONSE).getBytes());
                            break;
                    }
                }
            }

            // flush output stream to ensure response is sent, then close socket
            socket.getOutputStream().flush();
            socket.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildGETFilesResponse(Path path) {
        try {
            // read the content as a string
            String content = Files.readString(path);
            // build and return the response
            return buildGoodResponseWithBody(content, OCTET_STREAM_CONTENT_TYPE, GOOD_RESPONSE);
        } catch (Exception e) {
            System.out.println("Error trying to read from path: " + path);
            return buildEmptyBody(BAD_RESPONSE);
        }
    }

    private Boolean saveFile(String filename, String body) {
        try {
            System.out.println("inside saveFile method: " + filename);
            Path file = Paths.get(filename);
            Files.createDirectories(file.getParent());
            Files.writeString(file, body, StandardCharsets.UTF_8);
            return true;
        } catch (Exception e) {
            System.out.println("Error trying to save file to path: " + filename + " with body: " + body);
            e.printStackTrace();
            return false;
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

    private String getHttpMethodType(String s) {
        return s.split(" ")[0];
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
            System.out.println("Current line in headers method: " + line);
        }
        System.out.println("headers: " + headerMap.toString());
    }

    private String getBody(BufferedReader reader, Map<String, String> headerMap) throws IOException {
        int length = Integer.parseInt(headerMap.getOrDefault("content-length", "0"));
        if (length == 0) return "";

        char[] buf = new char[length];
        int read = 0;
        while (read < length) {
            int r = reader.read(buf, read, length - read);
            if (r == -1) throw new IOException("Client closed connection prematurely");
            read += r;
        }
        return new String(buf);
    }


    private String buildGoodResponseWithBody(String body, String contentType, String httpMessage) {
        System.out.println("buildGoodResponseWithBody(String body): " + body);
        return HTTP_TYPE + " " + httpMessage + "\r\nContent-Type: " + contentType + "\r\nContent-Length: " + body.length() + "\r\n\r\n" + body;
    }

    /*
    Generate a response without headers or a body.
    */
    private String buildEmptyBody(String response) {
        return HTTP_TYPE + " " + response + "\r\nContent-Length: 0\r\n\r\n";
    }
}
