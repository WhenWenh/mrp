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

    }

    @Nested
    class GetRatingTests {

    }

    @Nested
    class DeleteRatingTests {

    }

    @Nested
    class Like_and_UnlikeRating {

    }

    @Nested
    class ConfirmRatingCommentTests {

    }
}