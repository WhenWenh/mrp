package mrp.application;

import mrp.domain.model.LeaderboardEntry;
import mrp.domain.ports.UserRepository;

import java.util.List;

public class LeaderboardService {

    private UserRepository userRepo;

    public LeaderboardService(UserRepository userRepo) {
        if (userRepo == null) throw new IllegalArgumentException("userRepo null");
        this.userRepo = userRepo;
    }

    public List<LeaderboardEntry> getLeaderboard(int limit, int offset) {
        if (limit < 0) throw new IllegalArgumentException("limit < 0");
        if (offset < 0) throw new IllegalArgumentException("offset < 0");
        return userRepo.leaderboardByRatings(limit, offset);
    }
}
