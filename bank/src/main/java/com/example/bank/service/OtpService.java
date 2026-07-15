

/*package com.example.bank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class OtpService {

    @Autowired
    private JavaMailSender mailSender;

    // 🔥 OTP GENERATE
    public String generateOTP() {
        return String.valueOf((int)(Math.random() * 900000) + 100000);
    }

    // 🔥 EMAIL SEND
    public void sendOtp(String email, String otp) {

        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(email);
        message.setSubject("SecureBank Login OTP");
        message.setText("Your OTP is: " + otp + "\n\nValid for 2 minutes.");

        mailSender.send(message);
    }
}*/