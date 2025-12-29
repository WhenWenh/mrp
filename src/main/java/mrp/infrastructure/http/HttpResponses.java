package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.dto.ApiErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class HttpResponses {

    private ObjectMapper mapper;

    public HttpResponses(ObjectMapper mapper) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        this.mapper = mapper;
    }

    public void json(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    public void error(HttpExchange ex, int status, String message) throws IOException {
        String msg = message == null ? "" : message;
        json(ex, status, new ApiErrorResponse(msg));
    }

    public void message(HttpExchange ex, int status, String message) throws IOException {
        // falls für "success: ..." wirklich nur eine Message ist
        json(ex, status, new ApiErrorResponse(message)); // oder ApiMessageResponse
    }

    public void empty(HttpExchange ex, int status) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }
}
