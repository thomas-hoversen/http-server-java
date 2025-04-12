# Java HTTP Server

A minimal multithreaded HTTP/1.1 server written entirely from scratch in Java.  
Implemented as part of a **Codecrafters** challenge, meaning every feature was built by hand with only milestone hints for structure.

The server supports:

* `GET /` – root (200 OK, empty body)
* `GET /echo/<msg>` – echoes `<msg>`
* `GET /user-agent` – echoes the request’s **User‑Agent** header
* `GET /files/<name>` – returns a file from the supplied directory
* `POST /files/<name>` – saves the request body to `<name>` and replies **201 Created**

---

## Running locally

### Prerequisites

* **JDK 11+**
* **Maven** (`mvn -v` should work)
* **bash / sh**

### Start the server

```bash
chmod +x your_program.sh          # one‑time
./your_program.sh --directory /tmp/data/codecrafters.io/http-server-tester/
```

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

# Concurrent‑connections stage (four parallel GET / requests)
curl -v http://localhost:4221/          # client‑2, first pair
curl -v http://localhost:4221/          # client‑1, first pair
curl -v http://localhost:4221/          # client‑1, second pair
curl -v http://localhost:4221/          # client‑2, second pair

# Echo endpoints
curl -v http://localhost:4221/user-agent \
     -H "User-Agent: orange/raspberry-apple"

curl -v http://localhost:4221/echo/pear

# Unknown path → 404 Not Found
curl -v http://localhost:4221/raspberry

# Root path (blank body, 200)
curl -v http://localhost:4221/
```

## Resources consulted to implement the code
## Resources consulted

* **RFC 7230 – HTTP/1.1 Message Syntax and Routing**  
  <https://datatracker.ietf.org/doc/html/rfc7230#page-5>

* **Java concurrency & thread pools**  
  <https://medium.com/@ShantKhayalian/advanced-java-concurrency-patterns-and-best-practices-6cc071b5d96c>

* **`java.util.Optional` guide**  
  <https://www.baeldung.com/java-optional>

* **Working with `java.nio.file.Files`**  
  <https://www.marcobehler.com/guides/java-files>

* **Codecrafters CLI** – run automated tests locally with `codecrafters test` (this won't work outside Thomas's local machine).