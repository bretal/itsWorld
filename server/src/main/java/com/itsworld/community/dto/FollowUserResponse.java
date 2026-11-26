package com.itsworld.community.dto;

import lombok.Data;

@Data
public class FollowUserResponse {
    private Long id;
    private String nickName;
    private String avatarUrl;
    private String bio;
    private Boolean isFollowing;
    private Boolean followsMe;
}
