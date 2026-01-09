package mrp.infrastructure.http;

import java.util.UUID;

public class Routes {

    public static void register(
            Router router,
            UserHandler userHandler,
            MediaHandler mediaHandler,
            RatingHandler ratingHandler,
            FavoriteHandler favoriteHandler,
            RecommendationHandler recommendationHandler,
            LeaderboardHandler leaderboardHandler
    ) {
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
        router.add("GET", "^/users/([0-9a-fA-F-]{36})/ratings$", (ex, m) -> {
            UUID userId = UUID.fromString(m.group(1));
            ratingHandler.listForUser(ex, userId);
        });
        router.add("GET", "^/users/me/ratings$", (ex, m) -> ratingHandler.listMine(ex));

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

        // Favorites
        router.add("POST", "^/media/([0-9a-fA-F-]{36})/favorite$", (ex, m) -> {
            UUID mediaId = UUID.fromString(m.group(1));
            favoriteHandler.add(ex, mediaId);
        });
        router.add("DELETE", "^/media/([0-9a-fA-F-]{36})/favorite$", (ex, m) -> {
            UUID mediaId = UUID.fromString(m.group(1));
            favoriteHandler.remove(ex, mediaId);
        });
        router.add("GET", "^/users/([0-9a-fA-F-]{36})/favorites$", (ex, m) -> {
            UUID userId = UUID.fromString(m.group(1));
            favoriteHandler.listForUser(ex, userId);
        });
        router.add("GET", "^/users/me/favorites$", (ex, m) -> favoriteHandler.listMine(ex));

        // Recommendations
        router.add("GET", "^/users/([0-9a-fA-F-]{36})/recommendations$", (ex, m) -> {
            UUID userId = UUID.fromString(m.group(1));
            recommendationHandler.listForUser(ex, userId);
        });
        router.add("GET", "^/users/me/recommendations$", (ex, m) -> recommendationHandler.listMine(ex));

        // Leaderboard
        router.add("GET", "^/leaderboard$", (ex, m) -> leaderboardHandler.list(ex));
    }
}
