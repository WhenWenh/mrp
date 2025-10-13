package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Router {

    private String basePath;
    private List<Route> routes = new ArrayList<>();
    private ObjectMapper mapper = new ObjectMapper();

    public Router(String basePath) {
        this.basePath = basePath == null ? "" : basePath;
    }

    public void add(String method, String regex, RouteHandler handler) {
        Route r = new Route();
        r.method = method.toUpperCase();
        r.pattern = Pattern.compile(regex);
        r.handler = handler;
        routes.add(r);
    }

    public void handle(HttpExchange exchange) {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            String rel = path.startsWith(basePath) ? path.substring(basePath.length()) : path;

            for (Route r : routes) {
                if (!r.method.equals(method)) continue;
                Matcher m = r.pattern.matcher(rel);
                if (m.matches()) {
                    r.handler.handle(exchange, m);
                    return;
                }
            }

            sendText(exchange, 404, "Not Found");
        } catch (Exception e) {
            e.printStackTrace();
            try {
                sendText(exchange, 500, "Internal Server Error");
            } catch (IOException ioException) {
                ioException.printStackTrace();
            }
        } finally {
            exchange.close();
        }
    }

    public <T> T readJson(HttpExchange exchange, Class<T> type) throws IOException {
        InputStream in = exchange.getRequestBody();
        try {
            return mapper.readValue(in, type);
        } finally {
            in.close();
        }
    }

    public void sendJson(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] json = mapper.writeValueAsBytes(body);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, json.length);
        OutputStream os = exchange.getResponseBody();
        os.write(json);
        os.flush();
    }

    public void sendText(HttpExchange exchange, int status, String text) throws IOException {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, data.length);
        OutputStream os = exchange.getResponseBody();
        os.write(data);
        os.flush();
    }

    private static class Route {
        String method;
        Pattern pattern;
        RouteHandler handler;
    }
}
