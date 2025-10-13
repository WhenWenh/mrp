package mrp;

import com.sun.net.httpserver.HttpServer;
import mrp.application.UserService;
import mrp.domain.ports.AuthTokenService;
import mrp.domain.ports.UserRepository;
import mrp.infrastructure.http.Router;
import mrp.infrastructure.http.UserHandler;
import mrp.infrastructure.persistence.JdbcUserRepository;
import mrp.infrastructure.security.AuthService;
import mrp.infrastructure.security.OpaqueTokenService;

import java.net.InetSocketAddress;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;

        // Ports -> Implementierungen (plain JDBC + opaque tokens)
        UserRepository userRepo = new JdbcUserRepository();
        AuthTokenService tokenService = new OpaqueTokenService();

        // Services
        UserService userService = new UserService(userRepo, tokenService);
        AuthService authService = new AuthService(tokenService);

        // HTTP
        UserHandler userHandler = new UserHandler(userService, authService);
        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);

        Router router = new Router("/api");
        router.add("POST", "^/users/register$", userHandler);
        router.add("POST", "^/users/login$",    userHandler);
        router.add("GET",  "^/users/me$",       userHandler);
        //router.add("POST", "^/users/logout$",   userHandler);

        server.createContext("/api", router::handle);
        server.setExecutor(null);
        server.start();

        System.out.println("MRP HTTP Server läuft auf http://localhost:" + port + "/api");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Server fährt herunter …");
            server.stop(0);
        }));
    }
}
