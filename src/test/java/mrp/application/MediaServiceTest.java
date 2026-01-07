package mrp.application;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import mrp.dto.MediaRequest;
import mrp.dto.MediaResponse;
import mrp.domain.model.MediaEntry;
import mrp.domain.model.enums.MediaType;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.MediaSearch;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

class MediaServiceTest {

    private MediaRepository repo;
    private MediaService service;

    @BeforeEach
    void setUp() {
        repo = Mockito.mock(MediaRepository.class);
        service = new MediaService(repo);

        Mockito.when(repo.save(Mockito.any(MediaEntry.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    // for update media
    private MediaEntry existingEntry(UUID id, UUID creatorId) {
        return new MediaEntry(
                id,
                creatorId,
                "Old Title",
                "Old Desc",
                MediaType.MOVIE,
                2010,
                List.of("Drama"),
                16,
                3.5,
                Instant.parse("2025-01-01T10:00:00Z"),
                Instant.parse("2025-01-01T10:00:00Z")
        );
    }


    /**
     * CREATE
     */


    @Nested
    class CreateMediaTests {

        @Test
        void create_nullCreatorId_throwsIllegalArgumentException() {
            MediaRequest req = new MediaRequest(
                    "Media Title",
                    "Description of Media",
                    MediaType.MOVIE,
                    2020,
                    List.of("Drama"),
                    12);

            assertThrows(IllegalArgumentException.class, () ->
                    service.create(null, req)
            );

            Mockito.verifyNoInteractions(repo); // Ensures that everything stops to work the moment when the exception is thrown
        }

        @Test
        void create_releaseYear_throwsIllegalArgumentException() {
            UUID creatorId = UUID.randomUUID();
            MediaRequest req = new MediaRequest(
                    "Media Title",
                    "Description of Media",
                    MediaType.MOVIE,
                    1700,
                    List.of("Drama"),
                    12
            );

            assertThrows(IllegalArgumentException.class, () ->
                    service.create(creatorId, req)
            );

            Mockito.verifyNoInteractions(repo);
        }

        @Test
        void create_success_trimsTitle_andSavesEntry() {
            UUID creatorId = UUID.randomUUID();
            MediaRequest req = new MediaRequest(
                    "  Dune  ",
                    "Paul Atreus on the way to reclaim his birthright",
                    MediaType.MOVIE,
                    2014,
                    List.of("Sci-Fi"),
                    12
            );

            ArgumentCaptor<MediaEntry> captor = ArgumentCaptor.forClass(MediaEntry.class);

            service.create(creatorId, req);

            Mockito.verify(repo).save(captor.capture());
            MediaEntry saved = captor.getValue();

            assertEquals("Dune", saved.getTitle()); // trim
            assertEquals(creatorId, saved.getCreatorId());
            assertEquals(MediaType.MOVIE, saved.getMediaType());
            assertEquals(2014, saved.getReleaseYear());
            assertEquals(List.of("Sci-Fi"), saved.getGenres());
            assertEquals(12, saved.getAgeRestriction());
        }

        @Test
        void create_success_setsDefaultsAndSystemFields() {
            UUID creatorId = UUID.randomUUID();
            MediaRequest req = new MediaRequest(
                    "Spongebob",
                    "Spongebob Squarepants",
                    MediaType.MOVIE,
                    2006,
                    List.of("Kids"),
                    12
            );

            ArgumentCaptor<MediaEntry> captor = ArgumentCaptor.forClass(MediaEntry.class);

            service.create(creatorId, req);

            Mockito.verify(repo).save(captor.capture());
            MediaEntry saved = captor.getValue();

            // defaults/system fields created inside create()
            assertNotNull(saved.getId());
            assertEquals(0.0, saved.getAverageScore(), 0.000001);

            assertNotNull(saved.getCreatedAt());
            assertNotNull(saved.getUpdatedAt());

            // In your create() both are set to the same 'now'
            assertEquals(saved.getCreatedAt(), saved.getUpdatedAt());

            // timestamps should not be in the future (optional but safe-ish)
            assertFalse(saved.getCreatedAt().isAfter(Instant.now()));
        }

        @Test
        void create_success_returnsResponseMappedFromSavedEntry() {
            UUID creatorId = UUID.randomUUID();
            MediaRequest req = new MediaRequest(
                    "  Interstellar  ",
                    "Space",
                    MediaType.MOVIE,
                    2014,
                    List.of("Sci-Fi"),
                    12
            );

            MediaResponse res = service.create(creatorId, req);

            assertNotNull(res);

            // Response comes from toResponse(saved), and saved == entry (identity save)
            assertNotNull(res.getId());
            assertEquals(creatorId, res.getCreatorId());
            assertEquals("Interstellar", res.getTitle()); // trimmed
            assertEquals("Space", res.getDescription());
            assertEquals(MediaType.MOVIE, res.getMediaType());
            assertEquals(2014, res.getReleaseYear());
            assertEquals(List.of("Sci-Fi"), res.getGenres());
            assertEquals(12, res.getAgeRestriction());
            assertEquals(0.0, res.getAverageScore(), 0.000001);
            assertNotNull(res.getCreatedAt());
            assertNotNull(res.getUpdatedAt());
            assertEquals(res.getCreatedAt(), res.getUpdatedAt()); // same now
        }
    }
    /**
     * GET
     */


    @Nested
    class GetMediaTests{
    @Test
    void get_nullId_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () ->
                service.get(null)
        );
        Mockito.verifyNoInteractions(repo);
    }

    @Test
    void get_notFound_throwsIllegalArgumentException() {
        UUID id = UUID.randomUUID();
        Mockito.when(repo.findById(id)).thenReturn(Optional.empty());

        IllegalArgumentException ex =
                assertThrows(IllegalArgumentException.class, () ->
                    service.get(id)
                );

        assertEquals("media not found", ex.getMessage());

        Mockito.verify(repo).findById(id);
        Mockito.verifyNoMoreInteractions(repo);
    }

    @Test
    void get_found_returnsMappedResponse() {
        UUID id = UUID.randomUUID();
        UUID creatorId = UUID.randomUUID();
        Instant created = Instant.parse("2025-01-01T10:00:00Z");
        Instant updated = Instant.parse("2025-01-02T10:00:00Z");

        MediaEntry entry = new MediaEntry(
                id,
                creatorId,
                "Dune",
                "Space epic",
                MediaType.MOVIE,
                2021,
                List.of("Sci-Fi"),
                12,
                4.5,
                created,
                updated
        );

        Mockito.when(repo.findById(id)).thenReturn(Optional.of(entry));

        MediaResponse res = service.get(id);

        assertNotNull(res);
        assertEquals(id, res.getId());
        assertEquals(creatorId, res.getCreatorId());
        assertEquals("Dune", res.getTitle());
        assertEquals("Space epic", res.getDescription());
        assertEquals(MediaType.MOVIE, res.getMediaType());
        assertEquals(2021, res.getReleaseYear());
        assertEquals(List.of("Sci-Fi"), res.getGenres());
        assertEquals(12, res.getAgeRestriction());
        assertEquals(4.5, res.getAverageScore(), 0.000001);
        assertEquals(created, res.getCreatedAt());
        assertEquals(updated, res.getUpdatedAt());

        Mockito.verify(repo).findById(id);
        Mockito.verifyNoMoreInteractions(repo);
    }
    }
    /**
     * UPDATE
     */

    @Nested
    class UpdateMediaTests {
        @Test
        void update_requesterNotCreator_throwsSecurityException() {
            UUID id = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            UUID requesterId = UUID.randomUUID(); // different -> forbidden

            MediaRequest req = new MediaRequest(
                    "New Title",
                    "New Desc",
                    MediaType.MOVIE,
                    2014,
                    List.of("Sci-Fi"),
                    12
            );

            Mockito.when(repo.findById(id)).thenReturn(Optional.of(existingEntry(id, creatorId)));

            SecurityException ex =
                    assertThrows(SecurityException.class, () ->
                            service.update(id, requesterId, req)
                    );

            assertEquals("forbidden: not the creator", ex.getMessage());

            Mockito.verify(repo).findById(id);
            Mockito.verify(repo, Mockito.never()).update(Mockito.any(MediaEntry.class));
            Mockito.verifyNoMoreInteractions(repo);
        }

        @Test
        void update_repoUpdateReturnsFalse_throwsIllegalStateException() {
            UUID id = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();

            MediaRequest req = new MediaRequest(
                    "New Title",
                    "New Desc",
                    MediaType.MOVIE,
                    2014,
                    List.of("Sci-Fi"),
                    12
            );

            MediaEntry current = existingEntry(id, creatorId);
            Mockito.when(repo.findById(id)).thenReturn(Optional.of(current));
            Mockito.when(repo.update(Mockito.any(MediaEntry.class))).thenReturn(false);

            IllegalStateException ex =
                    assertThrows(IllegalStateException.class, () ->
                            service.update(id, creatorId, req)
                    );

            assertEquals("update failed", ex.getMessage());

            Mockito.verify(repo).findById(id);
            Mockito.verify(repo).update(Mockito.any(MediaEntry.class));
            Mockito.verifyNoMoreInteractions(repo);
        }

        @Test
        void update_success_updatesFields_andReturnsResponse() {
            UUID id = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();

            MediaRequest req = new MediaRequest(
                    "  Dune  ",
                    "Paul Atreus...",
                    MediaType.MOVIE,
                    2021,
                    List.of("Sci-Fi"),
                    12
            );

            MediaEntry current = existingEntry(id, creatorId);
            Instant beforeUpdatedAt = current.getUpdatedAt();

            Mockito.when(repo.findById(id)).thenReturn(Optional.of(current));
            Mockito.when(repo.update(Mockito.any(MediaEntry.class))).thenReturn(true);

            ArgumentCaptor<MediaEntry> captor = ArgumentCaptor.forClass(MediaEntry.class);

            MediaResponse res = service.update(id, creatorId, req);

            Mockito.verify(repo).findById(id);
            Mockito.verify(repo).update(captor.capture());
            MediaEntry updated = captor.getValue();

            // changed fields
            assertEquals("Dune", updated.getTitle());
            assertEquals("Paul Atreus...", updated.getDescription());
            assertEquals(MediaType.MOVIE, updated.getMediaType());
            assertEquals(2021, updated.getReleaseYear());
            assertEquals(List.of("Sci-Fi"), updated.getGenres());
            assertEquals(12, updated.getAgeRestriction());

            // updatedAt refreshed
            assertNotNull(updated.getUpdatedAt());
            assertNotEquals(beforeUpdatedAt, updated.getUpdatedAt());

            // response mapping (service returns toResponse(current))
            assertNotNull(res);
            assertEquals(id, res.getId());
            assertEquals(creatorId, res.getCreatorId());
            assertEquals("Dune", res.getTitle());
            assertEquals(updated.getUpdatedAt(), res.getUpdatedAt());

            Mockito.verifyNoMoreInteractions(repo);
        }
    }
    /**
     * DELETE
     */

    @Nested
    class DeleteMediaTests {
        @Test
        void delete_requesterIdNull_throwsIllegalArgumentException() {
            UUID id = UUID.randomUUID();

            assertThrows(IllegalArgumentException.class, () ->
                    service.delete(id, null)
            );

            Mockito.verifyNoInteractions(repo);
        }

        @Test
        void delete_repoDeleteReturnsFalse_throwsIllegalArgumentException() {
            UUID id = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID(); // requester == creator

            MediaEntry current = existingEntry(id, creatorId);

            Mockito.when(repo.findById(id)).thenReturn(Optional.of(current));
            Mockito.when(repo.delete(id)).thenReturn(false);

            IllegalArgumentException ex =
                    assertThrows(IllegalArgumentException.class, () ->
                            service.delete(id, creatorId)
                    );

            assertEquals("media not found", ex.getMessage());

            Mockito.verify(repo).findById(id);
            Mockito.verify(repo).delete(id);
            Mockito.verifyNoMoreInteractions(repo);
        }
    }

    /**
     * SEARCH
     */

    @Nested
    class SearchMediaTests {

        @Test
        void search_nullSearch_usesDefaultAndCallsRepoWithIt() {
            ArgumentCaptor<MediaSearch> captor = ArgumentCaptor.forClass(MediaSearch.class);

            Mockito.when(repo.search(Mockito.any(MediaSearch.class)))
                    .thenReturn(List.of()); // empty result ok

            List<MediaResponse> res = service.search(null);

            assertNotNull(res);
            assertTrue(res.isEmpty());

            Mockito.verify(repo).search(captor.capture());
            MediaSearch used = captor.getValue();

            // checks for the important defaults
            assertEquals("title", used.getSortBy());
            assertEquals("asc", used.getSortDir());
            assertEquals(20, used.getLimit());
            assertEquals(0, used.getOffset());

            Mockito.verifyNoMoreInteractions(repo);
        }

        @Test
        void search_mapsEntriesToResponses() {
            MediaSearch s = new MediaSearch(
                    "dune", null, null, null, null, null,
                    "title", "asc", 20, 0
            );

            UUID id = UUID.randomUUID();
            UUID creatorId = UUID.randomUUID();
            Instant created = Instant.parse("2025-01-01T10:00:00Z");
            Instant updated = Instant.parse("2025-01-02T10:00:00Z");

            MediaEntry e = new MediaEntry(
                    id, creatorId, "Dune", "Space epic", MediaType.MOVIE,
                    2021, List.of("Sci-Fi"), 12, 4.5, created, updated
            );


            Mockito.when(repo.search(s)).thenReturn(List.of(e));

            List<MediaResponse> res = service.search(s);

            assertEquals(1, res.size());
            MediaResponse r = res.get(0);

            assertEquals(id, r.getId());
            assertEquals(creatorId, r.getCreatorId());
            assertEquals("Dune", r.getTitle());

            Mockito.verify(repo).search(s);
            Mockito.verifyNoMoreInteractions(repo);
        }
    }
}