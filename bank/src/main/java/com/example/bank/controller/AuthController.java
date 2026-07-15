package com.example.bank.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.bank.entity.User;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Transactional

@Controller
public class AuthController {

    @GetMapping("/login_customer")
    public String getekycMethod() {
        return "login_customer";
    }

    @GetMapping("/logout")
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/";
    }

    @Autowired
    private UserRepository UserRepo;

    @Autowired
    private EmailService emailService; 

    // SEND OTP
    @PostMapping("/send_Otp")
    public String sendOtp(@RequestParam String email,
                          @RequestParam String password,
                          HttpSession session,
                          Model model) {

    
        List<User> users = UserRepo.findByEmailAndPassword(email, password);

        System.out.println("Email: " + email); // debug

        if (users.isEmpty()) {
            model.addAttribute("msg", "Invalid Email or Password ❌");
            return "login_customer";
        }
        model.addAttribute("email", email);
        model.addAttribute("password", password);

        User user = users.get(0); // first record

        // OTP generate
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);

        session.setAttribute("otp", otp);
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", email);

        // 🔥 SAVE OTP IN DB

        // email send
        emailService.sendEmail(
                user.getEmail(),
                "Your OTP",
                "Your Login OTP is: " + otp);

        model.addAttribute("msg", "OTP sent to email");

        return "login_customer";
    }

    // VERIFY OTP

    // private OtpRepository otpRepository;
    @PostMapping("/verify_Otp")
    public String verifyOtp(@RequestParam("otp") String otp,
            HttpSession session,
            Model model) {

        String sessionOtp = (String) session.getAttribute("otp");
        Long userId = (Long) session.getAttribute("userId");

        if (sessionOtp == null || !sessionOtp.equals(otp)) {
            model.addAttribute("msg", "Invalid OTP ❌");
            return "login_customer";
        }

        User user = UserRepo.findById(userId).orElse(null);
        session.setAttribute("userId", user.getId());

        user.setOtpStatus("Verified");

        UserRepo.save(user);
        model.addAttribute("app", user);

        model.addAttribute("msg", "Login Verified Successfully ✅");

        return "customer-dashboard";
    }

}
