package mrp.application;

import mrp.domain.model.MediaEntry;
import mrp.domain.model.Rating;
import mrp.domain.model.enums.MediaType;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.MediaSearch;
import mrp.domain.ports.RatingRepository;
import mrp.dto.MediaResponse;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class RecommendationService {

    private RatingRepository ratings;
    private MediaRepository media;

    public RecommendationService(RatingRepository ratings, MediaRepository media) {
        if (ratings == null) throw new IllegalArgumentException("ratings null");
        if (media == null) throw new IllegalArgumentException("media null");
        this.ratings = ratings;
        this.media = media;
    }

    /**
     * Recommendations basierend auf:
     * - Rating-History (Stars gewichten Präferenzen)
     * - Similarity: Genre-Overlap + MediaType Match (+ kleiner avg-score Bonus)
     * - Ausschließen: bereits bewertete Medien
     */
    public List<MediaResponse> recommendForUser(UUID userId, int limit) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        List<Rating> history = ratings.listByUser(userId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        // 1) ratedMediaIds: zum Ausschließen
        Set<UUID> ratedMediaIds = new HashSet<>();
        for (Rating r : history) {
            if (r != null && r.getMediaId() != null) ratedMediaIds.add(r.getMediaId());
        }

        // 2) Präferenzen (Genre + Type) aus Rating-History, gewichtet nach Stars
        Map<String, Integer> genreWeights = new HashMap<>();
        Map<MediaType, Integer> typeWeights = new HashMap<>();

        for (Rating r : history) {
            if (r == null || r.getMediaId() == null) continue;

            MediaEntry rated = media.findById(r.getMediaId()).orElse(null);
            if (rated == null) continue;

            int w = r.getStars(); // 1..5

            List<String> genres = rated.getGenres();
            if (genres != null) {
                for (String g : genres) {
                    if (g == null) continue;
                    String key = g.trim();
                    if (key.isEmpty()) continue;
                    genreWeights.put(key, genreWeights.getOrDefault(key, 0) + w);
                }
            }

            MediaType mt = rated.getMediaType();
            if (mt != null) {
                typeWeights.put(mt, typeWeights.getOrDefault(mt, 0) + w);
            }
        }

        if (genreWeights.isEmpty()) {
            return List.of();
        }

        MediaType preferredType = mostWeightedType(typeWeights);
        String preferredTypeStr = preferredType != null ? preferredType.name() : null;

        // 3) Kandidaten NICHT "alle Medien": hol gezielt pro Top-Genre aus der History
        List<String> topGenres = topKeysByWeight(genreWeights, 3);

        Map<UUID, MediaEntry> candidatesById = new HashMap<>();

        for (String g : topGenres) {
            MediaSearch search = new MediaSearch(
                    null,               // query
                    preferredTypeStr,    // mediaType (String)
                    g,                  // genre
                    null, null,          // yearFrom/yearTo
                    null,               // ageMax
                    "averageScore",      // sortBy
                    "desc",             // sortDir
                    50,                 // limit
                    0                   // offset
            );

            List<MediaEntry> found = media.search(search);
            if (found == null) continue;

            for (MediaEntry e : found) {
                if (e == null || e.getId() == null) continue;
                if (ratedMediaIds.contains(e.getId())) continue; // exclude already rated
                candidatesById.putIfAbsent(e.getId(), e);
            }
        }

        // 4) Similarity-Scoring
        List<ScoredMedia> scored = new ArrayList<>();
        for (MediaEntry c : candidatesById.values()) {
            double s = similarityScore(c, genreWeights, preferredType);
            if (s > 0.0) scored.add(new ScoredMedia(c, s));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // 5) Limit + Output
        List<MediaResponse> out = new ArrayList<>();
        for (int i = 0; i < scored.size() && out.size() < limit; i++) {
            out.add(toResponse(scored.get(i).media));
        }
        return out;
    }

    /**
     * Similarity:
     * - Genre-Overlap (gewichteter Anteil) → 0..1
     * - MediaType Match Bonus → +0.25
     * - kleiner avg-score Bonus → max +0.1
     */
    private double similarityScore(MediaEntry candidate, Map<String, Integer> genreWeights, MediaType preferredType) {
        if (candidate == null) return 0.0;

        double s = 0.0;

        // Genre overlap (weighted)
        List<String> genres = candidate.getGenres();
        if (genres != null && !genres.isEmpty() && genreWeights != null && !genreWeights.isEmpty()) {
            int denom = 0;
            for (Integer w : genreWeights.values()) denom += w;

            int matched = 0;
            for (String g : genres) {
                if (g == null) continue;
                Integer w = genreWeights.get(g.trim());
                if (w != null) matched += w;
            }

            if (denom > 0) s += matched / (double) denom;
        }

        // MediaType match bonus
        if (preferredType != null && candidate.getMediaType() != null && preferredType.equals(candidate.getMediaType())) {
            s += 0.25;
        }

        // small quality bonus
        if (candidate.getAverageScore() != null) {
            s += Math.min(candidate.getAverageScore(), 5.0) / 50.0;
        }

        return s;
    }

    private List<String> topKeysByWeight(Map<String, Integer> weights, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(weights.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size() && out.size() < n; i++) {
            out.add(list.get(i).getKey());
        }
        return out;
    }

    private MediaType mostWeightedType(Map<MediaType, Integer> typeWeights) {
        MediaType best = null;
        int bestW = -1;

        for (Map.Entry<MediaType, Integer> e : typeWeights.entrySet()) {
            if (e.getKey() == null || e.getValue() == null) continue;
            if (e.getValue() > bestW) {
                bestW = e.getValue();
                best = e.getKey();
            }
        }
        return best;
    }

    private MediaResponse toResponse(MediaEntry e) {
        return new MediaResponse(
                e.getId(),
                e.getCreatorId(),
                e.getTitle(),
                e.getDescription(),
                e.getMediaType(),
                e.getReleaseYear(),
                e.getGenres(),
                e.getAgeRestriction(),
                e.getAverageScore(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }

    private static class ScoredMedia {
        private MediaEntry media;
        private double score;

        private ScoredMedia(MediaEntry media, double score) {
            this.media = media;
            this.score = score;
        }
    }
}
