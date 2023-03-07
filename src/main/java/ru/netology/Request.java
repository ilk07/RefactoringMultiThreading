package ru.netology;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class Request {
    private final String message;

    private final String httpVersion;
    private final String url;
    private final HttpMethod method;
    private final String body;
    protected final Map<String, String> headers;

    public String getUrl() {
        return url;
    }

    public HttpMethod getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public Request(String message, String delimiter, String lineDelimiter, String headerDelimiter) {

        this.message = message;

        String[] parts = message.split(delimiter);
        String head = parts[0];
        String[] headers = head.split(lineDelimiter);
        String[] startLine = headers[0].split(" ");

        this.method = HttpMethod.valueOf(startLine[0]);
        this.url = startLine[1];
        this.httpVersion = startLine[2];

        this.headers = Collections.unmodifiableMap(
                new HashMap<>() {{
                    for (int i = 1; i < headers.length; i++) {
                        String[] headerContent = headers[i].split(headerDelimiter, 2);
                        put(headerContent[0].trim(), headerContent[1].trim());
                    }
                }}
        );

        if (parts.length > 1 && parts[1].length() > 0) {
            this.body = parts[1].trim();
        } else {
            this.body = "";
        }
    }

    @Override
    public String toString() {
        return "Request{" +
                ", url='" + url + '\'' +
                ", method=" + method +
                ", body='" + body + '\'' +
                ", headers=" + headers.toString() +
                '}';
    }

}
