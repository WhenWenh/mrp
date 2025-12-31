package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.RatingService;
import mrp.dto.RatingRequest;
import mrp.infrastructure.security.AuthService;

import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import com.fasterxml.jackson.core.JsonProcessingException;


import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;


public class RatingHandler {
    private ObjectMapper mapper;
    private RatingService service;
    private AuthService auth;
    private HttpResponses resp;

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
        this.resp = new HttpResponses(mapper);
    }

    // POST /media/{mediaId}/ratings
    public void rateMedia(HttpExchange ex, UUID mediaId) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if(ct == null || !ct.toLowerCase().contains("application/json")){
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

        try(InputStream in = ex.getRequestBody()){
            RatingRequest req = mapper.readValue(in, RatingRequest.class);
            Object rated = service.create(userId, mediaId, req);
            resp.json(ex, 201, rated);
        }catch (InvalidFormatException e) {
            // z.B. falsches Enum / falscher Typ im JSON
            resp.error(ex, 400, "invalid value");

        } catch (JsonProcessingException e) {
            // kaputtes JSON
            resp.error(ex, 400, "invalid json");

        }catch (IllegalStateException e) {
            //duplicate rating
            String msg = e.getMessage();
            if (msg != null && "rating already exists".equalsIgnoreCase(msg)) {
                resp.error(ex, 409, "rating already exists for this media");
            } else {
                resp.error(ex, 409, (msg == null || msg.isBlank()) ? "conflict" : msg);
            }
        } catch(IllegalArgumentException e){
            String msg = e.getMessage();
            if("media not found".equalsIgnoreCase(msg)){
                resp.error(ex, 404, "not found");
            }else{
                resp.error(ex, 400, e.getMessage());
            }
        } catch(SecurityException e){
            resp.error(ex, 403, "forbidden");
        }
    }

    // GET /media/{mediaId}/ratings
    public void listForMedia(HttpExchange ex, UUID mediaId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try{
            Object list = service.listForMedia(mediaId, userId);
            resp.json(ex, 200, list);
        } catch(IllegalArgumentException e){
                String msg =  e.getMessage();
                if("media not found".equalsIgnoreCase(msg)){
                    resp.error(ex, 404, "not found");
                } else{
                    resp.error(ex, 400, e.getMessage());
                }
        }
    }

    // GET /users/me/ratings
    public void listMine(HttpExchange ex) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        Object list = service.listForUser(userId);
        resp.json(ex, 200, list);
    }

    // PUT /ratings/{ratingId}
    public void update(HttpExchange ex, UUID ratingId) throws IOException {
        String ct = ex.getRequestHeaders().getFirst("Content-Type");
        if (ct == null || !ct.toLowerCase().contains("application/json")) {
            resp.error(ex, 405, "unsupported media type");
            return;
        }

        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage()); // z.B. token expired
            return;
        }

        try (InputStream in = ex.getRequestBody()) {
            RatingRequest req = mapper.readValue(in, RatingRequest.class);
            service.update(ratingId, userId, req);
            resp.empty(ex, 204);

        } catch (InvalidFormatException e) {
            // z.B. falsches Enum / falscher Typ im JSON
            resp.error(ex, 400, "invalid value");

        } catch (JsonProcessingException e) {
            // kaputtes JSON
            resp.error(ex, 400, "invalid json");

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && "rating not found".equalsIgnoreCase(msg)) {
                resp.error(ex, 404, "not found");
            } else {
                resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
            }

        } catch (SecurityException e) {
            resp.error(ex, 403, "forbidden");
        }
    }


    // DELETE /ratings/{ratingId}
    public void delete(HttpExchange ex, UUID ratingId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage()); // token expired / missing header
            return;
        }

        try{
            service.delete(ratingId, userId);
            resp.empty(ex, 204);
        } catch(IllegalArgumentException e){
            String msg = e.getMessage();
            if("rating not found".equalsIgnoreCase(msg)){
                resp.error(ex, 404, "not found");
            } else{
                resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
            }
        } catch(SecurityException e){
            resp.error(ex, 403, "forbidden");
        }
    }

    // POST ratings/{{ratingId}}/confirm-comment
    public void confirmComment(HttpExchange ex, UUID ratingId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage()); // token expired / missing header
            return;
        }

        try {
            service.confirmComment(ratingId, userId);
            resp.empty(ex, 204);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 404, "not found");
        } catch (SecurityException se) {
            resp.error(ex, 403, "forbidden");
        }
    }

    // POST ratings/{{ratingId}}/like
    public void like(HttpExchange ex, UUID ratingId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try {
            service.like(ratingId, userId);
            resp.empty(ex, 204);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
        }
    }

    // DELETE ratings/{{ratingId}}/like
    public void unlike(HttpExchange ex, UUID ratingId) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try {
            service.unlike(ratingId, userId);
            resp.empty(ex, 204);
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
        }
    }
}
