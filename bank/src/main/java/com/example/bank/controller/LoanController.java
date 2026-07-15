package com.example.bank.controller;

import java.io.IOException;
import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.bank.entity.LoanRequest;
import com.example.bank.entity.User;
import com.example.bank.repository.LoanRequestRepository;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.EmailService;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoanController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private LoanRequestRepository loanRequestRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/apply-loan")
    public String applyLoanForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) {
            return "redirect:/login_customer";
        }
        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return "redirect:/login_customer";
        }
        model.addAttribute("user", user);
        return "apply-loan";
    }

    @PostMapping("/loan-send-otp")
    public String sendLoanOtp(
            @RequestParam String employmentType,
            @RequestParam Double monthlyIncome,
            @RequestParam Double amount,
            @RequestParam Integer duration,
            @RequestParam String purpose,
            @RequestParam("document") MultipartFile file,
            @RequestParam("aadharCardPhoto") MultipartFile aadharCardPhoto,
            @RequestParam("panCardPhoto") MultipartFile panCardPhoto,
            @RequestParam("userPhoto") MultipartFile userPhoto,
            @RequestParam("signature") MultipartFile signature,
            HttpSession session,
            RedirectAttributes redirectAttributes) throws IOException {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login_customer";

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return "redirect:/login_customer";

        // Eligibility Check
        if (amount > (monthlyIncome * 60)) {
            // Not eligible
            LoanRequest rejectedLoan = new LoanRequest();
            rejectedLoan.setCustomerId(user.getId());
            rejectedLoan.setAccountNumber(user.getAccountNumber());
            rejectedLoan.setPan(user.getPan());
            rejectedLoan.setAadhar(user.getAadhar());
            rejectedLoan.setEmploymentType(employmentType);
            rejectedLoan.setMonthlyIncome(monthlyIncome);
            rejectedLoan.setAmount(amount);
            rejectedLoan.setDurationMonths(duration);
            rejectedLoan.setPurpose(purpose);
            // Save basic fields, skip heavy images for automatic rejection to save DB space
            rejectedLoan.setStatus("REJECTED_AUTOMATICALLY");
            rejectedLoan.setRemark("Automatically rejected: Requested amount exceeds eligible limit.");
            rejectedLoan.setRequestDate(LocalDateTime.now());
            loanRequestRepository.save(rejectedLoan);

            redirectAttributes.addFlashAttribute("msg",
                    "Your loan application was automatically rejected: Requested amount exceeds eligible limit.");
            return "redirect:/apply-loan";
        }

        // Store data in session temporarily
        LoanRequest tempLoan = new LoanRequest();
        tempLoan.setCustomerId(user.getId());
        if (user.getAccounts() != null && !user.getAccounts().isEmpty()) {
            com.example.bank.entity.AccountOpeningEntity acc = user.getAccounts().get(0);
            tempLoan.setAccountNumber(acc.getAccountNumber());
            tempLoan.setPan(acc.getPan());
            tempLoan.setAadhar(acc.getAadhar());
        } else {
            tempLoan.setAccountNumber(user.getAccountNumber());
            tempLoan.setPan(user.getPan());
            tempLoan.setAadhar(user.getAadhar());
        }
        tempLoan.setEmploymentType(employmentType);
        tempLoan.setMonthlyIncome(monthlyIncome);
        tempLoan.setAmount(amount);
        tempLoan.setDurationMonths(duration);
        tempLoan.setPurpose(purpose);
        tempLoan.setDocument(file.getBytes());
        tempLoan.setAadharCardPhoto(aadharCardPhoto.getBytes());
        tempLoan.setPanCardPhoto(panCardPhoto.getBytes());
        tempLoan.setUserPhoto(userPhoto.getBytes());
        tempLoan.setSignature(signature.getBytes());
        tempLoan.setRequestDate(LocalDateTime.now());
        tempLoan.setStatus("PENDING_STAFF_REVIEW");

        session.setAttribute("tempLoan", tempLoan);

        // Generate OTP
        String otp = String.valueOf((int) (Math.random() * 900000) + 100000);
        session.setAttribute("loanOtp", otp);

        // Send Email
        try {
            emailService.sendEmail(
                    user.getEmail(),
                    "Loan Application OTP - SecureBank",
                    "Your OTP for Loan Application of ₹" + amount + " is: " + otp + "\nDo not share this with anyone.");
        } catch (Exception e) {
            System.out.println("Failed to send OTP email: " + e.getMessage());
        }

        return "apply-loan-otp";
    }

    @PostMapping("/loan-verify-otp")
    public String verifyLoanOtp(@RequestParam String otp, HttpSession session, RedirectAttributes redirectAttributes,
            Model model) {
        String sessionOtp = (String) session.getAttribute("loanOtp");
        LoanRequest tempLoan = (LoanRequest) session.getAttribute("tempLoan");

        if (sessionOtp == null || !sessionOtp.equals(otp) || tempLoan == null) {
            model.addAttribute("msg", "Invalid OTP. Please try again.");
            return "apply-loan-otp";
        }

        // OTP Verified, Save to Database
        loanRequestRepository.save(tempLoan);

        // Clear Session Data
        session.removeAttribute("loanOtp");
        session.removeAttribute("tempLoan");

        // Could also save a Notification here if desired

        redirectAttributes.addFlashAttribute("success",
                "Loan Application Submitted Successfully! Staff will verify your application soon.");
        // Redirect back to dashboard
        User user = userRepository.findById(tempLoan.getCustomerId()).orElse(null);
        if (user != null && user.getAccounts() != null && !user.getAccounts().isEmpty()) {
            return "redirect:/customer-dashboard/" + user.getAccounts().get(0).getId();
        }
        return "redirect:/login_customer";
    }

    @GetMapping("/loan-document/{id}/{type}")
    public org.springframework.http.ResponseEntity<byte[]> getLoanDocument(@PathVariable Long id,
            @PathVariable String type) {
        LoanRequest loan = loanRequestRepository.findById(id).orElse(null);
        if (loan == null)
            return org.springframework.http.ResponseEntity.notFound().build();

        byte[] data = new byte[0];
        switch (type) {
            case "document" -> data = loan.getDocument() != null ? loan.getDocument() : new byte[0];
            case "aadhar" -> data = loan.getAadharCardPhoto() != null ? loan.getAadharCardPhoto() : new byte[0];
            case "pan" -> data = loan.getPanCardPhoto() != null ? loan.getPanCardPhoto() : new byte[0];
            case "photo" -> data = loan.getUserPhoto() != null ? loan.getUserPhoto() : new byte[0];
            case "signature" -> data = loan.getSignature() != null ? loan.getSignature() : new byte[0];
        }

        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        if (data.length > 4 && data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46) {
            headers.setContentType(org.springframework.http.MediaType.APPLICATION_PDF);
        } else {
            headers.setContentType(org.springframework.http.MediaType.IMAGE_JPEG);
        }
        return new org.springframework.http.ResponseEntity<>(data, headers, org.springframework.http.HttpStatus.OK);
    }

}
