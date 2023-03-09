package ru.netology;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Request {

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

    public String getHttpVersion() {
        return httpVersion;
    }

    public Request(String message, String delimiter, String lineDelimiter, String headerDelimiter) {

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


    public String getPathFromUrl() {
        return url.split("\\?")[0];
    }

    public NameValuePair getQueryParam(String name) throws URISyntaxException {
        return getQueryParams().stream().filter(paar -> paar.getName().equalsIgnoreCase(name)).findFirst().orElse(new NameValuePair() {
            @Override
            public String getName() {
                return null;
            }

            @Override
            public String getValue() {
                return null;
            }
        });
    }

    public List<NameValuePair> getQueryParams() throws URISyntaxException {
        return URLEncodedUtils.parse(new URI(url), StandardCharsets.UTF_8);
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
