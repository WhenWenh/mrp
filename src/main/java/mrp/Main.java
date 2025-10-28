package mrp;

import com.fasterxml.jackson.databind.jsontype.impl.ClassNameIdResolver;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import mrp.application.UserService;
import mrp.domain.ports.AuthTokenService;
import mrp.domain.ports.UserRepository;
import mrp.infrastructure.config.ConnectionFactory;
import mrp.infrastructure.http.Router;
import mrp.infrastructure.http.UserHandler;
import mrp.infrastructure.persistence.JdbcUserRepository;
import mrp.infrastructure.security.AuthService;
import mrp.infrastructure.security.OpaqueTokenService;
import mrp.infrastructure.persistence.JdbcMediaRepository;
import mrp.infrastructure.http.MediaHandler;
import mrp.domain.ports.MediaRepository;
import mrp.application.MediaService;

import java.net.InetSocketAddress;
import java.util.UUID;
import mrp.infrastructure.util.UUIDv7;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = 8080;
        ObjectMapper mapper = new ObjectMapper();

        // Ports -> Implementierungen (plain JDBC + opaque tokens)
        UserRepository userRepo = new JdbcUserRepository();
        MediaRepository mediaRepo = new JdbcMediaRepository();
        AuthTokenService tokenService = new OpaqueTokenService();

        // Services
        UserService userService = new UserService(userRepo, tokenService);
        MediaService mediaService = new MediaService(mediaRepo);
        AuthService authService = new AuthService(tokenService);


        // HTTP
        UserHandler userHandler = new UserHandler(userService, authService);
        MediaHandler mediaHandler = new MediaHandler(mapper, mediaService, authService);



        Router router = new Router("/api");

        //User
        router.add("POST", "^/users/register$", userHandler);
        router.add("POST", "^/users/login$",    userHandler);
        router.add("GET",  "^/users/me$",       userHandler);
        //router.add("POST", "^/users/logout$",   userHandler);

        //Media
        router.add("POST", "^/media$",          (ex, m) -> mediaHandler.create(ex));
        router.add("GET",  "^/media$",          (ex, m) -> mediaHandler.list(ex));
        router.add("GET", "^/media/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1));
            mediaHandler.getOne(ex, id);
        });
        router.add("PUT", "^/media/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1));
            mediaHandler.update(ex, id);
        });
        router.add("DELETE", "^/media/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1));
            mediaHandler.delete(ex, id);
        });

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
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
