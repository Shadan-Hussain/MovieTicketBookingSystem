package com.example.MovieTicketBookingSystemBackend;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/** One-off: run to print BCrypt hash for a password (same as app uses). */
public class PasswordHashUtil {

    public static void main(String[] args) {
        String password = args.length > 0 ? args[0] : "Shadan@2002";
        String hash = new BCryptPasswordEncoder().encode(password);
        System.out.println(hash);
    }
}
