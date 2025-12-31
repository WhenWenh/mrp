package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.MediaService;
import mrp.dto.MediaRequest;
import mrp.infrastructure.security.AuthService;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.core.JsonProcessingException;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class MediaHandler {

    private ObjectMapper mapper;
    private MediaService service;
    private AuthService auth;
    private HttpResponses resp;

    public MediaHandler(ObjectMapper mapper, MediaService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        if (service == null) throw new IllegalArgumentException("service null");
        if (auth == null) throw new IllegalArgumentException("auth null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
        this.resp = new HttpResponses(mapper);
    }

    public void create(HttpExchange ex) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("application/json")) {
            resp.error(ex, 405, "unsupported media type");
            return;
        }

        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }


        try (InputStream in = ex.getRequestBody()) {
            MediaRequest req = mapper.readValue(in, MediaRequest.class);
            Object created = service.create(userId, req);
            resp.json(ex, 201, created);
        } catch (InvalidFormatException e) {
            //falscher type
            resp.error(ex, 400, "invalid value for field: " + e.getPathReference());
        } catch (JsonProcessingException e) {
            // generell kaputtes JSON
            resp.error(ex, 400, "invalid json");
        } catch (IllegalArgumentException e) {
            resp.error(ex, 400, e.getMessage());
        } catch (SecurityException se) {
            resp.error(ex, 403, "forbidden");
        }
    }

    public void getOne(HttpExchange ex, UUID id) throws IOException {
        try {
            auth.requireAuth(ex); // kann "token expired" werfen
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage()); // {"error":"token expired"}
            return;
        }

        try {
            Object one = service.get(id);
            resp.json(ex, 200, one);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 404, "not found");
        }
    }

    public void update(HttpExchange ex, UUID id) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("application/json")) {
            resp.error(ex, 415, "unsupported media type");
            return;
        }

        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }


        try (InputStream in = ex.getRequestBody()) {
            MediaRequest req = mapper.readValue(in, MediaRequest.class);
            Object updated = service.update(id, userId, req);
            resp.json(ex, 200, updated);
        }catch (InvalidFormatException e) {
            // z.B. falsches Enum / falscher Typ im JSON
            resp.error(ex, 400, "invalid value");

        } catch (JsonProcessingException e) {
            // kaputtes JSON
            resp.error(ex, 400, "invalid json");

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if ("media not found".equalsIgnoreCase(msg)) {
                resp.error(ex, 404, "media not found");
            } else {
                resp.error(ex, 400, msg);
            }
        } catch (SecurityException se) {
            resp.error(ex, 403, "forbidden");
        }
    }

    public void delete(HttpExchange ex, UUID id) throws IOException {
        try {
            UUID userId;
            try {
                userId = auth.requireUserId(ex);
            } catch (IllegalArgumentException e) {
                resp.error(ex, 401, e.getMessage());
                return;
            }

            service.delete(id, userId);
            resp.empty(ex, 204);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 404, "not found");
        } catch (SecurityException se) {
            resp.error(ex, 403, "forbidden");
        }
    }

    public void list(HttpExchange ex) throws IOException {
        try {
            auth.requireAuth(ex); // kann "token expired" werfen
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage()); // {"error":"token expired"}
            return;
        }

        var q = Query.from(ex.getRequestURI());
        var search = new mrp.domain.ports.MediaSearch(
                q.s("q"), q.s("type"), q.s("genre"),q.i("yearFrom"), q.i("yearTo"), q.i("ageMax"),
                q.sOr("sortBy","created"), q.sOr("sortDir","desc"),
                q.iOr("limit",20), q.iOr("offset",0)
        );
        Object list = service.search(search);
        resp.json(ex, 200, list);
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
