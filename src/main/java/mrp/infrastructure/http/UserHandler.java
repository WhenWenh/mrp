package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import mrp.application.UserService;
import mrp.domain.model.User;
import mrp.dto.TokenResponse;
import mrp.dto.UserCredentials;
import mrp.dto.UserResponse;
import mrp.infrastructure.security.AuthService;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;
import java.util.regex.Matcher;

//TODO Tokens sollen geupdated werden nach jedem login kein neuer insert

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
        //if ("POST".equals(method) && path.endsWith("/users/logout"))   { logout(exchange);   return; }

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

   /* private void logout(HttpExchange ex) throws IOException {
        try {
            String authHeader = ex.getRequestHeaders().getFirst("Authorization");
            auth.revokeFromAuthHeader(authHeader);
            ex.sendResponseHeaders(204, -1);
        } catch (IllegalArgumentException e) {
            sendText(ex, 401, e.getMessage());
        }
    }
*/
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
