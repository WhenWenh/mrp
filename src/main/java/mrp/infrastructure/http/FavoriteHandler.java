package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.FavoriteService;
import mrp.infrastructure.security.AuthService;

import java.io.IOException;
import java.util.UUID;

public class FavoriteHandler {

    private ObjectMapper mapper;
    private FavoriteService service;
    private AuthService auth;

    public FavoriteHandler(ObjectMapper mapper, FavoriteService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
    }

    // POST /media/{mediaId}/favorite
    public void add(HttpExchange ex, UUID mediaId) throws IOException {
        try {
            UUID userId = auth.requireUserId(ex);
            service.add(userId, mediaId);
            sendEmpty(ex, 204);
        } catch (IllegalArgumentException e) {
            if ("media not found".equalsIgnoreCase(e.getMessage())) sendError(ex, 404, "not found");
            else sendError(ex, 400, e.getMessage() != null ? e.getMessage() : "bad request");
        }
    }

    // DELETE /media/{mediaId}/favorite
    public void remove(HttpExchange ex, UUID mediaId) throws IOException {
        UUID userId = auth.requireUserId(ex);
        service.remove(userId, mediaId);
        sendEmpty(ex, 204);
    }

    // GET /users/me/favorites
    public void listMine(HttpExchange ex) throws IOException {
        UUID userId = auth.requireUserId(ex);
        byte[] json = mapper.writeValueAsBytes(service.listMine(userId));
        sendJson(ex, 200, json);
    }

    private void sendJson(HttpExchange ex, int status, byte[] json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, json.length);
        ex.getResponseBody().write(json);
        ex.close();
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] body = ("{\"error\":\"" + (msg == null ? "" : msg.replace("\"", "\\\"")) + "\"}")
                .getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, body.length);
        ex.getResponseBody().write(body);
        ex.close();
    }

    private void sendEmpty(HttpExchange ex, int code) throws IOException {
        ex.sendResponseHeaders(code, -1);
        ex.close();
    }
}
