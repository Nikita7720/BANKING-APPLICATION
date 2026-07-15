package com.example.bank.controller;


import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

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
    private EmailService emailService;   // 🔥 IMPORTANT

    // SEND OTP
    @PostMapping("/send_Otp")
public String sendOtp(String email,
                      String password,
                      HttpSession session,
                      Model model){

    // 🔥 इथे change करायचा
List<User> users = UserRepo.findByEmailAndPassword(email, password);

    System.out.println("Email: " + email); // debug


    if(users.isEmpty()){
        model.addAttribute("msg","Invalid PAN/Aadhaar ❌");
        return "ekyc";
    }
    model.addAttribute("email", email);
    model.addAttribute("password", password);

    

    User    user = users.get(0); // first record

    // OTP generate
    String otp = String.valueOf((int)(Math.random()*900000)+100000);


    session.setAttribute("otp", otp);
    session.setAttribute("userId", user.getId());
    session.setAttribute("email", email);

    // 🔥 SAVE OTP IN DB


    // email send
    emailService.sendEmail(
            user.getEmail(),
            "Your OTP",
            "Your Login OTP is: " + otp
    );

    model.addAttribute("msg","OTP sent to email");

    return "login_customer";
}



    // VERIFY OTP
    
    

  

//private OtpRepository otpRepository;
    @PostMapping("/verify_Otp")
public String verifyOtp(@RequestParam String otp,
                        HttpSession session,
                        Model model){

    String sessionOtp = (String) session.getAttribute("otp");
    Long userId = (Long) session.getAttribute("userId");

    if(sessionOtp == null || !sessionOtp.equals(otp)){
        model.addAttribute("msg","Invalid OTP ❌");
        return "login_customer";
    }

    User user =
        UserRepo.findById(userId).orElse(null);
    session.setAttribute("userId", user.getId());


    user.setOtpStatus("Verified");

    UserRepo.save(user);
    model.addAttribute("app", user);


    model.addAttribute("msg","Login Verified Successfully ✅");
    

    return "New_customer_login";
}

    // AJAX Endpoint for SEND OTP
    @PostMapping("/api/send-otp")
    @ResponseBody
    public ResponseEntity<?> sendOtpApi(@RequestBody Map<String, String> payload, HttpSession session) {
        String email = payload.get("email");
        String password = payload.get("password");
        
        List<User> users = UserRepo.findByEmailAndPassword(email, password);
        
        if(users.isEmpty()){
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid Email or Password ❌");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = users.get(0);
        String otp = String.valueOf((int)(Math.random()*900000)+100000);
        
        session.setAttribute("otp", otp);
        session.setAttribute("userId", user.getId());
        session.setAttribute("email", email);
        
        emailService.sendEmail(user.getEmail(), "Your OTP", "Your Login OTP is: " + otp);
        
        Map<String, String> response = new HashMap<>();
        response.put("message", "OTP sent to email");
        return ResponseEntity.ok(response);
    }

    // AJAX Endpoint for VERIFY OTP
    @PostMapping("/api/verify-otp")
    @ResponseBody
    public ResponseEntity<?> verifyOtpApi(@RequestBody Map<String, String> payload, HttpSession session) {
        String otp = payload.get("otp");
        String sessionOtp = (String) session.getAttribute("otp");
        Long userId = (Long) session.getAttribute("userId");
        
        if(sessionOtp == null || !sessionOtp.equals(otp)){
            Map<String, String> response = new HashMap<>();
            response.put("error", "Invalid OTP ❌");
            return ResponseEntity.badRequest().body(response);
        }
        
        User user = UserRepo.findById(userId).orElse(null);
        if (user != null) {
            user.setOtpStatus("Verified");
            UserRepo.save(user);
            session.setAttribute("userId", user.getId());
            
            Map<String, String> response = new HashMap<>();
            response.put("message", "Login Verified Successfully ✅");
            response.put("redirect", "/New_customer_login");
            return ResponseEntity.ok(response);
        }
        
        Map<String, String> response = new HashMap<>();
        response.put("error", "User not found");
        return ResponseEntity.badRequest().body(response);
    }
}
