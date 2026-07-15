package com.example.bank.controller;

import com.example.bank.entity.CardRequest;
import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.entity.Notification;
import com.example.bank.repository.CardRequestRepository;
import com.example.bank.repository.AccountOpeningRepository;
import com.example.bank.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Random;

@Controller
public class CardController {

    @Autowired
    private CardRequestRepository cardRepo;

    @Autowired
    private AccountOpeningRepository accRepo;

    @Autowired
    private NotificationRepository notifRepo;

    @GetMapping("/apply-card")
    public String showApplyCard(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) return "redirect:/login_customer";
        
        Long userId = (Long) session.getAttribute("userId");
        AccountOpeningEntity acc = accRepo.findByUserId(userId);
        
        List<CardRequest> myRequests = cardRepo.findByUserId(userId);
        model.addAttribute("myRequests", myRequests);
        model.addAttribute("acc", acc);
        
        return "apply-card";
    }

    @PostMapping("/submit-card-request")
    public String submitCardRequest(@RequestParam String cardType, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("userId") == null) return "redirect:/login_customer";
        
        Long userId = (Long) session.getAttribute("userId");
        AccountOpeningEntity acc = accRepo.findByUserId(userId);
        
        if (acc != null) {
            CardRequest request = new CardRequest();
            request.setUserId(userId);
            request.setCustomerName(acc.getFullname());
            request.setAccountNumber(acc.getAccountNumber());
            request.setCardType(cardType);
            cardRepo.save(request);
            
            ra.addFlashAttribute("success", cardType + " Card application submitted successfully!");
        }
        
        return "redirect:/apply-card";
    }

    // Staff Card Management
    @GetMapping("/staff/card-requests")
    public String viewCardRequests(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null) return "redirect:/staff-login";
        
        List<CardRequest> pending = cardRepo.findByStatus("PENDING");
        List<CardRequest> history = cardRepo.findAll().stream()
                .filter(c -> !"PENDING".equals(c.getStatus()))
                .collect(java.util.stream.Collectors.toList());
        
        model.addAttribute("pending", pending);
        model.addAttribute("history", history);
        return "staff-card-requests";
    }

    @PostMapping("/staff/approve-card/{id}")
    public String approveCard(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("staffId") == null) return "redirect:/staff-login";
        
        CardRequest req = cardRepo.findById(id).orElse(null);
        if (req != null) {
            Random rand = new Random();
            String cardNum = String.format("%04d %04d %04d %04d", 
                4000 + rand.nextInt(1000), rand.nextInt(10000), rand.nextInt(10000), rand.nextInt(10000));
            String cvv = String.format("%03d", rand.nextInt(1000));
            String expiry = "12/30";
            
            req.setStatus("APPROVED");
            req.setCardNumber(cardNum);
            req.setCvv(cvv);
            req.setExpiryDate(expiry);
            cardRepo.save(req);
            
            // Notification
            AccountOpeningEntity acc = accRepo.findByAccountNumber(req.getAccountNumber());
            if (acc != null && acc.getUser() != null) {
                Notification n = new Notification();
                n.setUser(acc.getUser());
                n.setMessage("Congratulations! Your " + req.getCardType() + " card application is approved. Card No: " + cardNum);
                n.setDateTime(LocalDateTime.now());
                notifRepo.save(n);
            }
            
            ra.addFlashAttribute("success", "Card Approved and Generated!");
        }
        return "redirect:/staff/card-requests";
    }

    @PostMapping("/staff/reject-card/{id}")
    public String rejectCard(@PathVariable Long id, HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("staffId") == null) return "redirect:/staff-login";
        
        CardRequest req = cardRepo.findById(id).orElse(null);
        if (req != null) {
            req.setStatus("REJECTED");
            cardRepo.save(req);
            ra.addFlashAttribute("error", "Card Application Rejected.");
        }
        return "redirect:/staff/card-requests";
    }
}
