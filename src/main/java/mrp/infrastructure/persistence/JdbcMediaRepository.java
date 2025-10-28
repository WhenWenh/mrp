package mrp.infrastructure.persistence;

import mrp.domain.model.MediaEntry;
import mrp.domain.model.enums.MediaType;
import mrp.domain.ports.MediaRepository;
import mrp.domain.ports.MediaSearch;
import mrp.infrastructure.config.ConnectionFactory;

import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class JdbcMediaRepository implements MediaRepository {


    public JdbcMediaRepository() {
    }

    @Override
    public MediaEntry save(MediaEntry e) {
        String sql = "INSERT INTO media_entries " +
                "(id, creator_id, title, description, media_type, release_year, genres, age_restriction, average_score, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, e.getId());
            ps.setObject(2, e.getCreatorId());
            ps.setString(3, e.getTitle());
            ps.setString(4, e.getDescription());
            ps.setString(5, e.getMediaType() != null ? e.getMediaType().name() : null);

            if (e.getReleaseYear() == null){
                ps.setNull(6, Types.INTEGER);
            } else{
                ps.setInt(6, e.getReleaseYear());
            }

            if (e.getGenres() == null) {
                ps.setNull(7, Types.ARRAY);
            } else {
                ps.setArray(7, c.createArrayOf("text", e.getGenres().toArray(new Object[0])));
            }

            if (e.getAgeRestriction() == null){
                ps.setNull(8, Types.INTEGER);
            } else {
                ps.setInt(8, e.getAgeRestriction());
            }

            if (e.getAverageScore() == null){
                ps.setNull(9, Types.DOUBLE);
            } else {
                ps.setDouble(9, e.getAverageScore());
            }

            ps.setTimestamp(10, Timestamp.from(e.getCreatedAt() != null ? e.getCreatedAt() : Instant.now()));
            ps.setTimestamp(11, Timestamp.from(e.getUpdatedAt() != null ? e.getUpdatedAt() : Instant.now()));

            ps.executeUpdate();
            return e;
        } catch (SQLException ex) {
            throw new RuntimeException("save media failed", ex);
        }
    }

    @Override
    public Optional<MediaEntry> findById(UUID id) {
        String sql = "SELECT * FROM media_entries WHERE id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()){
                    return Optional.empty();
                }
                return Optional.of(map(rs));
            }
        } catch (SQLException ex) {
            throw new RuntimeException("findById failed", ex);
        }
    }

    @Override
    public boolean update(MediaEntry e) {
        String sql = "UPDATE media_entries " +
                "SET title=?, description=?, media_type=?, release_year=?, genres=?, age_restriction=?, average_score=?, updated_at=? " +
                "WHERE id=?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setString(1, e.getTitle());
            ps.setString(2, e.getDescription());
            ps.setString(3, e.getMediaType() != null ? e.getMediaType().name() : null);

            if (e.getReleaseYear() == null){
                ps.setNull(4, Types.INTEGER);
            } else{
                ps.setInt(4, e.getReleaseYear());
            }

            if (e.getGenres() == null) {
                ps.setNull(5, Types.ARRAY);
            } else {
                ps.setArray(5, c.createArrayOf("text", e.getGenres().toArray(new Object[0])));
            }

            if (e.getAgeRestriction() == null){
                ps.setNull(6, Types.INTEGER);
            } else {
                ps.setInt(6, e.getAgeRestriction());
            }

            if (e.getAverageScore() == null){
                ps.setNull(7, Types.DOUBLE);
            } else {
                ps.setDouble(7, e.getAverageScore());
            }

            ps.setTimestamp(8, Timestamp.from(e.getUpdatedAt() != null ? e.getUpdatedAt() : Instant.now()));
            ps.setObject(9, e.getId());

            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RuntimeException("update failed", ex);
        }
    }

    @Override
    public boolean delete(UUID id) {
        String sql = "DELETE FROM media_entries WHERE id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, id);
            return ps.executeUpdate() == 1;
        } catch (SQLException ex) {
            throw new RuntimeException("delete failed", ex);
        }
    }

    // Wenn dein Interface bereits auf MediaSearch umgestellt ist:
    public List<MediaEntry> search(MediaSearch s) {
        StringBuilder sb = new StringBuilder("SELECT * FROM media_entries WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (s != null) {
            if (s.getQuery() != null && !s.getQuery().trim().isEmpty()) {
                sb.append(" AND title ILIKE ?");
                params.add("%" + s.getQuery().trim() + "%");
            }
            if (s.getMediaType() != null && !s.getMediaType().trim().isEmpty()) {
                sb.append(" AND media_type = ?");
                params.add(s.getMediaType().trim());
            }
            if (s.getYearFrom() != null) {
                sb.append(" AND release_year >= ?");
                params.add(s.getYearFrom());
            }
            if (s.getYearTo() != null) {
                sb.append(" AND release_year <= ?");
                params.add(s.getYearTo());
            }
            if (s.getAgeMax() != null) {
                sb.append(" AND (age_restriction IS NULL OR age_restriction <= ?)");
                params.add(s.getAgeMax());
            }
        }

        String sortBy = (s != null && s.getSortBy() != null) ? s.getSortBy().toLowerCase() : "created";
        String sortDir = (s != null && s.getSortDir() != null) ? s.getSortDir().toLowerCase() : "desc";

        String sortCol = "created_at";
        if ("title".equals(sortBy)){
            sortCol = "title";
        }
        else if ("year".equals(sortBy)) {
            sortCol = "release_year";
        }

        String dir = "DESC";
        if ("asc".equals(sortDir)){
            dir = "ASC";
        }

        int limit = (s != null ? s.getLimit() : 20);
        int offset = (s != null ? s.getOffset() : 0);
        if (limit <= 0){
            limit = 20;
        }
        if (offset < 0) {
            offset = 0;
        }

        sb.append(" ORDER BY ").append(sortCol).append(" ").append(dir);
        sb.append(" LIMIT ? OFFSET ?");
        params.add(limit);
        params.add(offset);

        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sb.toString())) {

            int i = 1;
            for (Object p : params) {
                if (p instanceof Integer) ps.setInt(i++, (Integer) p);
                else ps.setObject(i++, p);
            }

            List<MediaEntry> out = new ArrayList<>();
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add(map(rs));
            }
            return out;
        } catch (SQLException ex) {
            throw new RuntimeException("search failed", ex);
        }
    }

    @Override
    public boolean isOwner(UUID mediaId, UUID userId) {
        String sql = "SELECT 1 FROM media_entries WHERE id = ? AND creator_id = ?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {

            ps.setObject(1, mediaId);
            ps.setObject(2, userId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException ex) {
            throw new RuntimeException("isOwner failed", ex);
        }
    }

    private MediaEntry map(ResultSet rs) throws SQLException {
        UUID id = (UUID) rs.getObject("id");
        UUID creatorId = (UUID) rs.getObject("creator_id");
        String title = rs.getString("title");
        String description = rs.getString("description");
        String mt = rs.getString("media_type");
        Integer year = (Integer) rs.getObject("release_year");

        Array genresArr = rs.getArray("genres");
        List<String> genres = null;
        if (genresArr != null) {
            Object[] arr = (Object[]) genresArr.getArray();
            genres = new ArrayList<>();
            for (Object o : arr) genres.add((String) o);
        }

        Integer age = (Integer) rs.getObject("age_restriction");
        Double avg = (Double) rs.getObject("average_score");
        Instant created = rs.getTimestamp("created_at").toInstant();
        Instant updated = rs.getTimestamp("updated_at").toInstant();

        return new MediaEntry(
                id,
                creatorId,
                title,
                description,
                mt != null ? MediaType.valueOf(mt) : null,
                year,
                genres,
                age,          // FSK: wird 1:1 übernommen (nullable)
                avg,
                created,
                updated
        );
    }
}