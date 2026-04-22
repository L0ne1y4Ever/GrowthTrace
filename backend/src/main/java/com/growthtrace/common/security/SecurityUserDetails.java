package com.growthtrace.common.security;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@Getter
@AllArgsConstructor
public class SecurityUserDetails implements UserDetails {

    private final Long userId;
    private final String username;
    private final String password;

    public static SecurityUserDetails of(Long userId, String username) {
        return new SecurityUserDetails(userId, username, null);
    }

    public static SecurityUserDetails withPassword(Long userId, String username, String password) {
        return new SecurityUserDetails(userId, username, password);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.emptyList();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }

    public static Long currentUserId() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof SecurityUserDetails sud)) {
            return null;
        }
        return sud.getUserId();
    }

    public static Long requireCurrentUserId() {
        Long id = currentUserId();
        if (id == null) {
            throw new org.springframework.security.access.AccessDeniedException("未登录");
        }
        return id;
    }
}
