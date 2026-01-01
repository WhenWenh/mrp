package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.UserService;
import mrp.domain.model.User;
import mrp.dto.TokenResponse;
import mrp.dto.UserCredentials;
import mrp.dto.UserResponse;
import mrp.infrastructure.security.AuthService;

import mrp.dto.UserProfileResponse;
import mrp.dto.UserProfileUpdate;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.regex.Matcher;

public class UserHandler implements RouteHandler {

    private ObjectMapper mapper;
    private UserService service;
    private AuthService auth;
    private HttpResponses resp;

    public UserHandler(ObjectMapper mapper, UserService service, AuthService auth) {
        if (mapper == null) throw new IllegalArgumentException("mapper null");
        this.mapper = mapper;
        this.service = service;
        this.auth = auth;
        this.resp = new HttpResponses(mapper);
    }

    @Override
    public void handle(HttpExchange exchange, Matcher matcher) throws Exception {
        String method = exchange.getRequestMethod().toUpperCase();
        String path = exchange.getRequestURI().getPath();

        if ("POST".equals(method) && path.endsWith("/users/register")) { register(exchange); return; }
        if ("POST".equals(method) && path.endsWith("/users/login"))    { login(exchange);    return; }
        if ("GET".equals(method)  && path.endsWith("/users/me"))       { me(exchange);       return; }

        if (matcher != null && matcher.groupCount() >= 1
                && path.matches(".*/users/[0-9a-fA-F-]{36}/profile$")) {

            UUID userId = UUID.fromString(matcher.group(1));

            if ("GET".equals(method)) { profile(exchange, userId); return; }
            if ("PUT".equals(method)) { updateProfile(exchange, userId); return; }
        }

        // vorher: exchange.sendResponseHeaders(404, -1);
        resp.error(exchange, 404, "not found");
    }

    private void register(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            UserCredentials creds = mapper.readValue(in, UserCredentials.class);
            User u = service.register(creds.username, creds.password);
            resp.json(ex, 201, new UserResponse(u.getId(), u.getUsername()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            resp.error(ex, 400, "invalid json");
        } catch (IllegalArgumentException e) {
            resp.error(ex, 400, e.getMessage());
        } catch (IllegalStateException e) {
            resp.error(ex, 409, e.getMessage());
        }
    }

    private void login(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            UserCredentials creds = mapper.readValue(in, UserCredentials.class);
            String token = service.login(creds.username, creds.password);
            resp.json(ex, 200, new TokenResponse(token));
        } catch (IllegalArgumentException e) {
            // validateCredentials -> 400
            resp.error(ex, 400, e.getMessage());
        } catch (SecurityException se) {
            // falsche Credentials -> 401
            resp.error(ex, 401, se.getMessage());
        }
    }

    private void me(HttpExchange ex) throws IOException {
        UUID userId;
        try {
            userId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            resp.error(ex, 401, e.getMessage());
            return;
        }

        try {
            User u = service.getProfile(userId);
            resp.json(ex, 200, new UserResponse(u.getId(), u.getUsername()));
        } catch (IllegalArgumentException e) {
            // "user not found" -> 404, alles andere 400 (theoretisch)
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                resp.error(ex, 404, "not found");
            } else {
                resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
            }
        }
    }


    private void profile(HttpExchange ex, UUID userId) throws IOException {
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
            User u = service.getProfile(userId);
            mrp.dto.UserRatingStats stats = service.getUserRatingStats(userId);

            UserProfileResponse dto = toProfileResponse(u, stats.totalRatings, stats.averageScore);
            resp.json(ex, 200, dto);

        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            if (msg != null && msg.toLowerCase().contains("not found")) {
                resp.error(ex, 404, msg);
            } else {
                resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
            }
        }
    }


    private void updateProfile(HttpExchange ex, UUID userId) throws IOException {
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

        try (InputStream in = ex.getRequestBody()) {
            UserProfileUpdate update = mapper.readValue(in, UserProfileUpdate.class);
            User u = service.updateProfile(userId, update.email, update.favoriteGenre);

            mrp.dto.UserRatingStats stats = service.getUserRatingStats(userId);
            UserProfileResponse dto = toProfileResponse(u, stats.totalRatings, stats.averageScore);
            resp.json(ex, 200, dto);

        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            resp.error(ex, 400, "invalid json");
        } catch (IllegalArgumentException e) {
            String msg = e.getMessage();
            resp.error(ex, 400, (msg == null || msg.isBlank()) ? "bad request" : msg);
        }
    }


    private UserProfileResponse toProfileResponse(User u, int totalRatings, double averageScore) {
        UserProfileResponse dto = new UserProfileResponse();
        dto.id = u.getId();
        dto.username = u.getUsername();
        dto.email = u.getEmail();
        dto.favoriteGenre = u.getFavoriteGenre();
        dto.totalRatings = totalRatings;
        dto.averageScore = averageScore;
        return dto;
    }
}
