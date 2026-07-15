package com.example.bank.controller;

import com.example.bank.entity.QueryEntity;
import com.example.bank.entity.User;
import com.example.bank.repository.QueryRepository;
import com.example.bank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;
import java.util.List;

@Controller
public class SupportController {

    @Autowired
    private QueryRepository queryRepo;

    @Autowired
    private UserRepository userRepo;

    @GetMapping("/help")
    public String showHelpPage(HttpSession session, Model model) {
        if (session.getAttribute("userId") == null) return "redirect:/login_customer";
        
        Long userId = (Long) session.getAttribute("userId");
        User user = userRepo.findById(userId).orElse(null);
        model.addAttribute("user", user);
        
        List<QueryEntity> myQueries = queryRepo.findByCustomerId(userId);
        model.addAttribute("myQueries", myQueries);
        
        return "help";
    }

    @PostMapping("/submit-query")
    public String submitQuery(String subject, String message, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("userId") == null) return "redirect:/login_customer";
        
        Long userId = (Long) session.getAttribute("userId");
        User user = userRepo.findById(userId).orElse(null);
        
        if (user != null) {
            QueryEntity query = new QueryEntity();
            query.setCustomerId(userId);
            query.setCustomerName(user.getFull_name());
            query.setEmail(user.getEmail());
            query.setSubject(subject);
            query.setMessage(message);
            queryRepo.save(query);
            
            redirectAttributes.addFlashAttribute("success", "Your query has been submitted successfully! We will get back to you soon.");
        }
        
        return "redirect:/help";
    }

    // Staff view for queries
    @GetMapping("/staff/queries")
    public String viewQueries(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null) return "redirect:/staff-login";
        
        List<QueryEntity> allQueries = queryRepo.findAll();
        allQueries.sort((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        model.addAttribute("queries", allQueries);
        
        return "staff-queries";
    }

    @PostMapping("/staff/resolve-query")
    public String resolveQuery(Long id, HttpSession session) {
        if (session.getAttribute("staffId") == null) return "redirect:/staff-login";
        
        QueryEntity query = queryRepo.findById(id).orElse(null);
        if (query != null) {
            query.setStatus("RESOLVED");
            queryRepo.save(query);
        }
        
        return "redirect:/staff/queries";
    }
}
