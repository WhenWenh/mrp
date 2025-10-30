package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.MediaService;
import mrp.dto.MediaRequest;
import mrp.infrastructure.security.AuthService;

import java.io.IOException;
import java.io.InputStream;
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

    public void create(HttpExchange ex) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("application/json")) { sendError(ex, 415, "unsupported media type"); return; }
        try (InputStream in = ex.getRequestBody()) {
            UUID userId = auth.requireUserId(ex);
            MediaRequest req = mapper.readValue(in, MediaRequest.class);
            var resp = service.create(userId, req);
            sendJson(ex, 201, mapper.writeValueAsBytes(resp));
        } catch (IllegalArgumentException e) {
            sendError(ex, 400, e.getMessage());
        } catch (SecurityException se) {
            sendError(ex, 403, "forbidden");
        }
    }

    public void getOne(HttpExchange ex, UUID id) throws IOException {
        auth.requireAuth(ex);
        try {
            var resp = service.get(id);
            sendJson(ex, 200, mapper.writeValueAsBytes(resp));
        } catch (IllegalArgumentException e) {
            sendError(ex, 404, "not found");
        }
    }

    public void update(HttpExchange ex, UUID id) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("application/json")) { sendError(ex, 415, "unsupported media type"); return; }
        try (InputStream in = ex.getRequestBody()) {
            UUID userId = auth.requireUserId(ex);
            MediaRequest req = mapper.readValue(in, MediaRequest.class);
            var resp = service.update(id, userId, req);
            sendJson(ex, 200, mapper.writeValueAsBytes(resp));
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if ("media not found".equalsIgnoreCase(msg)) sendError(ex, 404, "not found");
            else sendError(ex, 400, msg);
        } catch (SecurityException se) {
            sendError(ex, 403, "forbidden");
        }
    }

    public void delete(HttpExchange ex, UUID id) throws IOException {
        try {
            UUID userId = auth.requireUserId(ex);
            service.delete(id, userId);
            sendEmpty(ex, 204);
        } catch (IllegalArgumentException e) {
            sendError(ex, 404, "not found");
        } catch (SecurityException se) {
            sendError(ex, 403, "forbidden");
        }
    }

    public void list(HttpExchange ex) throws IOException {
        auth.requireAuth(ex);
        var q = Query.from(ex.getRequestURI());
        var search = new mrp.domain.ports.MediaSearch(
                q.s("q"), q.s("type"), q.i("yearFrom"), q.i("yearTo"), q.i("ageMax"),
                q.sOr("sortBy","created"), q.sOr("sortDir","desc"),
                q.iOr("limit",20), q.iOr("offset",0)
        );
        var list = service.search(search);
        sendJson(ex, 200, mapper.writeValueAsBytes(list));
    }

    // ---- helpers ----
    private void sendJson(HttpExchange ex, int status, byte[] json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, json.length);
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

    // Mini Query-Helper (optional)
    private static class Query {
        java.util.Map<String,String> m;
        static Query from(java.net.URI u) {
            Query q = new Query(); q.m = new java.util.HashMap<>();
            String s = u.getQuery(); if (s == null) return q;
            for (String p : s.split("&")) {
                int i = p.indexOf('='); String k = i>0 ? p.substring(0,i) : p; String v = i>0 ? p.substring(i+1) : "";
                q.m.put(java.net.URLDecoder.decode(k, java.nio.charset.StandardCharsets.UTF_8),
                        java.net.URLDecoder.decode(v, java.nio.charset.StandardCharsets.UTF_8));
            } return q;
        }
        String s(String k){ return m.get(k); }
        String sOr(String k,String d){ String v=m.get(k); return v==null||v.isBlank()?d:v; }
        Integer i(String k){ try { return m.get(k)==null?null:Integer.parseInt(m.get(k)); } catch(Exception e){ return null; } }
        Integer iOr(String k,int d){ Integer v=i(k); return v==null?d:v; }
    }
}
