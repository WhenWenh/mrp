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
    private HttpResponses resp;

    public FavoriteHandler(ObjectMapper mapper, FavoriteService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
        this.resp = new HttpResponses(mapper);
    }

    // POST /media/{mediaId}/favorite
    public void add(HttpExchange ex, UUID mediaId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try {
            service.add(userId, mediaId);
            resp.empty(ex, 204);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();

            if ("media not found".equalsIgnoreCase(msg)) {
                resp.error(ex, 404, msg);
            } else {
                resp.error(ex, 400, msg.isBlank() ? "bad request" : msg);
            }
        }
    }

    // DELETE /media/{mediaId}/favorite
    public void remove(HttpExchange ex, UUID mediaId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try {
            service.remove(userId, mediaId);
            resp.empty(ex, 204);
        }catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if ("media not found".equalsIgnoreCase(msg)) {
                resp.error(ex, 404, msg);
            }else {
                resp.error(ex, 400, msg.isBlank() ? "bad request" : msg);
            }
        }
    }

    // GET /users/me/favorites
    public void listMine(HttpExchange ex) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try {
            resp.json(ex, 200, service.listMine(userId));
        } catch (IllegalArgumentException e) {
            resp.error(ex, 400, e.getMessage() == null || e.getMessage().isBlank() ? "bad request" : e.getMessage());
        }
    }

    // SPEC: GET /users/{userId}/favorites
    public void listForUser(HttpExchange ex, UUID userId) throws IOException {
        UUID authUserId;
        try {
            authUserId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        if (!authUserId.equals(userId)) {
            resp.error(ex, 403, "forbidden");
            return;
        }

        try {
            resp.json(ex, 200, service.listMine(userId));
        } catch (IllegalArgumentException e) {
            resp.error(ex, 400, e.getMessage() == null || e.getMessage().isBlank() ? "bad request" : e.getMessage());
        }
    }


}
