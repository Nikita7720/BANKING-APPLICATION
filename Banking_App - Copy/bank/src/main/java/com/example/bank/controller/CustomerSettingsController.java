package com.example.bank.controller;

import com.example.bank.entity.User;
import com.example.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class CustomerSettingsController {

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/customer/change-password")
    public String showChangePassword(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) return "redirect:/login_customer";
        return "change-password";
    }

    @PostMapping("/customer/update-password")
    public String updatePassword(@RequestParam String currentPassword,
                                 @RequestParam String newPassword,
                                 @RequestParam String confirmPassword,
                                 HttpSession session,
                                 RedirectAttributes ra) {
        
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null) return "redirect:/login_customer";

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) return "redirect:/login_customer";

        // 1. Check current password
        if (!user.getPassword().equals(currentPassword)) {
            ra.addFlashAttribute("error", "Current password is incorrect! ❌");
            return "redirect:/customer/change-password";
        }

        // 2. Check if new passwords match
        if (!newPassword.equals(confirmPassword)) {
            ra.addFlashAttribute("error", "New passwords do not match! ❌");
            return "redirect:/customer/change-password";
        }

        // 3. Update password
        user.setPassword(newPassword);
        userRepository.save(user);

        ra.addFlashAttribute("success", "Password updated successfully! ✅");
        return "redirect:/customer/change-password";
    }
}
