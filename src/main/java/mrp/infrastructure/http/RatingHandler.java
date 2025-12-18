package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.RatingService;
import mrp.dto.RatingRequest;
import mrp.infrastructure.security.AuthService;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class RatingHandler {
    private ObjectMapper mapper;
    private RatingService service;
    private AuthService auth;

    public RatingHandler(ObjectMapper mapper, RatingService service, AuthService auth) {
        if(mapper == null){
            throw new IllegalArgumentException("Mapper cannot be null");
        }
        if(service == null){
            throw new IllegalArgumentException("Service cannot be null");
        }
        if(auth == null){
            throw new IllegalArgumentException("Auth cannot be null");
        }
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
    }

    // POST /media/{mediaId}/ratings
    public void rateMedia(HttpExchange ex, UUID mediaId) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if(ct == null || !ct.toLowerCase().contains("application/json")){
            sendError(ex, 405, "unsupported Media Type");
            return;
        }

        try(InputStream in = ex.getRequestBody()){
            UUID userId = auth.requireUserId(ex);
            RatingRequest req = mapper.readValue(in, RatingRequest.class);
            var resp = service.create(userId, mediaId, req);
            sendJson(ex, 201, mapper.writeValueAsBytes(resp));
        } catch(IllegalArgumentException e){
            String msg = e.getMessage();
            if("media not found".equalsIgnoreCase(msg)){
                sendError(ex, 404, "not found");
            }else{
                sendError(ex, 400, msg != null ? msg : "bad request");
            }
        } catch(SecurityException e){
            sendError(ex, 403, "forbidden");
        }
    }

    // GET /media/{mediaId}/ratings
    public void listForMedia(HttpExchange ex, UUID mediaId) throws IOException {
        UUID requesterId =  auth.requireUserId(ex);
        try{
            var list = service.listForMedia(mediaId, requesterId);
            sendJson(ex, 200, mapper.writeValueAsBytes(list));
        } catch(IllegalArgumentException e){
                String msg =  e.getMessage();
                if("media not found".equalsIgnoreCase(msg)){
                    sendError(ex, 404, "not found");
                } else{
                    sendError(ex, 400, msg != null ? msg : "bad request");
                }
        }
    }

    // GET /users/me/ratings
    public void listMine(HttpExchange ex) throws IOException {
        UUID userId =  auth.requireUserId(ex);
        var list = service.listForUser(userId);
        sendJson(ex, 200, mapper.writeValueAsBytes(list));
    }

    // PUT /ratings/{ratingId}
    public void update(HttpExchange ex, UUID ratingId) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if(ct == null || !ct.toLowerCase().contains("application/json")){
            sendError(ex, 405, "unsupported Media Type");
            return;
        }

        try(InputStream in = ex.getRequestBody()){
            UUID userId = auth.requireUserId(ex);
            RatingRequest req = mapper.readValue(in, RatingRequest.class);
            service.update(ratingId, userId, req);
            sendEmpty(ex, 204);
        } catch(IllegalArgumentException e){
            String msg = e.getMessage();
            if("rating not found".equalsIgnoreCase(msg)){
                sendError(ex, 404, "not found");
            } else{
                sendError(ex, 400, msg != null ? msg : "bad request");
            }
        } catch(SecurityException e){
            sendError(ex, 403, "forbidden");
        }
    }

    // DELETE /ratings/{ratingId}
    public void delete(HttpExchange ex, UUID ratingId) throws IOException {
        try{
            UUID userId = auth.requireUserId(ex);
            service.delete(ratingId, userId);
            sendEmpty(ex, 204);
        } catch(IllegalArgumentException e){
            String msg = e.getMessage();
            if("rating not found".equalsIgnoreCase(msg)){
                sendError(ex, 404, "not found");
            } else{
                sendError(ex, 400, msg != null ? msg : "bad request");
            }
        } catch(SecurityException e){
            sendError(ex, 403, "forbidden");
        }
    }

    private void sendJson(HttpExchange ex, int status, byte[] json) throws IOException {
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, json.length);
        ex.getResponseBody().write(json);
        ex.close();
    }

    private void sendError(HttpExchange ex, int code, String msg) throws IOException {
        byte[] body = ("{\"error\":\"" + msg + "\"}")
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
