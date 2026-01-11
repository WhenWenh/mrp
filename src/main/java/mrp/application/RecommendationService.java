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

/**
 * Application service responsible for generating media recommendations for a user.
 *
 * Recommendation approach (high level):
 * - Uses the user's rating history as preference signal
 * - Only "highly rated" items (>= 4 stars) contribute to preferences
 * - Excludes all already rated media (even 1-star ratings)
 * - Builds candidates via repository search and ranks them by similarity score
 */

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
     * Returns recommendations for a user.
     *
     * Rules:
     * - Preference signal comes only from ratings with stars >= 4
     * - Candidates exclude ALL media the user has already rated
     * - Similarity = genre overlap + media type match bonus + small avg-score bonus
     * - If the user has no ratings >= 4, return an empty list (HTTP layer can still return 200)
     */

    public List<MediaResponse> recommendForUser(UUID userId, int limit) {
        if (userId == null) throw new IllegalArgumentException("userId null");
        if (limit <= 0) limit = 10;
        if (limit > 50) limit = 50;

        // Load rating history. No history => no recommendations.
        List<Rating> history = ratings.listByUser(userId);
        if (history == null || history.isEmpty()) {
            return List.of();
        }

        // Preference signal: only keep "positive" ratings (>= 4 stars).
        List<Rating> positive = new ArrayList<>();
        for (Rating r : history) {
            if (r == null) continue;
            if (r.getStars() >= 4) positive.add(r);
        }
        if (positive.isEmpty()) {
            return List.of(); // 200 + []
        }

        // Exclusion set: all rated media IDs (including negative ratings).
        Set<UUID> ratedMediaIds = new HashSet<>();
        for (Rating r : history) {
            if (r != null && r.getMediaId() != null) ratedMediaIds.add(r.getMediaId());
        }

        // Build weighted preferences (genres + media type) from positive ratings.
        // Weight by stars (4..5) to strengthen stronger likes.
        Map<String, Integer> genreWeights = new HashMap<>();
        Map<MediaType, Integer> typeWeights = new HashMap<>();

        for (Rating r : positive) {
            if (r == null || r.getMediaId() == null) continue;

            MediaEntry rated = media.findById(r.getMediaId()).orElse(null);
            if (rated == null) continue;

            int w = r.getStars(); // 4..5

            // Collect genre weights
            List<String> genres = rated.getGenres();
            if (genres != null) {
                for (String g : genres) {
                    if (g == null) continue;
                    String key = g.trim();
                    if (key.isEmpty()) continue;
                    genreWeights.put(key, genreWeights.getOrDefault(key, 0) + w);
                }
            }

            // Collect media type weights
            MediaType mt = rated.getMediaType();
            if (mt != null) {
                typeWeights.put(mt, typeWeights.getOrDefault(mt, 0) + w);
            }
        }

        // If no genre preferences could be derived, then the service cannot recommend reliably.
        if (genreWeights.isEmpty()) {
            return List.of();
        }

        // Determine a single "preferred" media type (highest weight).
        MediaType preferredType = mostWeightedType(typeWeights);
        String preferredTypeStr = preferredType != null ? preferredType.name() : null;

        // Derive an age preference from positive ratings (used as a max filter).
        Integer preferredAge = preferredAgeMax(positive);

        // Candidate generation: search for media per top genres (e.g., top 3).
        List<String> topGenres = topKeysByWeight(genreWeights, 3);

        Map<UUID, MediaEntry> candidatesById = new HashMap<>();

        for (String g : topGenres) {
            MediaSearch search = new MediaSearch(
                    null,               // title
                    preferredTypeStr,    // mediaType
                    g,                  // genre
                    null,               // releaseYear
                    preferredAge,       // ageRestriction (als max-Filter)
                    null,               // rating (min avg-score) - nicht genutzt
                    "score",            // sortBy
                    "desc",             // sortDir
                    50,
                    0
            );

            List<MediaEntry> found = media.search(search);
            if (found == null) continue;

            for (MediaEntry e : found) {
                if (e == null || e.getId() == null) continue;
                if (ratedMediaIds.contains(e.getId())) continue; // exclude already rated (ALL)
                candidatesById.putIfAbsent(e.getId(), e);       // avoid duplicates across genres
            }
        }

        // Score candidates by similarity and sort descending.
        List<ScoredMedia> scored = new ArrayList<>();
        for (MediaEntry c : candidatesById.values()) {
            double s = similarityScore(c, genreWeights, preferredType);
            if (s > 0.0) scored.add(new ScoredMedia(c, s));
        }

        scored.sort((a, b) -> Double.compare(b.score, a.score));

        // Limit and map to DTO responses.
        List<MediaResponse> out = new ArrayList<>();
        for (int i = 0; i < scored.size() && out.size() < limit; i++) {
            out.add(toResponse(scored.get(i).media));
        }
        return out;
    }

    /**
     * Computes a similarity score for a candidate media entry.
     *
     * Components:
     * - Weighted genre overlap: matchedWeight / totalPreferenceWeight  -> 0..1
     * - Media type match bonus: +0.25 if candidate type equals preferred type
     * - Small quality bonus: avgScore/50 -> max +0.1 (since score max is 5)
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

        // Small quality bonus based on candidate average score
        if (candidate.getAverageScore() != null) {
            s += Math.min(candidate.getAverageScore(), 5.0) / 50.0;
        }

        return s;
    }

    /**
     * Returns the top N keys from a weight map, sorted descending by weight.
     */
    private List<String> topKeysByWeight(Map<String, Integer> weights, int n) {
        List<Map.Entry<String, Integer>> list = new ArrayList<>(weights.entrySet());
        list.sort((a, b) -> Integer.compare(b.getValue(), a.getValue()));

        List<String> out = new ArrayList<>();
        for (int i = 0; i < list.size() && out.size() < n; i++) {
            out.add(list.get(i).getKey());
        }
        return out;
    }


    /**
     * Returns the MediaType with the highest accumulated weight.
     */
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


    /**
     * Derives an age restriction preference from the user's positive history.
     *
     * Current strategy: conservative -> use the smallest age restriction the user positively rated.
     * Meaning: do not recommend content that is "harsher" than what the user already liked.
     *
     * Note: An alternative (commented out) would be the maximum age restriction.
     */
    private Integer preferredAgeMax(List<Rating> history) {
        Integer best = null;

        for (Rating r : history) {
            if (r == null || r.getMediaId() == null) {
                continue;
            }

            MediaEntry m = media.findById(r.getMediaId()).orElse(null);
            if (m == null) {
                continue;
            }

            Integer age = m.getAgeRestriction();
            if (age == null) {
                continue;
            }

            // Conservative: pick the smallest age restriction in the (positive) history
            /*
            if (best == null || age < best) {
                best = age;
            }
            */

            // Alternative (more permissive): maximum age restriction
            if (best == null || age > best) {
                best = age;
            }


        }

        return best;
    }


    /**
     * Converts a MediaEntry domain object into a MediaResponse DTO.
     */
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
