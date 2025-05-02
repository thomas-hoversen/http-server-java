# Java HTTP Server

A lean, from-scratch HTTP/1.1 server written in Java as part of a **Codecrafters** challenge, 
meaning every feature was built by hand with only milestone hints for structure.

At work I would build a Java web service using Spring Boot and WebFlux. These abstract away HTTP 
protocols, virtual thread management and scaling, etc, out of the box. This simple project is
an educational experience to implement thin slices of the protocol by hand. It isn't perfect and
would need structural improvements to memory management and code structure before it would be 
'production-ready'. The important concepts I took away from this project was managing virtual threads,
using http headers like accept-encoding, the general HTTP protocol and request/response structure, 
and using the ServerSocket and Socket classes to expose a port and accept incoming connection requests.

Some future improvements would include more robust handling of request sizes and request parsing;
separating concerns from the Server file via more Java objects; and introducing a handler interface
per endpoint to mirror real web-framework patterns.

**AI USE IN THIS PROJECT**

During the project, I tried to avoid using AI to solve each milestone step outlined in the
CodeCrafters challenge because I felt using AI would detract from the educational value. Once
the project was complete and passing all of the CodeCrafter tests, I used AI to evaluate the
project and identify areas of improvement. For example, the AI helped me refactor the
One-thread-per-socket model I was using to replace it with Virtual threads (Project Loom) and a
Semaphore gate for better scalability (not that this project needs it).


Key features:

* **Virtual threads** (Project Loom) for inexpensive concurrency
* **Semaphore gate** (100 k permits) to cap simultaneous connections
* **Keep-alive** by default; 10 s application idle + read timeouts
* Optional **gzip** response if the client sends `Accept-Encoding: gzip`
* Tiny routing layer:
  * `GET /` – empty 200
  * `GET /echo/<msg>` – returns `<msg>`
  * `GET /user-agent` – echoes the `User-Agent` header
  * `GET /files/<name>` / `POST /files/<name>` – simple file download / upload

---

## Running locally

### Prerequisites

* **JDK11+**
* **Maven** (`mvn -v` should work)
* **bash / sh**

### Start the server

```bash
chmod +x http_server_start.sh          # one‑time
./http_server_start.sh --directory /tmp/data/codecrafters.io/http-server-tester/
```

---

### Sample curl commands

```bash
# Upload a file (POST)
curl -v -X POST http://localhost:4221/files/raspberry_raspberry_banana_raspberry \
     -H "Content-Length: 52" \
     -H "Content-Type: application/octet-stream" \
     -d 'banana blueberry grape banana grape mango pear apple'

# Download the same file (GET)
curl -v http://localhost:4221/files/raspberry_raspberry_banana_raspberry

# Missing file → 404
curl -v http://localhost:4221/files/non-existentapple_blueberry_raspberry_grape

# Persistent connection test (keep-alive)
curl -v http://localhost:4221/ \
     -H "Connection: keep-alive"

# Test gzip compression
curl --compressed -v  http://localhost:4221/echo/pear \
     -H "Accept-Encoding: gzip"

# Echo endpoints
curl -v http://localhost:4221/user-agent \
     -H "User-Agent: orange/raspberry-apple"

curl -v http://localhost:4221/echo/pear

# Unknown path → 404 Not Found
curl -v http://localhost:4221/raspberry

# Root path (blank body, 200)
curl -v http://localhost:4221/
```

---

## Resources consulted

* **RFC 7230 – HTTP/1.1 Message Syntax and Routing**  
  <https://datatracker.ietf.org/doc/html/rfc7230#page-5>

* **HTTP keep-alive in Java sockets**  
  <https://docs.oracle.com/javase/7/docs/technotes/guides/net/http-keepalive.html>

* **Java concurrency & thread pools**  
  <https://medium.com/@ShantKhayalian/advanced-java-concurrency-patterns-and-best-practices-6cc071b5d96c>

* **`java.util.Optional` guide**  
  <https://www.baeldung.com/java-optional>

* **Working with `java.nio.file.Files`**  
  <https://www.marcobehler.com/guides/java-files>

* **To check that the compressed GZIP body is correct, run `echo -n <uncompressed-str> | gzip | hexdump -C`**

* **Codecrafters CLI** – run automated tests locally with `codecrafters test` (this won't work outside Thomas's local machine).