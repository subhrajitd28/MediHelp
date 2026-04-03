package com.medihelp.auth.service;

import com.medihelp.auth.dto.*;
import com.medihelp.auth.entity.RefreshToken;
import com.medihelp.auth.entity.UserAuth;
import com.medihelp.auth.repository.RefreshTokenRepository;
import com.medihelp.auth.repository.UserAuthRepository;
import com.medihelp.common.event.RabbitMQConfig;
import com.medihelp.common.event.UserRegisteredEvent;
import com.medihelp.common.exception.BadRequestException;
import com.medihelp.common.exception.UnauthorizedException;
import com.medihelp.common.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAuthRepository userAuthRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final RabbitTemplate rabbitTemplate;
    private final StringRedisTemplate redisTemplate;

    private static final long OTP_VALIDITY_MINUTES = 5;
    private static final long REFRESH_TOKEN_DAYS = 60;

    @Transactional
    public String register(RegisterRequest request) {
        if (userAuthRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Email already registered");
        }

        if (request.getPhone() != null && userAuthRepository.existsByPhone(request.getPhone())) {
            throw new BadRequestException("Phone number already registered");
        }

        String otp = otpService.generateOtp();

        UserAuth user = UserAuth.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .otpCode(otpService.hashOtp(otp))
                .otpExpiry(Instant.now().plus(Duration.ofMinutes(OTP_VALIDITY_MINUTES)))
                .isVerified(false)
                .isActive(true)
                .role("USER")
                .build();

        userAuthRepository.save(user);

        // TODO: Send OTP via email (Resend integration in Notification Service)
        log.info("OTP generated for {}: {} (send via email in production)", request.getEmail(), otp);

        return "Registration successful. Please verify your email with the OTP sent.";
    }

    @Transactional
    public String verifyOtp(OtpVerifyRequest request) {
        UserAuth user = userAuthRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.isVerified()) {
            throw new BadRequestException("Email already verified");
        }

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("OTP has expired. Please request a new one.");
        }

        if (!otpService.verifyOtp(request.getOtp(), user.getOtpCode())) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setVerified(true);
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userAuthRepository.save(user);

        // Publish user registered event
        UserRegisteredEvent event = UserRegisteredEvent.builder()
                .userId(user.getId().toString())
                .email(user.getEmail())
                .registeredAt(Instant.now())
                .build();

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE,
                RabbitMQConfig.RK_USER_REGISTERED,
                event
        );

        log.info("User verified and registered event published for: {}", user.getEmail());
        return "Email verified successfully. You can now log in.";
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        UserAuth user = userAuthRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (!user.isActive()) {
            throw new UnauthorizedException("Account is deactivated");
        }

        if (!user.isVerified()) {
            throw new UnauthorizedException("Please verify your email before logging in");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("Invalid email or password");
        }

        String accessToken = jwtUtil.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(user.getId().toString());

        // Store hashed refresh token
        RefreshToken tokenEntity = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(passwordEncoder.encode(refreshToken))
                .expiresAt(Instant.now().plus(Duration.ofDays(REFRESH_TOKEN_DAYS)))
                .build();
        refreshTokenRepository.save(tokenEntity);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .userId(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresIn(900) // 15 minutes in seconds
                .build();
    }

    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String refreshToken = request.getRefreshToken();

        if (!jwtUtil.isTokenValid(refreshToken)) {
            throw new UnauthorizedException("Invalid or expired refresh token");
        }

        String userId = jwtUtil.getUserId(refreshToken);

        UserAuth user = userAuthRepository.findById(java.util.UUID.fromString(userId))
                .orElseThrow(() -> new UnauthorizedException("User not found"));

        String newAccessToken = jwtUtil.generateAccessToken(
                user.getId().toString(), user.getEmail(), user.getRole());

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(refreshToken) // Return the same refresh token
                .userId(user.getId().toString())
                .email(user.getEmail())
                .role(user.getRole())
                .expiresIn(900)
                .build();
    }

    public void logout(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return;
        }

        String token = authorizationHeader.substring(7);

        try {
            String jti = jwtUtil.getTokenId(token);
            long remainingMs = jwtUtil.getRemainingExpirationMs(token);

            if (remainingMs > 0) {
                redisTemplate.opsForValue().set(
                        "blacklist:" + jti,
                        "true",
                        remainingMs,
                        TimeUnit.MILLISECONDS
                );
            }

            log.info("Token blacklisted: {}", jti);
        } catch (Exception e) {
            log.warn("Failed to blacklist token: {}", e.getMessage());
        }
    }

    @Transactional
    public String forgotPassword(String email) {
        UserAuth user = userAuthRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("User not found"));

        String otp = otpService.generateOtp();
        user.setOtpCode(otpService.hashOtp(otp));
        user.setOtpExpiry(Instant.now().plus(Duration.ofMinutes(OTP_VALIDITY_MINUTES)));
        userAuthRepository.save(user);

        // TODO: Send OTP via email
        log.info("Password reset OTP for {}: {} (send via email in production)", email, otp);

        return "Password reset OTP sent to your email.";
    }

    @Transactional
    public String resetPassword(PasswordResetRequest request) {
        UserAuth user = userAuthRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("User not found"));

        if (user.getOtpExpiry() == null || user.getOtpExpiry().isBefore(Instant.now())) {
            throw new BadRequestException("OTP has expired");
        }

        if (!otpService.verifyOtp(request.getOtp(), user.getOtpCode())) {
            throw new BadRequestException("Invalid OTP");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setOtpCode(null);
        user.setOtpExpiry(null);
        userAuthRepository.save(user);

        return "Password reset successfully.";
    }
}
