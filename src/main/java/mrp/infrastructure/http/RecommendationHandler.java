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

    public RecommendationHandler(ObjectMapper mapper, RecommendationService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
    }

    // GET /users/me/recommendations?limit=10
    public void listMine(HttpExchange ex) throws IOException {
        UUID userId = auth.requireUserId(ex);

        int limit = 10;
        String q = ex.getRequestURI().getQuery();
        if (q != null) {
            for (String part : q.split("&")) {
                String[] kv = part.split("=");
                if (kv.length == 2 && "limit".equalsIgnoreCase(kv[0])) {
                    try { limit = Integer.parseInt(kv[1]); } catch (NumberFormatException ignored) { }
                }
            }
        }

        byte[] json = mapper.writeValueAsBytes(service.recommendForUser(userId, limit));
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, json.length);
        ex.getResponseBody().write(json);
        ex.close();
    }
}
