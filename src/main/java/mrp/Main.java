package mrp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;
import mrp.infrastructure.http.AppFactory;
import mrp.infrastructure.http.Router;

import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

        Router router = new AppFactory().buildRouter(mapper);

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", router::handle);
        server.setExecutor(null);
        server.start();


        System.out.println("MRP HTTP Server läuft auf http://localhost:" + port + "/api");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> server.stop(0)));
    }
}
