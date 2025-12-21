package mrp.infrastructure.http;

import com.sun.net.httpserver.HttpExchange;

import java.util.regex.Matcher;

public interface RouteHandler {
    void handle(HttpExchange exchange, Matcher matcher) throws Exception;
}
