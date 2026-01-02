package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.RecommendationService;
import mrp.infrastructure.security.AuthService;

import java.io.IOException;
import java.util.UUID;

public class RecommendationHandler {

    private ObjectMapper mapper;
    private RecommendationService service;
    private AuthService auth;
    private HttpResponses resp;

    public RecommendationHandler(ObjectMapper mapper, RecommendationService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
        this.resp = new HttpResponses(mapper);
    }

    // SPEC: GET /users/{userId}/recommendations?limit=10
    // Security: only allow the authenticated user to access their own recommendations
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

        // Delegate to existing implementation
        listMine(ex);
    }

    // GET /users/me/recommendations?limit=10
    public void listMine(HttpExchange ex) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        int limit = 10;
        String q = ex.getRequestURI().getQuery();
        if (q != null) {
            for (String part : q.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && "limit".equalsIgnoreCase(kv[0])) {
                    try {
                        limit = Integer.parseInt(kv[1]);
                    } catch (NumberFormatException e) {
                        resp.error(ex, 400, "limit must be an integer");
                        return;
                    }
                }
            }
        }

        try {
            resp.json(ex, 200, service.recommendForUser(userId, limit));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            resp.error(
                    ex,
                    400,
                    (msg == null || msg.isBlank()) ? "bad request" : msg
            );
        }
    }
}
