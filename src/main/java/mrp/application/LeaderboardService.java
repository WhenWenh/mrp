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
        return userRepo.leaderboardByRatings(limit, offset);
    }
}
