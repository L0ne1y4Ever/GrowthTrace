package com.growthtrace.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserInfoVO {

    private Long id;
    private String username;
    private String nickname;
    private String email;
    private String avatarUrl;
}
