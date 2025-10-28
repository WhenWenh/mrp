package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import mrp.application.MediaService;
import mrp.dto.MediaRequest;
import mrp.dto.MediaResponse;
import mrp.domain.ports.MediaSearch;
import mrp.infrastructure.security.AuthService;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.*;
import java.util.UUID;

public class MediaHandler {

    private ObjectMapper mapper;
    private MediaService service;
    private AuthService auth;

    public MediaHandler(ObjectMapper mapper, MediaService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
    }

    // POST /media
    public void create(HttpExchange ex) throws IOException {
        var userId = auth.requireUserId(ex); // <— kurz & klar
        try (InputStream in = ex.getRequestBody()) {
            MediaRequest req = mapper.readValue(in, MediaRequest.class);
            MediaResponse resp = service.create(userId, req);
            sendJson(ex, 201, mapper.writeValueAsBytes(resp));
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());
        } catch (SecurityException se) {
            sendError(ex, 403, "forbidden");
        }
    }


    // GET /media/{id}
    public void getOne(HttpExchange ex, UUID id) throws IOException {
        auth.requireAuth(ex);
        try {
            MediaResponse resp = service.get(id);
            sendJson(ex, 200, mapper.writeValueAsBytes(resp));
        } catch (IllegalArgumentException e) {
            sendError(ex, 404, "not found");
        }
    }

    // PUT /media/{id}
    public void update(HttpExchange ex, UUID id) throws IOException {
        var userId = auth.requireUserId(ex);
        try (InputStream in = ex.getRequestBody()) {
            MediaRequest req = mapper.readValue(in, MediaRequest.class);
            MediaResponse resp = service.update(id, userId, req);
            sendJson(ex, 200, mapper.writeValueAsBytes(resp));
        } catch (IllegalArgumentException e) {
            // not found / validation
            String msg = e.getMessage();
            if ("media not found".equalsIgnoreCase(msg)) sendError(ex, 404, "not found");
            else sendError(ex, 400, msg);
        } catch (SecurityException se) {
            sendError(ex, 403, "forbidden");
        }
    }

    // DELETE /media/{id}
    public void delete(HttpExchange ex, UUID id) throws IOException {
        var userId = auth.requireUserId(ex);
        try {
            service.delete(id, userId);
            sendEmpty(ex, 204);
        } catch (IllegalArgumentException e) {
            sendError(ex, 404, "not found");
        } catch (SecurityException se) {
            sendError(ex, 403, "forbidden");
        }
    }

    // GET /media?q=&type=&yearFrom=&yearTo=&ageMax=&sortBy=&sortDir=&limit=&offset=
    public void list(HttpExchange ex) throws IOException {
        auth.requireAuth(ex);
        Map<String, String> q = parseQuery(ex.getRequestURI());

        String query = q.get("q");
        String type = q.get("type"); // erwartet "MOVIE"/"SERIES"/"GAME" als TEXT

        Integer yearFrom = toInt(q.get("yearFrom"));
        Integer yearTo   = toInt(q.get("yearTo"));
        Integer ageMax   = toInt(q.get("ageMax"));

        String sortBy = emptyToNull(q.get("sortBy"));   // "title" | "year" | "created"
        String sortDir = emptyToNull(q.get("sortDir")); // "asc" | "desc"

        int limit  = orDefault(toInt(q.get("limit")), 20);
        int offset = orDefault(toInt(q.get("offset")), 0);

        MediaSearch search = new MediaSearch(
                query, type, yearFrom, yearTo, ageMax,
                sortBy == null ? "created" : sortBy,
                sortDir == null ? "desc" : sortDir,
                limit, offset
        );

        var list = service.search(search);
        sendJson(ex, 200, mapper.writeValueAsBytes(list));
    }

    // ---- helpers ----
    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        String q = uri.getQuery();
        if (q == null || q.isBlank()) return map;
        String[] parts = q.split("&");
        for (String p : parts) {
            int i = p.indexOf('=');
            if (i > 0) {
                String k = urlDecode(p.substring(0, i));
                String v = urlDecode(p.substring(i + 1));
                map.put(k, v);
            }
        }
        return map;
    }

    private String urlDecode(String s) {
        try { return java.net.URLDecoder.decode(s, java.nio.charset.StandardCharsets.UTF_8); }
        catch (Exception e) { return s; }
    }

    private Integer toInt(String s) {
        try { return s == null ? null : Integer.parseInt(s); }
        catch (Exception e) { return null; }
    }

    private int orDefault(Integer v, int def) { return v == null ? def : v; }

    private String emptyToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private void sendJson(HttpExchange ex, int code, byte[] json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(code, json.length);
        ex.getResponseBody().write(json);
        ex.close();
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] body = ("{\"error\":\"" + msg + "\"}").getBytes(java.nio.charset.StandardCharsets.UTF_8);
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
