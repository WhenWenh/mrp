package mrp.domain.ports;

import mrp.domain.model.User;
import java.util.*;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(UUID id);
    Optional<User> findByEmail(String email);
}
