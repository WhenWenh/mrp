package mrp;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;

public class Main {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        // hier Router/Handler registrieren:
        // server.createContext("/api/users", exchange -> ...);
        server.setExecutor(null);
        server.start();
        System.out.println("MRP listening on http://localhost:8080");
    }
}
