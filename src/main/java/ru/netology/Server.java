package ru.netology;
import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class Server {
    private final String DELIMITER = "\r\n\r\n";
    private final String LINE_DELIMITER = "\r\n";
    private final String HEADER_DELIMITER = ":";
    private final String SOURCE_FOLDER = "public";
    private final String HTTP_VERSION = "HTTP/1.1";
    private final Map<HttpMethod, HashMap<String, Handler>> handlers = new ConcurrentHashMap<>();
    private final Map<Integer, String> responseCodes = new ConcurrentHashMap<>();

    public Server() {
        initServerErrors();
    }

    void processRequest(Socket socket) {
        try (

                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {

            while (!in.ready()) ;

            final var startLine = in.readLine(); //read start line
            final var parts = startLine.split(" ");

            if (parts.length != 3) {
                // just close socket
                wrongRequestResponse(400, out);
                socket.close();
            }

            if (!parts[2].equals(HTTP_VERSION)) {
                wrongRequestResponse(505, out);
                socket.close();
            }

            StringBuilder sb = new StringBuilder();
            sb.append(startLine).append(LINE_DELIMITER);

            HttpMethod currentMethod = HttpMethod.valueOf(parts[0]);

            String headerLine;
            int contentLength = 0;
            while ((headerLine = in.readLine()) != null) {
                sb.append(headerLine).append(LINE_DELIMITER);

                if (headerLine.contains("Content-Length")) { //check Content-Length header
                    contentLength = getRequestMessageLength(headerLine);
                }

                if (headerLine.equals("")) {  //end of headers
                    break;
                }
            }

            if (currentMethod.equals(HttpMethod.GET) && contentLength > 0) {
                wrongRequestResponse(400, out);
                socket.close();
            }

            if (contentLength > 0) {
                StringBuilder sbBody = new StringBuilder();
                int c;
                while ((c = in.read()) != -1) {
                    sbBody.append((char) c);
                    if (sbBody.length() == contentLength) {
                        sb.append(LINE_DELIMITER).append(sbBody);
                        break;
                    }
                }
            }

            //метод не представлен
            if (!methodIsAvailable(currentMethod)) {

                final var filePath = Path.of(".", "public", "error501.html");
                final var notImplementedTemplate = Files.readString(filePath);
                final var notImplementedContent = notImplementedTemplate.replace(
                        "{method}",
                        currentMethod.toString()
                );

                final var length = notImplementedContent.length();
                String response = createResponseHeaders(501, "text/html", length);
                out.write(response.getBytes());
                out.write(notImplementedContent.getBytes());
                out.flush();
                socket.close();

            } else {

                //create Request object
                Request request = new Request(sb.toString(), DELIMITER, LINE_DELIMITER, HEADER_DELIMITER);

                if (handlerIsAvailable(request.getMethod(), request.getPathFromUrl())) {
                    handlers.get(request.getMethod()).get(request.getPathFromUrl()).handle(request, out);
                    socket.close();
                } else {
                    //not found!
                    final var filePath = Path.of(".", "public", "error404.html");
                    final var errorPageTemplate = Files.readString(filePath);
                    final var errorPageContent = errorPageTemplate.replace(
                            "{item}",
                            request.getUrl()
                    );
                    final var length = errorPageContent.length();

                    String response = createResponseHeaders(404, "text/html", length);
                    out.write(response.getBytes());
                    out.write(errorPageContent.getBytes());
                    out.flush();
                    socket.close();
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void addHandler(HttpMethod httpMethod, String path, Handler handler) {
        if (!handlers.containsKey(httpMethod)) {
            handlers.put(httpMethod, new HashMap<>());
        }
        handlers.get(httpMethod).put(path, handler);
    }

    void listen(int port) {
        ExecutorService executorService = Executors.newFixedThreadPool(64);
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                var socket = serverSocket.accept();
                Thread responseThread = new Thread(() -> processRequest(socket));
                executorService.execute(responseThread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String createResponseHeaders(int statusCode, String contentType, long length) {

        return HTTP_VERSION +
                " " +
                statusCode +
                " " +
                responseCodes.get(statusCode) +
                LINE_DELIMITER +
                "Content-Type: " +
                contentType +
                LINE_DELIMITER +
                "Content-Length: " +
                length +
                LINE_DELIMITER +
                "Connection: close" + LINE_DELIMITER +
                LINE_DELIMITER;
    }

    void wrongRequestResponse(int code, BufferedOutputStream out) throws IOException {
        out.write(createResponseHeaders(code, "text/plain", 0).getBytes());
        out.flush();
    }

    public boolean handlerIsAvailable(HttpMethod method, String url) {
        if (handlers.containsKey(method)) {
            return handlers.get(method).containsKey(url);
        }
        return false;
    }

    public boolean methodIsAvailable(HttpMethod method) {
        return handlers.containsKey(method);
    }

    public int getRequestMessageLength(String contentLengthHeader) {
        String[] header = contentLengthHeader.split(HEADER_DELIMITER, 2);
        if (header[0].trim().contains("Content-Length")) {
            if (Integer.parseInt(header[1].trim()) > 0) {
                return Integer.parseInt(header[1].trim());
            }
        }
        return 0;
    }

    public void fillHandlerList() {
        final var sourceFolder = Path.of(".", SOURCE_FOLDER);
        File dir = new File(String.valueOf(sourceFolder)); //папка проекта
        if (dir.isDirectory()) {
            addHandler(HttpMethod.POST, "/postform.html", (request, responseStream) -> {
                try {
                    final var filePath = Path.of(".", SOURCE_FOLDER, "postform.html");
                    final var mimeType = Files.probeContentType(filePath);
                    final var length = Files.size(filePath);

                    responseStream.write(
                            createResponseHeaders(200, mimeType, length).getBytes()
                    );
                    Files.copy(filePath, responseStream);
                    responseStream.flush();


                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });
            for (File item : Objects.requireNonNull(dir.listFiles())) {
                if (!item.isDirectory()) {


                    addHandler(HttpMethod.GET, "/" + item.getName(), (request, responseStream) -> {
                        try {
                            final var filePath = Path.of(".", SOURCE_FOLDER, item.getName());
                            final var mimeType = Files.probeContentType(filePath);
                            final var length = Files.size(filePath);

                            if (item.getName().equals("classic.html")) { //replace time
                                final var classicTemplate = Files.readString(filePath);
                                final var classicContent = classicTemplate.replace(
                                        "{time}",
                                        LocalDateTime.now().toString()
                                ).getBytes();

                                responseStream.write(
                                        createResponseHeaders(200, mimeType, classicContent.length).getBytes()
                                );
                                responseStream.write(classicContent);
                                responseStream.flush();
                            } else {
                                responseStream.write(
                                        createResponseHeaders(200, mimeType, length).getBytes()
                                );
                                Files.copy(filePath, responseStream);
                                responseStream.flush();
                            }

                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                }
            }
        }
    }

    private void initServerErrors() {

        responseCodes.put(100, "Continue");
        responseCodes.put(101, "Switching Protocols");
        responseCodes.put(102, "Processing");
        responseCodes.put(103, "Early Hints");
        responseCodes.put(200, "OK");
        responseCodes.put(201, "Created");
        responseCodes.put(202, "Accepted");
        responseCodes.put(203, "Non-Authoritative");
        responseCodes.put(204, "No Content");
        responseCodes.put(205, "Reset Content");
        responseCodes.put(206, "Partial Reset");
        responseCodes.put(207, "Multi-Status");
        responseCodes.put(226, "IM Used");
        responseCodes.put(300, "Multiple Choices");
        responseCodes.put(301, "Moved Permanently");
        responseCodes.put(302, "Found");
        responseCodes.put(303, "See Other");
        responseCodes.put(304, "Not Modified");
        responseCodes.put(305, "Use Proxy");
        responseCodes.put(306, "Reserved");
        responseCodes.put(307, "Temporary Redirect");
        responseCodes.put(308, "Permanent Redirect");
        responseCodes.put(400, "Bad Request");
        responseCodes.put(401, "Unauthorized");
        responseCodes.put(402, "Payment Required");
        responseCodes.put(403, "Forbidden");
        responseCodes.put(404, "Not Found");
        responseCodes.put(405, "Method Not Allowed");
        responseCodes.put(406, "Not Acceptable");
        responseCodes.put(407, "Proxy Authentication Required");
        responseCodes.put(408, "Request Timeout");
        responseCodes.put(409, "Conflict");
        responseCodes.put(410, "Gone");
        responseCodes.put(411, "Length Required");
        responseCodes.put(412, "Precondition Failed");
        responseCodes.put(413, "Request Entity Too Large");
        responseCodes.put(414, "Request-URL Too Long");
        responseCodes.put(415, "Unsupported Media Type");
        responseCodes.put(416, "Requested Range Not Satisfiable");
        responseCodes.put(417, "Expectation Failed");
        responseCodes.put(422, "Unprocessable Entity");
        responseCodes.put(423, "Locked");
        responseCodes.put(424, "Failed Dependency");
        responseCodes.put(426, "Upgrade Required");
        responseCodes.put(500, "Internal Server Error");
        responseCodes.put(501, "Not Implemented");
        responseCodes.put(502, "Bad Gateway");
        responseCodes.put(503, "Service Unavailable");
        responseCodes.put(504, "Gateway Timeout");
        responseCodes.put(505, "HTTP Version Not Supported");
        responseCodes.put(506, "Variant Also Negotiates");
        responseCodes.put(507, "Insufficient Storage");
        responseCodes.put(510, "Not Extended");
        responseCodes.put(511, "Network Authentication Required");
    }

}
