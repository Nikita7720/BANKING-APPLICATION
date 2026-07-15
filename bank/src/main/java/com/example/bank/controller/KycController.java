package com.example.bank.controller;


import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.repository.AccountOpeningRepository;
import com.example.bank.service.EmailService;

import jakarta.servlet.http.HttpSession;


@Controller
public class KycController {

    @GetMapping("/customer-dashboard/ekyc")
    public String getekycMethod() {
        return "ekyc";
    }
    

    @Autowired
    private AccountOpeningRepository accountRepository;

    @Autowired
    private EmailService emailService;   // 🔥 IMPORTANT

    // SEND OTP
    @PostMapping("/sendOtp")
    public String sendOtp(String accountNumber,
                          String pan,
                          HttpSession session,
                          Model model){

        List<AccountOpeningEntity> users = accountRepository.findByAccountNumberAndPan(accountNumber, pan);

        if(users.isEmpty()){
            model.addAttribute("msg","Invalid Account Number or PAN ❌");
            return "ekyc";
        }
        model.addAttribute("accountNumber", accountNumber);
        model.addAttribute("pan", pan);

        AccountOpeningEntity user = users.get(0); // first record

        // OTP generate
        String otp = String.valueOf((int)(Math.random()*900000)+100000);

        session.setAttribute("otp", otp);
        session.setAttribute("userId", user.getId());

    // email send
    emailService.sendEmail(
            user.getEmail(),
            "Your OTP",
            "Your KYC OTP is: " + otp
    );

    model.addAttribute("msg","OTP sent to email");

    return "ekyc";
}

    // VERIFY OTP
    @PostMapping("/verifyOtp")
public String verifyOtp(String otp,
                        HttpSession session,
                        Model model){

    String sessionOtp = (String) session.getAttribute("otp");
    Long userId = (Long) session.getAttribute("userId");

    if(sessionOtp == null || !sessionOtp.equals(otp)){
        model.addAttribute("msg","Invalid OTP ❌");
        return "ekyc";
    }

    AccountOpeningEntity user =
        accountRepository.findById(userId).orElse(null);

    user.setKycStatus("Verified");

    accountRepository.save(user);

    model.addAttribute("msg","KYC Verified Successfully ✅");

    return "ekyc";
}
}