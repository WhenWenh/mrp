package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.LeaderboardService;
import mrp.domain.model.LeaderboardEntry;
import mrp.dto.LeaderboardEntryResponse;
import mrp.infrastructure.security.AuthService;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class LeaderboardHandler {

    private ObjectMapper mapper;
    private LeaderboardService service;
    private AuthService auth;

    public LeaderboardHandler(ObjectMapper mapper, LeaderboardService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
    }

    // GET /leaderboard?limit=10&offset=0
    public void list(HttpExchange ex) throws IOException {
        auth.requireAuth(ex);

        int limit = parseIntQuery(ex, "limit", 10);
        int offset = parseIntQuery(ex, "offset", 0);

        List<LeaderboardEntry> entries = service.getLeaderboard(limit, offset);
        List<LeaderboardEntryResponse> resp = new ArrayList<>();

        int rank = offset + 1;
        for (LeaderboardEntry e : entries) {
            resp.add(new LeaderboardEntryResponse(rank, e.getUserId(), e.getUsername(), e.getRatingCount()));
            rank++;
        }

        byte[] json = mapper.writeValueAsBytes(resp);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(200, json.length);
        ex.getResponseBody().write(json);
        ex.close();
    }

    private int parseIntQuery(HttpExchange ex, String key, int defaultValue) {
        try {
            String q = ex.getRequestURI().getQuery();
            if (q == null || q.isEmpty()) return defaultValue;
            for (String part : q.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length == 2 && key.equalsIgnoreCase(kv[0])) return Integer.parseInt(kv[1]);
            }
            return defaultValue;
        } catch (Exception e) {
            return defaultValue;
        }
    }
}
