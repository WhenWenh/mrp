package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.dto.ApiErrorResponse;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility class for sending HTTP responses.
 * Centralizes JSON serialization and HTTP status handling.
 *
 * This avoids duplicated response logic across handlers and
 * ensures consistent Content-Type and error formats.
 */

public class HttpResponses {

    private ObjectMapper mapper;

    public HttpResponses(ObjectMapper mapper) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        this.mapper = mapper;
    }

    /**
     * Sends a JSON response with the given HTTP status code.
     *
     * @param ex HttpExchange
     * @param status HTTP status code (e.g. 200, 201, 400)
     * @param body object to serialize as JSON
     */
    public void json(HttpExchange ex, int status, Object body) throws IOException {
        byte[] bytes = mapper.writeValueAsBytes(body);
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, bytes.length);
        ex.getResponseBody().write(bytes);
        ex.close();
    }

    /**
     * Sends a standardized JSON error response.
     *
     * The error body is wrapped in ApiErrorResponse to keep
     * error responses consistent across the API.
     */
    public void error(HttpExchange ex, int status, String message) throws IOException {
        String msg = message == null ? "" : message;
        json(ex, status, new ApiErrorResponse(msg));
    }

    public void message(HttpExchange ex, int status, String message) throws IOException {
        // falls für "success: ..." wirklich nur eine Message ist
        json(ex, status, new ApiErrorResponse(message)); // oder ApiMessageResponse
    }

    /**
     * Sends an empty response body.
     *
     * Commonly used for:
     * - 204 No Content
     * - successful DELETE requests
     */
    public void empty(HttpExchange ex, int status) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        ex.sendResponseHeaders(status, -1);
        ex.close();
    }
}
