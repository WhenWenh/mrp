package mrp.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import mrp.domain.model.MediaEntry;
import mrp.domain.model.Rating;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.RatingRepository;

import mrp.dto.RatingRequest;
import mrp.dto.RatingResponse;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class RatingServiceTest {

    private MediaRepository mediaRepo;
    private RatingRepository ratingRepo;
    private RatingService service;

    @BeforeEach
    void setUp() {
        mediaRepo = Mockito.mock(MediaRepository.class);
        ratingRepo = Mockito.mock(RatingRepository.class);

        service = new RatingService(ratingRepo, mediaRepo);
    }

    @Nested
    class CreateRatingTests {
        @Test
        void create_success_createsRating_recalculatesAverage_andUpdatesMedia() {
            UUID userId = UUID.randomUUID();
            UUID mediaId = UUID.randomUUID();

            RatingRequest req = Mockito.mock(RatingRequest.class);
            Mockito.when(req.getStars()).thenReturn(5);
            Mockito.when(req.getComment()).thenReturn("great");

            // media exists
            MediaEntry media = Mockito.mock(MediaEntry.class);
            Mockito.when(media.getId()).thenReturn(mediaId);

            // findById is called twice:
            // 1) in create() to ensure media exists
            // 2) in recalcAverageScore() to update avg
            Mockito.when(mediaRepo.findById(mediaId)).thenReturn(Optional.of(media));

            // rating create should return saved rating (we return the argument)
            Mockito.when(ratingRepo.create(Mockito.any(Rating.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            // after create, recalcAverageScore calls ratings.listByMedia(mediaId)
            // simulate two ratings with stars 5 and 3 -> avg 4.0
            Rating r1 = Mockito.mock(Rating.class);
            Mockito.when(r1.getStars()).thenReturn(5);
            Rating r2 = Mockito.mock(Rating.class);
            Mockito.when(r2.getStars()).thenReturn(3);
            Mockito.when(ratingRepo.listByMedia(mediaId)).thenReturn(List.of(r1, r2));

            // MediaRepository.update(...) likely returns boolean in your project (like in MediaService)
            Mockito.when(mediaRepo.update(Mockito.any(MediaEntry.class))).thenReturn(true);

            RatingResponse res = service.create(userId, mediaId, req);

            // verify rating persisted
            Mockito.verify(ratingRepo).create(Mockito.any(Rating.class));

            // avg updated
            Mockito.verify(media).setAverageScore(4.0);
            Mockito.verify(mediaRepo).update(media);

            // comment visibility: for creator (requester=userId) comment is visible even if not confirmed
            // (Assuming RatingResponse has getComment())
            assertEquals("great", res.getComment());
            assertFalse(res.isCommentConfirmed());
            assertEquals(0, res.getLikeCount());
        }

        @Test
        void create_starsOutOfRange_throwsIllegalArgumentException_andDoesNotTouchRepos() {
            UUID userId = UUID.randomUUID();
            UUID mediaId = UUID.randomUUID();

            RatingRequest req = Mockito.mock(RatingRequest.class);
            Mockito.when(req.getStars()).thenReturn(0); // invalid
            Mockito.when(req.getComment()).thenReturn("x");

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    service.create(userId, mediaId, req)
            );
            assertEquals("stars must be 1..5", ex.getMessage());

            Mockito.verifyNoInteractions(ratingRepo);
            Mockito.verifyNoInteractions(mediaRepo);
        }
    }

    @Nested
    class Like_Rating {
        @Test
        void like_ownRating_throwsSecurityException_andDoesNotAddLike() {
            UUID ratingId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();

            Rating r = Mockito.mock(Rating.class);
            Mockito.when(r.getUserId()).thenReturn(userId);

            Mockito.when(ratingRepo.findById(ratingId)).thenReturn(Optional.of(r));

            SecurityException ex = assertThrows(SecurityException.class, () ->
                    service.like(ratingId, userId)
            );
            assertEquals("forbidden: cannot like own rating", ex.getMessage());

            Mockito.verify(ratingRepo, Mockito.never()).addLike(Mockito.any(), Mockito.any());
        }

        @Test
        void like_alreadyLiked_throwsIllegalStateException() {
            UUID ratingId = UUID.randomUUID();
            UUID authorId = UUID.randomUUID();
            UUID actorId = UUID.randomUUID();

            Rating r = Mockito.mock(Rating.class);
            Mockito.when(r.getUserId()).thenReturn(authorId);

            Mockito.when(ratingRepo.findById(ratingId)).thenReturn(Optional.of(r));
            Mockito.when(ratingRepo.addLike(ratingId, actorId)).thenReturn(false);

            IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                    service.like(ratingId, actorId)
            );
            assertEquals("already liked", ex.getMessage());
        }
    }

    @Nested
    class ConfirmRatingCommentTests {
        @Test
        void confirmComment_noComment_throwsIllegalArgumentException_andDoesNotConfirm() {
            UUID ratingId = UUID.randomUUID();
            UUID userId = UUID.randomUUID();
            UUID mediaId = UUID.randomUUID();

            Rating existing = new Rating(
                    ratingId,
                    mediaId,
                    userId,
                    4,
                    "   ", // blank
                    false,
                    Instant.now(),
                    0
            );

            Mockito.when(ratingRepo.findById(ratingId)).thenReturn(Optional.of(existing));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                    service.confirmComment(ratingId, userId)
            );
            assertEquals("no comment", ex.getMessage());

            Mockito.verify(ratingRepo, Mockito.never()).confirmComment(Mockito.any(), Mockito.any());
        }
    }
}