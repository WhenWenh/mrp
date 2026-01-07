package mrp.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.Mockito;

import mrp.application.security.PasswordHasher;
import mrp.domain.model.Rating;
import mrp.domain.model.User;
import mrp.domain.ports.AuthTokenService;
import mrp.domain.ports.RatingRepository;
import mrp.domain.ports.UserRepository;
import mrp.dto.UserRatingStats;


import java.util.List;
import java.util.Optional;
import java.util.UUID;


class UserServiceTest {
    private UserRepository users;
    private AuthTokenService tokens;
    private RatingRepository ratings;
    private PasswordHasher passwordHasher;

    private UserService service;

    @BeforeEach
    void setUp() {
        users = Mockito.mock(UserRepository.class);
        tokens = Mockito.mock(AuthTokenService.class);
        ratings = Mockito.mock(RatingRepository.class);
        passwordHasher = Mockito.mock(PasswordHasher.class);
        service = new UserService(users, tokens, ratings, passwordHasher);
    }


    /**
     * REGISTER
     */

    @Nested
    class RegisterTests {
        @Test
        void register_success() {
            String username = "alice";
            String rawPassword = "secret";
            String hashedPassword = "hashed-secret";

            User createdUser = Mockito.mock(User.class);

            Mockito.when(users.findByUsername(username)).thenReturn(Optional.empty());

            Mockito.when(passwordHasher.hash(rawPassword)).thenReturn(hashedPassword);

            Mockito.when(users.create(username, hashedPassword)).thenReturn(createdUser);

            User result = service.register(username, rawPassword);

            assertSame(createdUser, result);

            Mockito.verify(passwordHasher).hash(rawPassword);

            Mockito.verify(users).create(username, hashedPassword);
        }

        @Test
        void register_usernameAlreadyTaken_throwsException() {
            String username = "Wen";
            String rawPassword = "secret";

            User existingUser = Mockito.mock(User.class);

            Mockito.when(users.findByUsername(username)).thenReturn(Optional.of(existingUser));

            assertThrows(IllegalStateException.class, () ->
                    service.register(username, rawPassword)
            );

            Mockito.verify(users, Mockito.never()).create(Mockito.anyString(), Mockito.anyString());
            Mockito.verify(passwordHasher, Mockito.never()).hash(Mockito.anyString());
        }
    }
    /**
     * LOGIN
     */

    @Nested
    class LoginTests {
        @Test
        void login_wrongPassword_throwsSecurityException() {
            String username = "alice";
            String rawPassword = "wrongPassword";
            String storedHash = "correct-hash";

            UUID userId = UUID.randomUUID();
            User user = Mockito.mock(User.class);

            Mockito.when(user.getId()).thenReturn(userId);
            Mockito.when(user.getUsername()).thenReturn(username);
            Mockito.when(user.getPasswordHash()).thenReturn(storedHash);

            Mockito.when(users.findByUsername(username)).thenReturn(Optional.of(user));
            Mockito.when(passwordHasher.matches(rawPassword, storedHash)).thenReturn(false);

            assertThrows(SecurityException.class, () ->
                    service.login(username, rawPassword)
            );
        }

        @Test
        void login_success_returnsToken() {
            String username = "alice";
            String rawPassword = "secret";
            String token = "alice-mrpToken";

            UUID userId = UUID.randomUUID();
            User user = Mockito.mock(User.class);

            Mockito.when(user.getId()).thenReturn(userId);
            Mockito.when(user.getUsername()).thenReturn(username);
            Mockito.when(user.getPasswordHash()).thenReturn("hash");

            Mockito.when(users.findByUsername(username)).thenReturn(Optional.of(user));

            Mockito.when(passwordHasher.matches(rawPassword, "hash")).thenReturn(true);

            Mockito.when(tokens.issueToken(userId, username)).thenReturn(token);

            String result = service.login(username, rawPassword);

            assertEquals(token, result);
            Mockito.verify(tokens).issueToken(userId, username);
        }

        @Test
        void login_blankPassword_throws_IllegalArgumentException() {
            String username = "alice";
            String blankPassword = "";

            assertThrows(IllegalArgumentException.class, () ->
                    service.login(username, blankPassword)
            );
        }
    }
    /**
     * USER STATS
     */

    @Nested
    class UserStatsTests {
        @Test
        void getUserRatingStats_noRatings_returnsZeroAndZeroAvg() {
            UUID userId = UUID.randomUUID();
            Mockito.when(ratings.listByUser(userId)).thenReturn(List.of());

            UserRatingStats stats = service.getUserRatingStats(userId);

            assertEquals(0, stats.totalRatings);
            assertEquals(0.0, stats.averageScore, 0.000001);
        }

        @Test
        void getUserRatingStats_withRatings_returnsTotalAndAvg() {
            UUID userId = UUID.randomUUID();

            Rating r1 = Mockito.mock(Rating.class);
            Mockito.when(r1.getStars()).thenReturn(5);

            Rating r2 = Mockito.mock(Rating.class);
            Mockito.when(r2.getStars()).thenReturn(3);

            Mockito.when(ratings.listByUser(userId)).thenReturn(List.of(r1, r2));

            UserRatingStats stats = service.getUserRatingStats(userId);

            assertEquals(2, stats.totalRatings);
            assertEquals(4.0, stats.averageScore, 0.000001);
        }
    }

    /**
     * OTHER
     */
    @Nested
    class OtherTests {
        @Test
        void getProfile_userNotFound_throwsIllegalArgumentException() {

            UUID id = UUID.randomUUID();

            IllegalArgumentException e =
                    assertThrows(IllegalArgumentException.class,
                            () -> service.getProfile(id)
                    );

            assertEquals("user not found", e.getMessage());
        }
    }
}