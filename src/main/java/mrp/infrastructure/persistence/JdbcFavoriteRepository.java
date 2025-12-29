package mrp.infrastructure.persistence;

import mrp.domain.ports.FavoriteRepository;
import mrp.infrastructure.config.ConnectionFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JdbcFavoriteRepository implements FavoriteRepository {

    @Override
    public boolean add(UUID userId, UUID mediaId){
        String sql = "INSERT INTO favorites (user_id, media_id) VALUES (?, ?) ON CONFLICT DO NOTHING";
        try(Connection c = ConnectionFactory.get()){
            PreparedStatement ps = c.prepareStatement(sql);
            ps.setObject(1, userId);
            ps.setObject(2, mediaId);
            return ps.executeUpdate() == 1;
        }catch(SQLException e){
            throw new RuntimeException("add favorite failed", e);
        }
    }

    @Override
    public boolean remove(UUID userId, UUID mediaId) {
        String sql = "DELETE FROM favorites WHERE user_id=? AND media_id=?";
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            ps.setObject(2, mediaId);
            return ps.executeUpdate() == 1;
        } catch (SQLException e) {
            throw new RuntimeException("remove favorite failed", e);
        }
    }

    @Override
    public List<UUID> listMediaIdsByUser(UUID userId) {
        String sql = "SELECT media_id FROM favorites WHERE user_id=? ORDER BY created_at DESC";
        List<UUID> out = new ArrayList<>();
        try (Connection c = ConnectionFactory.get();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setObject(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) out.add((UUID) rs.getObject("media_id"));
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("list favorites failed", e);
        }
    }
}
