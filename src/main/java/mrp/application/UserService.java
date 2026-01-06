    package mrp.application;

    import mrp.application.security.PasswordHasher;
    import mrp.domain.model.User;
    import mrp.domain.ports.AuthTokenService;
    import mrp.domain.ports.UserRepository;
    import mrp.dto.TokenResponse;

    import mrp.domain.model.Rating;
    import mrp.domain.ports.RatingRepository;
    import mrp.dto.UserRatingStats;

    import java.util.List;
    import java.util.Optional;
    import java.util.UUID;

    /**
     * Application-Use-Cases für User (Register, Login, Profile).
     * Kennt NUR Ports (UserRepository, AuthTokenService) – keine Infrastruktur.
     */
    public class UserService {

        private UserRepository users;
        private AuthTokenService tokens;
        private RatingRepository ratings;
        private PasswordHasher passwordHasher;

        public UserService(UserRepository users, AuthTokenService tokens, RatingRepository ratings, PasswordHasher passwordHasher) {
            if (users == null) throw new IllegalArgumentException("users null");
            if (tokens == null) throw new IllegalArgumentException("tokens null");
            if (ratings == null) throw new IllegalArgumentException("ratings null");
            this.users = users;
            this.tokens = tokens;
            this.ratings = ratings;
            this.passwordHasher = passwordHasher;
        }

        /*
         * Registriert einen neuen Nutzer.
         * @throws IllegalStateException wenn Username bereits existiert
         * @throws IllegalArgumentException bei ungültigen Eingaben
         */

        public UserRatingStats getUserRatingStats(UUID userId) {
            List<Rating> ratingList = ratings.listByUser(userId);

            int total = ratingList.size();
            if (total == 0) {
                return new UserRatingStats(0, 0.0);
            }

            int sum = 0;
            for (Rating r : ratingList) {
                sum += r.getStars();
            }

            double avg = (double) sum / (double) total;
            return new UserRatingStats(total, avg);
        }

        public User register(String username, String rawPassword) {
            validateCredentials(username, rawPassword);

            Optional<User> existing = users.findByUsername(username);
            if (existing.isPresent()) {
                throw new IllegalStateException("username already exists");
            }

            String hash = passwordHasher.hash(rawPassword);
            return users.create(username, hash);
        }

        /*
         * Login mit Username/Password – liefert ein Bearer-Token.
         * @throws IllegalArgumentException bei ungültigen Credentials
         */



        public String login(String username, String rawPassword) {
            validateCredentials(username, rawPassword);

            Optional<User> userOpt = users.findByUsername(username);
            if (userOpt.isEmpty()) {
                throw new SecurityException("invalid username or password");
            }

            User u = userOpt.get();
            if (!passwordHasher.matches(rawPassword, u.getPasswordHash())) {
                throw new SecurityException("invalid username or password");
            }

            return tokens.issueToken(u.getId(), u.getUsername());
        }

        public User getProfile(UUID userId) {
            if (userId == null) throw new IllegalArgumentException("userId null");
            return users.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException("user not found"));
        }

        public User updateProfile(UUID userId, String email, String favoriteGenre) {
            if (userId == null) throw new IllegalArgumentException("userId null");
            users.updateProfile(userId, email, favoriteGenre);
            return getProfile(userId);
        }

        private void validateCredentials(String username, String rawPassword) {
            if (username == null || username.isBlank())
                throw new IllegalArgumentException("username blank");
            if (rawPassword == null || rawPassword.isBlank())
                throw new IllegalArgumentException("password blank");
        }
    }
