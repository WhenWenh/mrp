package mrp;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.sun.net.httpserver.HttpServer;

import mrp.application.security.PasswordHasher;

import mrp.application.MediaService;
import mrp.application.UserService;
import mrp.application.RatingService;
import mrp.application.FavoriteService;
import mrp.application.RecommendationService;
import mrp.application.LeaderboardService;

import mrp.domain.ports.AuthTokenService;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.UserRepository;
import mrp.domain.ports.RatingRepository;
import mrp.domain.ports.FavoriteRepository;

import mrp.infrastructure.http.MediaHandler;
import mrp.infrastructure.http.Router;
import mrp.infrastructure.http.UserHandler;
import mrp.infrastructure.http.RatingHandler;
import mrp.infrastructure.http.FavoriteHandler;
import mrp.infrastructure.http.RecommendationHandler;
import mrp.infrastructure.http.LeaderboardHandler;

import mrp.infrastructure.persistence.JdbcMediaRepository;
import mrp.infrastructure.persistence.JdbcUserRepository;
import mrp.infrastructure.persistence.JdbcRatingRepository;
import mrp.infrastructure.persistence.JdbcFavoriteRepository;

import mrp.infrastructure.security.AuthService;
import mrp.infrastructure.security.OpaqueTokenService;

import java.net.InetSocketAddress;
import java.util.UUID;

public class Main {
    public static void main(String[] args) throws Exception {
        int port = 8080;

        //TODO: Router eventuel verschieben
        //TODO: Singelton desgin pattern überprüfen

        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.configure(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        mapper.configure(com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);


        UserRepository userRepo = new JdbcUserRepository();
        MediaRepository mediaRepo = new JdbcMediaRepository();
        RatingRepository ratingRepo = new JdbcRatingRepository();
        FavoriteRepository favoriteRepo = new JdbcFavoriteRepository();

        AuthTokenService tokenService = new OpaqueTokenService();
        AuthService authService = new AuthService(tokenService);

        PasswordHasher passwordHasher = new PasswordHasher(12); //Cost = 12

        UserService userService = new UserService(userRepo, tokenService, ratingRepo, passwordHasher);
        MediaService mediaService = new MediaService(mediaRepo);
        RatingService ratingService = new RatingService(ratingRepo, mediaRepo);
        FavoriteService favoriteService = new FavoriteService(favoriteRepo, mediaRepo);
        RecommendationService recommendationService = new RecommendationService(ratingRepo, mediaRepo);
        LeaderboardService leaderboardService = new LeaderboardService(userRepo);

        UserHandler userHandler = new UserHandler(mapper, userService, authService);
        MediaHandler mediaHandler = new MediaHandler(mapper, mediaService, authService);
        RatingHandler ratingHandler = new RatingHandler(mapper, ratingService, authService);
        FavoriteHandler favoriteHandler = new FavoriteHandler(mapper, favoriteService, authService);
        RecommendationHandler recommendationHandler = new RecommendationHandler(mapper, recommendationService, authService);
        LeaderboardHandler leaderboardHandler = new LeaderboardHandler(mapper, leaderboardService, authService);

        Router router = new Router(mapper,"/api");

        // User
        router.add("POST", "^/users/register$", userHandler);
        router.add("POST", "^/users/login$",    userHandler);
        router.add("GET",  "^/users/me$",       userHandler);
        router.add("GET",  "^/users/([0-9a-fA-F-]{36})/profile$", userHandler);
        router.add("PUT",  "^/users/([0-9a-fA-F-]{36})/profile$", userHandler);

        // Media
        router.add("POST", "^/media$",          (ex, m) -> mediaHandler.create(ex));
        router.add("GET",  "^/media$",          (ex, m) -> mediaHandler.list(ex));
        router.add("GET",  "^/media/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1)); mediaHandler.getOne(ex, id);
        });
        router.add("PUT",  "^/media/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1)); mediaHandler.update(ex, id);
        });
        router.add("DELETE", "^/media/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1)); mediaHandler.delete(ex, id);
        });
        // Ratings
        router.add("POST", "^/media/([0-9a-fA-F-]{36})/rate$", (ex, m) -> {
            UUID mediaId = UUID.fromString(m.group(1));
            ratingHandler.rateMedia(ex, mediaId);
        });
        router.add("GET", "^/media/([0-9a-fA-F-]{36})/ratings$", (ex, m) -> {
            UUID mediaId = UUID.fromString(m.group(1));
            ratingHandler.listForMedia(ex, mediaId);
        });
        //SPEC Ansatz
        router.add("GET", "^/users/([0-9a-fA-F-]{36})/ratings$", (ex, m) -> {
            UUID userId = UUID.fromString(m.group(1));
            ratingHandler.listForUser(ex, userId);
        });
        //Mein Ansatz
        router.add("GET", "^/users/me/ratings$", (ex, m) -> {
            ratingHandler.listMine(ex);
        });

        router.add("PUT", "^/ratings/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID ratingId = UUID.fromString(m.group(1));
            ratingHandler.update(ex, ratingId);
        });
        router.add("DELETE", "^/ratings/([0-9a-fA-F-]{36})$", (ex, m) -> {
            UUID ratingId = UUID.fromString(m.group(1));
            ratingHandler.delete(ex, ratingId);
        });
        router.add("POST", "^/ratings/([0-9a-fA-F-]{36})/confirm$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1));
            ratingHandler.confirmComment(ex, id);
        });
        router.add("POST", "^/ratings/([0-9a-fA-F-]{36})/like$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1));
            ratingHandler.like(ex, id);
        });
        router.add("DELETE", "^/ratings/([0-9a-fA-F-]{36})/like$", (ex, m) -> {
            UUID id = UUID.fromString(m.group(1));
            ratingHandler.unlike(ex, id);
        });

        //favorites
        router.add("POST", "^/media/([0-9a-fA-F-]{36})/favorite$", (ex, m) -> {
            UUID mediaId = UUID.fromString(m.group(1));
            favoriteHandler.add(ex, mediaId);
        });
        router.add("DELETE", "^/media/([0-9a-fA-F-]{36})/favorite$", (ex, m) -> {
            UUID mediaId = UUID.fromString(m.group(1));
            favoriteHandler.remove(ex, mediaId);
        });

        //SPEC Ansatz
        router.add("GET", "^/users/([0-9a-fA-F-]{36})/favorites$", (ex, m) -> {
            UUID userId = UUID.fromString(m.group(1));
            favoriteHandler.listForUser(ex, userId);
        });
        //Mein Ansatz
        router.add("GET", "^/users/me/favorites$", (ex, m) -> {
            favoriteHandler.listMine(ex);
        });

        //Recommendation
        router.add("GET", "^/users/([0-9a-fA-F-]{36})/recommendations$", (ex, m) -> {
            UUID userId = UUID.fromString(m.group(1));
            recommendationHandler.listForUser(ex, userId);
        });

        router.add("GET", "^/users/me/recommendations$", (ex, m) -> {
            recommendationHandler.listMine(ex);
        });

        //Leaderboard
        router.add("GET", "^/leaderboard$", (ex, m) -> {
            leaderboardHandler.list(ex);
        });


        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/api", router::handle);
        server.setExecutor(null);
        server.start();
        System.out.println("MRP HTTP Server läuft auf http://localhost:" + port + "/api");
        Runtime.getRuntime().addShutdownHook(new Thread(() -> { server.stop(0); }));

    }
}
