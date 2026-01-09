package mrp.infrastructure.http;

import com.fasterxml.jackson.databind.ObjectMapper;
import mrp.application.*;
import mrp.application.security.PasswordHasher;
import mrp.domain.ports.*;
import mrp.infrastructure.persistence.*;
import mrp.infrastructure.security.AuthService;
import mrp.infrastructure.security.OpaqueTokenService;

public class AppFactory {

    public Router buildRouter(ObjectMapper mapper) {
        // Repos
        UserRepository userRepo = new JdbcUserRepository();
        MediaRepository mediaRepo = new JdbcMediaRepository();
        RatingRepository ratingRepo = new JdbcRatingRepository();
        FavoriteRepository favoriteRepo = new JdbcFavoriteRepository();

        // Security
        AuthTokenService tokenService = new OpaqueTokenService();
        AuthService authService = new AuthService(tokenService);

        // Services
        PasswordHasher passwordHasher = new PasswordHasher(12);
        UserService userService = new UserService(userRepo, tokenService, ratingRepo, passwordHasher);
        MediaService mediaService = new MediaService(mediaRepo);
        RatingService ratingService = new RatingService(ratingRepo, mediaRepo);
        FavoriteService favoriteService = new FavoriteService(favoriteRepo, mediaRepo);
        RecommendationService recommendationService = new RecommendationService(ratingRepo, mediaRepo);
        LeaderboardService leaderboardService = new LeaderboardService(userRepo);

        // Handlers
        UserHandler userHandler = new UserHandler(mapper, userService, authService);
        MediaHandler mediaHandler = new MediaHandler(mapper, mediaService, authService);
        RatingHandler ratingHandler = new RatingHandler(mapper, ratingService, authService);
        FavoriteHandler favoriteHandler = new FavoriteHandler(mapper, favoriteService, authService);
        RecommendationHandler recommendationHandler = new RecommendationHandler(mapper, recommendationService, authService);
        LeaderboardHandler leaderboardHandler = new LeaderboardHandler(mapper, leaderboardService, authService);

        // Router + Routes
        Router router = new Router(mapper, "/api");

        Routes.register(router,
                userHandler,
                mediaHandler,
                ratingHandler,
                favoriteHandler,
                recommendationHandler,
                leaderboardHandler
        );

        return router;
    }
}
