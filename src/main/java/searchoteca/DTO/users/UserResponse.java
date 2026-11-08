package searchoteca.DTO.users;

import searchoteca.model.UserModel;

public record UserResponse(
        Long userid,
        String customId,
        String username,
        String completeName,
        String email,
        String roleCode,
        Boolean status) {

    public static UserResponse from(UserModel u){
        return new UserResponse(u.getUserId(), u.getCustomId(), u.getUsername(), u.getCompleteName(), u.getEmail(), u.getRole_code(),u.getStatus());
    }
}

