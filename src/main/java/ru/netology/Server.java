package ru.netology;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    protected final int port = 9999;
    protected final String folderName = "public";

    void start() {
        List<String> validPaths = getValidPathsArrayList();
        ExecutorService executorService = Executors.newFixedThreadPool(64);
        try (final var serverSocket = new ServerSocket(port)) {
            while (true) {
                var socket = serverSocket.accept();
                Thread responseThread = new Thread(() -> processRequest(socket, validPaths));
                executorService.execute(responseThread);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public List<String> getValidPathsArrayList() {
        List<String> validPaths = new ArrayList<>();
        File dir = new File("./" + folderName + "/"); //папка проекта
        if (dir.isDirectory() && dir.canRead()) {
            for (File item : Objects.requireNonNull(dir.listFiles())) {
                if (!item.isDirectory()) {
                    validPaths.add("/" + item.getName());
                }
            }
        }
        return validPaths;
    }

    void processRequest(Socket socket, List<String> validPaths) {
        try (
                final var in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                final var out = new BufferedOutputStream(socket.getOutputStream())
        ) {

            // read only request line for simplicity
            // must be in form GET /path HTTP/1.1
            if (in.ready()) {

                final var requestLine = in.readLine();
                final var parts = requestLine.split(" ");

                if (parts.length != 3) {
                    // just close socket
                    socket.close();
                }

                final var path = parts[1];
                if (!validPaths.contains(path)) {

                    String statusText = "Not Found";
                    out.write((
                            "HTTP/1.1 404 Not Found\r\n" +
                                    "Content-Type: " + "text/plain" + "\r\n" +
                                    "Content-Length: " + statusText.length() + "\r\n" +
                                    "Connection: close\r\n" +
                                    "\r\n"

                    ).getBytes());
                    out.flush();
                    socket.close();
                } else {

                    final var filePath = Path.of(".", "public", path);
                    final var mimeType = Files.probeContentType(filePath);


                    // special case for classic
                    if (path.equals("/classic.html")) {
                        final var template = Files.readString(filePath);
                        final var content = template.replace(
                                "{time}",
                                LocalDateTime.now().toString()
                        ).getBytes();
                        out.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + mimeType + "\r\n" +
                                        "Content-Length: " + content.length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        out.write(content);
                        out.flush();
                        socket.close();
                    } else {
                        final var length = Files.size(filePath);
                        out.write((
                                "HTTP/1.1 200 OK\r\n" +
                                        "Content-Type: " + mimeType + "\r\n" +
                                        "Content-Length: " + length + "\r\n" +
                                        "Connection: close\r\n" +
                                        "\r\n"
                        ).getBytes());
                        Files.copy(filePath, out);
                        out.flush();
                    }
                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
