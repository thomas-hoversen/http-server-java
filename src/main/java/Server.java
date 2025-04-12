import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Server implements Runnable {

    private static final Logger LOG = Logger.getLogger(Server.class.getName());

    private static final String HTTP_TYPE               = "HTTP/1.1";
    private static final String PLAINTEXT_CONTENT_TYPE  = "text/plain";
    private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";

    private static final String OK_RESPONSE      = "200 OK";
    private static final String CREATED_RESPONSE = "201 Created";
    private static final String NOT_FOUND        = "404 Not Found";

    private static final List<String> ENDPOINTS =
            Arrays.asList("index.html", "echo", "user-agent", "files");

    private final Socket socket;
    private final String filesDir;

    public Server(Socket socket, String filesDir) {
        this.socket = socket;
        this.filesDir = filesDir;
    }

    @Override
    public void run() {
        try (BufferedReader reader =
                     new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            /* ---------- Request line ---------- */
            String requestLine = reader.readLine();
            LOG.fine(() -> "Request line: " + requestLine);

            if (requestLine == null || requestLine.isEmpty()) {
                LOG.fine("Empty request line, closing connection.");
                return;
            }

            /* ---------- Headers ---------- */
            Map<String, String> headers = readHeaders(reader);
            LOG.finer(() -> "Headers: " + headers);

            String[] urlParts   = getUrlParts(requestLine);
            String   method     = getMethod(requestLine);

            if (!isValidPath(urlParts)) {
                LOG.fine(() -> "Invalid URL: " + Arrays.toString(urlParts));
                write(buildEmptyBody(NOT_FOUND));
                return;
            }

            /* ---------- Routing ---------- */
            if (urlParts.length < 2) {
                write(buildEmptyBody(OK_RESPONSE));
                return;
            }

            String endpoint = urlParts[1];
            switch (endpoint) {
                case "echo" -> handleEcho(urlParts);
                case "user-agent" -> handleUserAgent(headers);
                case "files" -> handleFiles(urlParts, method, reader, headers);
                default -> write(buildEmptyBody(OK_RESPONSE));
            }

        } catch (IOException e) {
            LOG.log(Level.SEVERE, "I/O error while handling client", e);
        } finally {
            try {
                socket.getOutputStream().flush();
                socket.close();
            } catch (IOException ignored) { }
        }
    }

    /* ---------- Handlers ---------- */

    private void handleEcho(String[] urlParts) throws IOException {
        if (urlParts.length < 3) {
            write(buildEmptyBody(NOT_FOUND));
            return;
        }
        String msg = urlParts[2];
        LOG.fine(() -> "Echoing: " + msg);
        write(buildResponse(msg, PLAINTEXT_CONTENT_TYPE, OK_RESPONSE));
    }

    private void handleUserAgent(Map<String, String> headers) throws IOException {
        String ua = headers.getOrDefault("user-agent", "");
        LOG.fine(() -> "User-Agent echoed: " + ua);
        write(buildResponse(ua, PLAINTEXT_CONTENT_TYPE, OK_RESPONSE));
    }

    private void handleFiles(String[] urlParts,
                             String method,
                             BufferedReader reader,
                             Map<String, String> headers) throws IOException {

        if (urlParts.length < 3) {
            write(buildEmptyBody(NOT_FOUND));
            return;
        }
        String filename = urlParts[2];
        String path     = filesDir + filename;
        LOG.fine(() -> method + " /files/" + filename);

        if ("GET".equals(method)) {
            write(buildGetFileResponse(Path.of(path)));
        } else { // POST
            String body = readBody(reader, headers);
            boolean ok  = saveFile(path, body);
            write(buildEmptyBody(ok ? CREATED_RESPONSE : NOT_FOUND));
        }
    }

    /* ---------- Helpers ---------- */

    private Map<String, String> readHeaders(BufferedReader reader) throws IOException {
        Map<String, String> map = new HashMap<>();
        String line;
        while ((line = reader.readLine()) != null && !line.isEmpty()) {
            int colon = line.indexOf(':');
            if (colon > 0) {
                map.put(line.substring(0, colon).trim().toLowerCase(),
                        line.substring(colon + 1).trim());
            }
        }
        return map;
    }

    private String readBody(BufferedReader reader, Map<String, String> headers) throws IOException {
        int len = Integer.parseInt(headers.getOrDefault("content-length", "0"));
        char[] buf = new char[len];
        int read = 0;
        while (read < len) {
            int r = reader.read(buf, read, len - read);
            if (r == -1) throw new IOException("Client closed connection prematurely");
            read += r;
        }
        return new String(buf);
    }

    private boolean saveFile(String path, String body) {
        try {
            Path p = Paths.get(path);
            Files.createDirectories(p.getParent());
            Files.writeString(p, body, StandardCharsets.UTF_8);
            LOG.fine(() -> "Saved file " + p);
            return true;
        } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to save file " + path, e);
            return false;
        }
    }

    private String buildGetFileResponse(Path path) {
        try {
            String content = Files.readString(path);
            return buildResponse(content, OCTET_STREAM_CONTENT_TYPE, OK_RESPONSE);
        } catch (IOException e) {
            LOG.fine(() -> "File not found: " + path);
            return buildEmptyBody(NOT_FOUND);
        }
    }

    private String buildResponse(String body, String contentType, String status) {
        return HTTP_TYPE + " " + status +
                "\r\nContent-Type: " + contentType +
                "\r\nContent-Length: " + body.length() +
                "\r\n\r\n" + body;
    }

    private String buildEmptyBody(String status) {
        return HTTP_TYPE + " " + status + "\r\nContent-Length: 0\r\n\r\n";
    }

    private void write(String response) throws IOException {
        socket.getOutputStream().write(response.getBytes());
    }

    private String[] getUrlParts(String requestLine) {
        return requestLine.split(" ")[1].split("/");
    }

    private String getMethod(String requestLine) {
        return requestLine.split(" ")[0];
    }

    private boolean isValidPath(String[] parts) {
        if (parts.length < 2) return true;
        String ep = parts[1];
        if (!ENDPOINTS.contains(ep)) return false;
        if (("files".equals(ep) || "echo".equals(ep)) && parts.length != 3) {
            LOG.fine("Missing filename or echo text");
            return false;
        }
        return true;
    }
}