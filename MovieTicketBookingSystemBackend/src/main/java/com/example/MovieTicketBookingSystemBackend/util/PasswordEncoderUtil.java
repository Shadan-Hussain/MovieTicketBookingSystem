package com.example.MovieTicketBookingSystemBackend.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * Run locally to encode a password with BCrypt (same as the app uses for storage).
 * Run from IDE; set program arguments to the password to encode, or leave empty to encode "password".
 */
public class PasswordEncoderUtil {

    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "password";
        String hash = new BCryptPasswordEncoder().encode(password);
        System.out.println("Password: " + password);
        System.out.println("BCrypt hash: " + hash);
    }
}
