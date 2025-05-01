import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.GZIPOutputStream;

public class Server implements Runnable {

  private static final Logger LOG = Logger.getLogger(Server.class.getName());

  private static final String HTTP_TYPE = "HTTP/1.1";
  private static final String PLAINTEXT_CONTENT_TYPE = "text/plain";
  private static final String OCTET_STREAM_CONTENT_TYPE = "application/octet-stream";
  private static final String GZIP_ENCODING = "gzip";

  private static final String OK_RESPONSE = "200 OK";
  private static final String CREATED_RESPONSE = "201 Created";
  private static final String NOT_FOUND = "404 Not Found";

  private static final List<String> ENDPOINTS =
      Arrays.asList("index.html", "echo", "user-agent", "files");

  private final Socket SOCKET;
  private final String FILES_DIR;

  public Server(Socket socket, String filesDir) {
    this.SOCKET = socket;
    this.FILES_DIR = filesDir;
  }

  @Override
  public void run() {
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(SOCKET.getInputStream()))) {

      while (true) {
        /* ---------- Request line ---------- */
      /*
      Typical request line:
        - GET /user-agent HTTP/1.1
       */
        String requestLine = reader.readLine();
        LOG.info(() -> "Request line: " + requestLine);

        if (requestLine == null || requestLine.isEmpty()) {
          LOG.info(() -> "Empty request line, closing connection.");
          SOCKET.close(); // needed?
          return;
        }

        /* ---------- Headers ---------- */
        Map<String, String> headers = readHeaders(reader);
        LOG.info(() -> "Headers: " + headers);

        // ex: [] or [files, raspberry_raspberry_banana_raspberry] or [user-agent] or [echo, pear]
        String[] urlParts = getUrlParts(requestLine);
        // ex: GET or POST
        String method = getMethod(requestLine);

        if (!isValidPath(urlParts)) {
          LOG.info(() -> "Invalid URL: " + Arrays.toString(urlParts));
          write(buildEmptyBody(NOT_FOUND));
          return;
        }

        /* ---------- Routing ---------- */
        if (urlParts.length == 0) {
          LOG.info(() -> "urlParts.length == 0");
          write(buildEmptyBody(OK_RESPONSE));
          //return;
        }

        String endpoint = urlParts[0];
        switch (endpoint) {
          case "echo" -> handleEcho(urlParts, headers);
          case "user-agent" -> handleUserAgent(headers);
          case "files" -> handleFiles(urlParts, method, reader, headers);
          default -> write(buildEmptyBody(OK_RESPONSE));
        }

        SOCKET.getOutputStream().flush();

        if (headers.containsKey("connection")) {
          if (headers.get("connection").equals("close")) {
            SOCKET.close();
          }
        }
      }
    } catch (Exception e) {
      LOG.severe(() -> "I/O error while handling client" + e);
    }
  }

  /* ---------- Handlers ---------- */

  private void handleEcho(String[] urlParts, Map<String, String> headers) throws Exception {
    //LOG.info(() -> "handleEcho urlParts: " + Arrays.toString(urlParts));
    if (urlParts.length < 2) {
      write(buildEmptyBody(NOT_FOUND));
      return;
    }
    String msg = urlParts[1];
    LOG.info(() -> "Echoing: " + msg);
    write(buildResponse(msg, PLAINTEXT_CONTENT_TYPE, OK_RESPONSE, headers));
  }

  private void handleUserAgent(Map<String, String> headers) throws Exception {
    String ua = headers.getOrDefault("user-agent", "");
    LOG.info(() -> "User-Agent echoed: " + ua);
    write(buildResponse(ua, PLAINTEXT_CONTENT_TYPE, OK_RESPONSE, headers));
  }

  private void handleFiles(String[] urlParts,
      String method,
      BufferedReader reader,
      Map<String, String> headers) throws IOException {

    if (urlParts.length < 2) { // no filename, expecting something like [files, filename]
      write(buildEmptyBody(NOT_FOUND));
      return;
    }
    String filename = urlParts[1];
    String path = FILES_DIR + filename;
    LOG.info(() -> method + " /files/" + filename);

    if ("GET".equals(method)) {
      //LOG.info(() -> "GET");
      write(buildGetFileResponse(Path.of(path), headers));
    } else { // POST
      String body = readBody(reader, headers);
      //LOG.info(() -> "the body: " + body);
      boolean ok = saveFile(path, body);
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
            // remove whitespace
            line.substring(colon + 1).replace(" ", ""));
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
        if (r == -1) {
            throw new IOException("Client closed connection prematurely");
        }
      read += r;
    }
    return new String(buf);
  }

  private boolean saveFile(String path, String body) {
    try {
      Path p = Paths.get(path);
      Files.createDirectories(p.getParent());
      Files.writeString(p, body, StandardCharsets.UTF_8);
      LOG.info(() -> "Saved file " + p);
      return true;
    } catch (IOException e) {
      LOG.log(Level.WARNING, "Failed to save file " + path, e);
      return false;
    }
  }

  private byte[] buildGetFileResponse(Path path, Map<String, String> headers) {
    try {
      String content = Files.readString(path);
      return buildResponse(content, OCTET_STREAM_CONTENT_TYPE, OK_RESPONSE, headers);
    } catch (IOException e) {
      LOG.info(() -> "File not found: " + path);
      return buildEmptyBody(NOT_FOUND);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private byte[] buildResponse(String body, String contentType, String status, Map<String, String> headers)
      throws Exception {

    byte[] newBody = null;

    StringBuilder response = new StringBuilder();
    response.append(HTTP_TYPE).append(" ").append(status);
    response.append("\r\nContent-Type: ").append(contentType);

    if (headers.containsKey("accept-encoding")) {

      // ex: Headers: {host=localhost:4221, accept-encoding=encoding-1, encoding-2, gzip}
      List<String> encodings = new ArrayList<>(List.of(headers.get("accept-encoding").split(",")));
      if (encodings.contains(GZIP_ENCODING)) { // the only encoding type accepted
        // encode body
        newBody = compress(body);
        LOG.info(() -> "Response encoded to gzip.");
        // add header to response
        response.append("\r\nContent-Encoding: ").append(GZIP_ENCODING);
      }
    }

    if (newBody != null) {
      response.append("\r\nContent-Length: ").append(newBody.length);
      response.append("\r\n\r\n");
      byte[] currBody = response.toString().getBytes();

      ByteArrayOutputStream out = new ByteArrayOutputStream(currBody.length + newBody.length);
      out.write(currBody);
      out.write(newBody);
      return out.toByteArray();
    } else {
      response.append("\r\nContent-Length: ").append(body.length());
      response.append("\r\n\r\n").append(body);
      return response.toString().getBytes();
    }
  }

  private byte[] compress(String str) throws Exception {
    if (str == null || str.isEmpty()) {
      return null;
    }
    ByteArrayOutputStream obj=new ByteArrayOutputStream();
    GZIPOutputStream gzip = new GZIPOutputStream(obj);
    gzip.write(str.getBytes("UTF-8"));
    gzip.close();
    return obj.toByteArray();
  }

  private byte[] buildEmptyBody(String status) {
    return (HTTP_TYPE + " " + status + "\r\nContent-Length: 0\r\n\r\n").getBytes();
  }

  private void write(byte[] response) throws IOException {
    SOCKET.getOutputStream().write(response);
  }

  private String[] getUrlParts(String requestLine) {
    //LOG.info(() -> "getUrlParts: " + requestLine);
    // GET /echo/pear HTTP/1.1 --> [,"echo","pear"] --> ["echo","pear"]
    return Arrays.stream(requestLine.split(" ")[1].split("/"))
        .filter(s -> !s.isEmpty())
        .toArray(String[]::new);
  }

  private String getMethod(String requestLine) {
    //LOG.info(() -> "getMethod: " + requestLine);
    return requestLine.split(" ")[0];
  }

  private boolean isValidPath(String[] parts) {
    //LOG.info(() -> "isvalidpath: " + Arrays.toString(parts));
    // GET /echo/pear HTTP/1.1 --> parts = [echo, pear]
    // GET / HTTP/1.1 --> parts = []
    if (parts.length == 0) {
      //LOG.info(() -> "isValidPath length == 0");
      return true;
    }

    String ep = parts[0];
      if (!ENDPOINTS.contains(ep)) {
          return false;
      }
    if (("files".equals(ep) || "echo".equals(ep)) && parts.length != 2) {
      LOG.info("Missing filename for files endpoint or echo text for echo endpoint");
      return false;
    }
    //LOG.info(() -> "valid path");
    return true;
  }
}