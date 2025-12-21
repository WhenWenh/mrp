package mrp.dto;

import java.util.UUID;

public class UserResponse {
    public UUID id;
    public String username;

    public UserResponse() { }

    public UserResponse(UUID id, String username) {
        this.id = id;
        this.username = username;
    }
}
