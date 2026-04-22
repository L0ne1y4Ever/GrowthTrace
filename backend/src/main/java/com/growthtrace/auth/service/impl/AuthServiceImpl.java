package com.growthtrace.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.growthtrace.auth.dto.LoginRequest;
import com.growthtrace.auth.dto.LoginResponse;
import com.growthtrace.auth.dto.RegisterRequest;
import com.growthtrace.auth.dto.UserInfoVO;
import com.growthtrace.auth.entity.SysUser;
import com.growthtrace.auth.mapper.SysUserMapper;
import com.growthtrace.auth.service.AuthService;
import com.growthtrace.common.exception.BusinessException;
import com.growthtrace.common.result.ResultCode;
import com.growthtrace.common.security.JwtUtils;
import com.growthtrace.common.security.SecurityUserDetails;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @Value("${growthtrace.security.jwt.access-token-ttl-minutes:120}")
    private long accessTtlMinutes;

    @Override
    @Transactional
    public void register(RegisterRequest request) {
        boolean usernameTaken = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername())) > 0;
        if (usernameTaken) {
            throw new BusinessException(ResultCode.CONFLICT, "用户名已被使用");
        }
        if (StringUtils.hasText(request.getEmail())) {
            boolean emailTaken = sysUserMapper.selectCount(new LambdaQueryWrapper<SysUser>()
                    .eq(SysUser::getEmail, request.getEmail())) > 0;
            if (emailTaken) {
                throw new BusinessException(ResultCode.CONFLICT, "邮箱已被使用");
            }
        }

        SysUser u = new SysUser();
        u.setUsername(request.getUsername());
        u.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        u.setEmail(request.getEmail());
        u.setNickname(StringUtils.hasText(request.getNickname()) ? request.getNickname() : request.getUsername());
        u.setStatus("ACTIVE");
        sysUserMapper.insert(u);
        log.info("用户注册成功 id={}, username={}", u.getId(), u.getUsername());
    }

    @Override
    public LoginResponse login(LoginRequest request) {
        SysUser user = sysUserMapper.selectOne(new LambdaQueryWrapper<SysUser>()
                .eq(SysUser::getUsername, request.getUsername()));
        if (user == null) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }
        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new BusinessException(ResultCode.FORBIDDEN, "账号已被锁定或禁用");
        }
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new BusinessException(ResultCode.UNAUTHORIZED, "用户名或密码错误");
        }

        user.setLastLoginAt(LocalDateTime.now());
        sysUserMapper.updateById(user);

        String token = jwtUtils.generateAccessToken(user.getId(), user.getUsername());
        return LoginResponse.builder()
                .accessToken(token)
                .expiresInSeconds(accessTtlMinutes * 60)
                .user(toVO(user))
                .build();
    }

    @Override
    public UserInfoVO currentUser() {
        Long userId = SecurityUserDetails.requireCurrentUserId();
        SysUser user = sysUserMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.NOT_FOUND, "用户不存在");
        }
        return toVO(user);
    }

    private UserInfoVO toVO(SysUser u) {
        return UserInfoVO.builder()
                .id(u.getId())
                .username(u.getUsername())
                .nickname(u.getNickname())
                .email(u.getEmail())
                .avatarUrl(u.getAvatarUrl())
                .build();
    }
}
