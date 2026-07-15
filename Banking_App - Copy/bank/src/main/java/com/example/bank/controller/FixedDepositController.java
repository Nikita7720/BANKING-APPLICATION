package com.example.bank.controller;

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.entity.FixedDeposit;
import com.example.bank.entity.Notification;

import com.example.bank.entity.User;
import com.example.bank.repository.AccountOpeningRepository;
import com.example.bank.repository.FixedDepositRepository;
import com.example.bank.repository.NotificationRepository;
import com.example.bank.repository.UserRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
public class FixedDepositController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountOpeningRepository accountOpeningRepository;

    @Autowired
    private FixedDepositRepository fdRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    // Interest rate based on tenure
    private double getInterestRate(int tenureMonths) {
        if (tenureMonths <= 3)
            return 5.5;
        if (tenureMonths <= 6)
            return 6.5;
        if (tenureMonths <= 12)
            return 7.0;
        if (tenureMonths <= 24)
            return 7.5;
        if (tenureMonths <= 36)
            return 7.75;
        return 7.90; // 60+ months
    }

    @GetMapping("/apply-fd")
    public String applyFdForm(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login_customer";

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return "redirect:/login_customer";

        AccountOpeningEntity app = null;
        if (user.getAccountNumber() != null) {
            app = accountOpeningRepository.findByAccountNumber(user.getAccountNumber());
        }

        model.addAttribute("user", user);
        model.addAttribute("app", app);
        return "apply-fd";
    }

    @PostMapping("/apply-fd")
    public String submitFd(
            @RequestParam Double amount,
            @RequestParam Integer tenureMonths,
            @RequestParam(required = false) String nomineeName,
            @RequestParam(required = false) String nomineeRelation,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login_customer";

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return "redirect:/login_customer";

        // Validate minimum amount
        if (amount < 1000) {
            redirectAttributes.addFlashAttribute("error", "Minimum FD amount is ₹1,000.");
            return "redirect:/apply-fd";
        }

        // Check balance (just verify, don't deduct yet)
        AccountOpeningEntity acc = accountOpeningRepository.findByAccountNumber(user.getAccountNumber());
        if (acc == null || acc.getBalance() < amount) {
            redirectAttributes.addFlashAttribute("error", "Insufficient balance to apply for Fixed Deposit.");
            return "redirect:/apply-fd";
        }

        // Calculate interest & maturity
        double interestRate = getInterestRate(tenureMonths);
        double interestAmount = (amount * interestRate * tenureMonths) / (12 * 100);
        double maturityAmount = Math.round((amount + interestAmount) * 100.0) / 100.0;

        LocalDate startDate = LocalDate.now();
        LocalDate maturityDate = startDate.plusMonths(tenureMonths);

        // Create FD with PENDING_STAFF_REVIEW status (no balance deduction yet)
        FixedDeposit fd = new FixedDeposit();
        fd.setCustomerId(userId);
        fd.setAccountNumber(user.getAccountNumber());
        fd.setFdNumber("FD" + System.currentTimeMillis());
        fd.setPrincipalAmount(amount);
        fd.setTenureMonths(tenureMonths);
        fd.setInterestRate(interestRate);
        fd.setMaturityAmount(maturityAmount);
        fd.setStartDate(startDate);
        fd.setMaturityDate(maturityDate);
        fd.setStatus("PENDING_STAFF_REVIEW");
        fd.setCreatedAt(LocalDateTime.now());
        fd.setNomineeName(nomineeName);
        fd.setNomineeRelation(nomineeRelation);
        fdRepository.save(fd);

        // Notification to customer
        Notification notif = new Notification();
        notif.setMessage("Your Fixed Deposit application of ₹" + amount + " for " + tenureMonths
                + " months has been submitted. Awaiting staff verification.");
        notif.setDateTime(LocalDateTime.now());
        notif.setUser(user);
        notificationRepository.save(notif);

        redirectAttributes.addFlashAttribute("success",
                "FD Application submitted! Staff will verify your application soon.");
        return "redirect:/customer-dashboard/" + acc.getId();
    }

    @GetMapping("/customer/fd-history")
    public String fdHistory(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId == null)
            return "redirect:/login_customer";

        User user = userRepository.findById(userId).orElse(null);
        if (user == null)
            return "redirect:/login_customer";

        List<FixedDeposit> myFds = fdRepository.findByCustomerId(userId);
        myFds.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        model.addAttribute("myFds", myFds);

        if (user.getAccountNumber() != null) {
            AccountOpeningEntity app = accountOpeningRepository.findByAccountNumber(user.getAccountNumber());
            model.addAttribute("app", app);
        }
        return "customer-fd-history";
    }
}
