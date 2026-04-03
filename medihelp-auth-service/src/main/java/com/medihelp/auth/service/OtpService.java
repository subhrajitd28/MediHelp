package com.medihelp.auth.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;

@Service
public class OtpService {

    private static final SecureRandom RANDOM = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public OtpService(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String generateOtp() {
        int otp = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(otp);
    }

    public String hashOtp(String otp) {
        return passwordEncoder.encode(otp);
    }

    public boolean verifyOtp(String rawOtp, String hashedOtp) {
        return passwordEncoder.matches(rawOtp, hashedOtp);
    }
}
