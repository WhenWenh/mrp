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
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;

//TODO Tokens sollen geupdated werden nach jedem login kein neuer insert
//TODO Einzelne Exceptions für verschiedene Fehlertypen werfen und auffangen
//TODO Passwort wiedergeben aber mit ****, id ausgeben

public class UserHandler implements RouteHandler {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private UserService service;
    private AuthService auth;

    public UserHandler(UserService service, AuthService auth) {
        this.service = service;
        this.auth = auth;
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

            if ("GET".equals(method)) {
                profile(exchange, userId);
                return;
            }
            if ("PUT".equals(method)) {
                updateProfile(exchange, userId);
                return;
            }
        }

        exchange.sendResponseHeaders(404, -1);
    }

    private void register(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            UserCredentials creds = MAPPER.readValue(in, UserCredentials.class);
            User u = service.register(creds.username, creds.password);
            byte[] json = MAPPER.writeValueAsBytes(new UserResponse(u.getId(), u.getUsername()));
            sendJson(ex, 201, json);
        } catch (IllegalArgumentException e) {
            sendText(ex, 400, e.getMessage());
        } catch (IllegalStateException e) {
            sendText(ex, 409, e.getMessage());
        }
    }

    private void login(HttpExchange ex) throws IOException {
        try (InputStream in = ex.getRequestBody()) {
            UserCredentials creds = MAPPER.readValue(in, UserCredentials.class);
            String token = service.login(creds.username, creds.password);
            byte[] json = MAPPER.writeValueAsBytes(new TokenResponse(token));
            sendJson(ex, 200, json);
        } catch (IllegalArgumentException e) {
            // aus validateCredentials -> 400
            sendText(ex, 400, e.getMessage()); // "username blank" / "password blank"
        } catch (SecurityException e) {
            // falsche Credentials -> 401
            sendText(ex, 401, "invalid credentials");
        }
    }

    private void me(HttpExchange ex) throws IOException {
        try {
            String authHeader = ex.getRequestHeaders().getFirst("Authorization");
            UUID userId = auth.userIdFromAuthHeader(authHeader);
            User u = service.getProfile(userId);
            byte[] json = MAPPER.writeValueAsBytes(new UserResponse(u.getId(), u.getUsername()));
            sendJson(ex, 200, json);
        } catch (IllegalArgumentException e) {
            sendText(ex, 401, e.getMessage());
        }
    }

    private void profile(HttpExchange ex, UUID userId) throws IOException {
        // Auth-Pflicht
        UUID authUserId;
        try {
            authUserId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            // kein / ungültiger Authorization-Header
            sendText(ex, 401, e.getMessage());
            return;
        }

        // Nutzer darf nur sein eigenes Profil sehen
        if (!authUserId.equals(userId)) {
            sendText(ex, 403, "forbidden");
            return;
        }

        try {
            User u = service.getProfile(userId);

            mrp.dto.UserRatingStats stats = service.getUserRatingStats(userId);

            UserProfileResponse dto = toProfileResponse(u, stats.totalRatings, stats.averageScore);
            byte[] json = MAPPER.writeValueAsBytes(dto);
            sendJson(ex, 200, json);
        } catch (IllegalArgumentException e) {
            // user not found
            sendText(ex, 404, e.getMessage());
        }
    }

    private void updateProfile(HttpExchange ex, UUID userId) throws IOException {
        UUID authUserId;
        try {
            authUserId = auth.requireUserId(ex);
        } catch (IllegalArgumentException e) {
            sendText(ex, 401, e.getMessage());
            return;
        }

        if (!authUserId.equals(userId)) {
            sendText(ex, 403, "forbidden");
            return;
        }

        try (InputStream in = ex.getRequestBody()) {
            UserProfileUpdate update = MAPPER.readValue(in, UserProfileUpdate.class);
            User u = service.updateProfile(userId, update.email, update.favoriteGenre);

            mrp.dto.UserRatingStats stats = service.getUserRatingStats(userId);

            UserProfileResponse dto = toProfileResponse(u, stats.totalRatings, stats.averageScore);
            byte[] json = MAPPER.writeValueAsBytes(dto);
            sendJson(ex, 200, json);
        } catch (IllegalArgumentException e) {
            sendText(ex, 400, e.getMessage());
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


    private void sendJson(HttpExchange ex, int status, byte[] body) throws IOException {
        ex.getResponseHeaders().set("Content-Type", "application/json");
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(body); }
    }
    private void sendText(HttpExchange ex, int status, String msg) throws IOException {
        byte[] data = msg.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        ex.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        ex.sendResponseHeaders(status, data.length);
        try (OutputStream os = ex.getResponseBody()) { os.write(data); }
    }
}
