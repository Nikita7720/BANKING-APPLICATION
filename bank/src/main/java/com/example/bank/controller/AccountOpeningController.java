package com.example.bank.controller;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.entity.Transaction;
import com.example.bank.entity.User;
import com.example.bank.repository.AccountOpeningRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.repository.UserRepository;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

@Controller
public class AccountOpeningController {

    @GetMapping("/customer-dashboard")
    public String dashboardDefault(Model model) {

        // example: always load id=1
        AccountOpeningEntity app = repo.findById(1L).orElse(null);

        model.addAttribute("app", app);

        return "customer-dashboard";
    }

    @GetMapping("/AccountOpening")
    public String firstPage() {
        return "AccountOpening";
    }

    @GetMapping("/Existinglogin")
    public String existinglogin() {
        return "/Existinglogin";
    }

    @Autowired
    private UserRepository UserRepo;

    @PostMapping("/Existinglogin")
    public String accountLogin(@RequestParam String accountNumber,
            @RequestParam String ifscCode,
            @RequestParam String password,
            HttpSession session,
            Model model,
            org.springframework.web.servlet.mvc.support.RedirectAttributes redirectAttributes) {

        AccountOpeningEntity acc = accountRepo.findByAccountNumberAndIfscCode(accountNumber, ifscCode);

        if (acc == null) {
            model.addAttribute("msg", "Invalid Account ❌");
            return "Existinglogin";
        }

        User user = acc.getUser();

        if (user == null || !password.equals(user.getPassword())) {
            model.addAttribute("msg", "Wrong Password ❌");
            return "Existinglogin";
        }

        session.setAttribute("userId", user.getId());

        // Redirect to the dashboard with the account ID
        return "redirect:/customer-dashboard/" + acc.getId();
    }

    @GetMapping("/Register_customer")
    public String getMethodName() {
        return "Register_customer";
    }

    @PostMapping("/saveRegisterUser")
    public String saveUser(User user, Model model) {

        UserRepo.save(user); // 🔥 MAIN LINE

        model.addAttribute("msg", "Registered Successfully");

        return "login_customer";
    }

    @Autowired
    private com.example.bank.repository.NotificationRepository notificationRepo;

    // Customer Dashboard
    @GetMapping("/customer-dashboard/{id:\\d+}")
    public String dashboard(
            @PathVariable Long id,
            Model model) {

        AccountOpeningEntity app = repo.findById(id).orElse(null);

        if (app == null) {
            return "redirect:/staff";
        }

        model.addAttribute("app", app);

        // Fetch notifications
        if (app.getUser() != null) {
            java.util.List<com.example.bank.entity.Notification> notifications = notificationRepo
                    .findByUserIdOrderByDateTimeDesc(app.getUser().getId());
            model.addAttribute("notifications", notifications);
            model.addAttribute("notifCount", notifications.stream().filter(n -> !n.isRead()).count());

            // Fetch Loans
            java.util.List<com.example.bank.entity.LoanRequest> myLoans = org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(jakarta.servlet.http.HttpServletRequest.class
                            .cast(org.springframework.web.context.request.RequestContextHolder
                                    .currentRequestAttributes().resolveReference(
                                            org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST))
                            .getServletContext())
                    .getBean(com.example.bank.repository.LoanRequestRepository.class)
                    .findByCustomerId(app.getUser().getId());
            model.addAttribute("myLoans", myLoans);
        }

        return "customer-dashboard";
    }

    @Autowired
    AccountOpeningRepository repo;

    /**
     * Handles the POST request from the account‑opening form.
     * After persisting the entity, redirects to a dedicated success page
     * so the user gets clear feedback.
     */
    @PostMapping("/saveUser")
    public String saveAccount(@ModelAttribute AccountOpeningEntity account,
            @RequestParam(value = "file", required = false) MultipartFile file,
            HttpSession session, Model model) throws Exception {

        // --- Process optional photo upload ------------------------------------
        if (file != null && !file.isEmpty()) {
            account.setPhoto(file.getBytes());
        }

        // Link the account to the logged‑in user (saved in session)
        Long userId = (Long) session.getAttribute("userId");
        User user = (userId != null) ? UserRepo.findById(userId).orElse(null) : null;
        account.setUser(user);

        repo.save(account);

        // Redirect to a success view rather than staying on the form page
        return "redirect:/account_success";
    }

    // Staff Dashboard
    @GetMapping("/staff")
    public String staffDashboard(Model model) {

        model.addAttribute("apps", repo.findAll());

        return "staff";
    }

    // Approve Account
    /*
     * @GetMapping("/approve/{id}")
     * public String approve(@PathVariable Long id) {
     * 
     * AccountOpeningEntity a = repo.findById(id).get();
     * 
     * a.setStatus("Approved");
     * 
     * 
     * // Random Account Number
     * String accountNo = String.valueOf(
     * 100000000000L +
     * (long)(Math.random() * 900000000000L)
     * );
     * 
     * a.setAccountNumber(accountNo);
     * 
     * a.setBalance(0.0);
     * 
     * 
     * // Customer ID
     * String customerId = "CUST" +
     * (10000 + (int)(Math.random()*90000));
     * 
     * a.setCustomerId(customerId);
     * 
     * String ifscCode = "IFSC" +(10000 + (int)(Math.random()*90000));
     * 
     * a.setIfscCode(ifscCode);
     * 
     * 
     * 
     * // Branch
     * a.setBranchName("Pune Main Branch");
     * 
     * repo.save(a);
     * 
     * return "redirect:/staff";
     * 
     * 
     * 
     * }
     */
    // The old approve method is disabled because it created duplicate User rows.
    // Use /staff/approve-account/{id} in StaffController instead.
    /*
     * @GetMapping("/approve/{id}")
     * public String approve(@PathVariable Long id) {
     * // ... disabled ...
     * return "redirect:/staff";
     * }
     */

    // Reject Application
    @GetMapping("/reject/{id}")
    public String reject(@PathVariable Long id) {

        AccountOpeningEntity a = repo.findById(id).get();

        a.setStatus("Rejected");

        repo.save(a);

        return "redirect:/staff";
    }

    // View Form
    @GetMapping("/view_form/{id}")
    public String viewApplication(@PathVariable Long id, Model model) {

        AccountOpeningEntity app = repo.findById(id).get();

        model.addAttribute("app", app);

        return "view_form";
    }

    @GetMapping("/customer-dashboard/deposit")
    public String showDepositPage() {
        return "deposit";
    }

    @Autowired
    private AccountOpeningRepository accountRepository;

    @Autowired

    private TransactionRepository transactionRepository;

    @PostMapping("/saveDeposit")
    public String deposit(@RequestParam String accountNumber,
            @RequestParam Double amount,
            Model model) {

        AccountOpeningEntity acc = accountRepository.findByAccountNumber(accountNumber.trim());

        // ❌ Account not found
        if (acc == null) {

            model.addAttribute("msg", "Invalid Account ❌");
            model.addAttribute("type", "error");

            return "deposit";
        }

        // ❌ Invalid amount
        if (amount == null || amount <= 0) {
            model.addAttribute("msg", "Enter valid amount");
            return "deposit";
        }

        // ✅ NULL safety (important)
        if (acc.getBalance() == null) {
            acc.setBalance(0.0);
        }

        // ✅ Update balance
        Double newBalance = acc.getBalance() + amount;
        acc.setBalance(newBalance);

        
        Transaction t = new Transaction();
        t.setAccountNumber(acc.getAccountNumber());
        t.setAmount(amount);
        t.setType("DEPOSIT");
        t.setDateTime(LocalDateTime.now());

        transactionRepository.save(t);

        // ✅ Send data to UI

        model.addAttribute("msg", "Deposit Successful ✅");
        model.addAttribute("redirectId", acc.getId()); // 👈 IMPORTANT
        // model.addAttribute("msg", "Deposit Successful");
        // model.addAttribute("balance", newBalance);

        return "deposit";
    }

    @GetMapping("/customer-dashboard/withdraw")
    public String withdrawpage() {
        return "withdraw";
    }

    @PostMapping("/savewithdraw")
    public String withdraw(@RequestParam String accountNumber,
            @RequestParam Double amount,
            Model model,
            // RedirectAttributes redirectAttributes
            HttpSession session

    ) {

        AccountOpeningEntity acc = accountRepository.findByAccountNumber(accountNumber.trim());

        // ❌ Account not found
        if (acc == null) {
            model.addAttribute("msg", "Invalid Account ❌");
            return "withdraw";
        }

        // ❌ Invalid amount
        if (amount == null || amount <= 0) {
            model.addAttribute("msg", "Enter valid amount ❌");
            return "withdraw";
        }

        // ❌ Balance check (🔥 important)
        if (acc.getBalance() == null || acc.getBalance() < amount) {
            model.addAttribute("msg", "Insufficient Balance ❌");
            return "withdraw";
        }

        // ✅ Withdraw (balance कमी कर)
        Double newBalance = acc.getBalance() - amount;
        acc.setBalance(newBalance);

        accountRepository.save(acc);

        // ✅ Transaction save
        Transaction t = new Transaction();
        t.setAccountNumber(acc.getAccountNumber());
        t.setAmount(amount);
        t.setType("WITHDRAW");

        t.setDateTime(LocalDateTime.now());

        transactionRepository.save(t);

        // session.setAttribute("msg", "Withdraw Successful ✅");

        // redirectAttributes.addFlashAttribute("msg", "Withdraw Successful ✅");

        model.addAttribute("msg", "Withdraw Successful ✅");
        model.addAttribute("redirectId", acc.getId()); // 👈 IMPORTANT

        model.addAttribute("msg", "Withdraw Successful ✅");
        model.addAttribute("balance", newBalance);

        return "withdraw";
    }

    private static final Logger logger = LoggerFactory.getLogger(AccountOpeningController.class);

    // -------------------------------------------------
    // Alias for case‑insensitive access to Mini Statement page
    // -------------------------------------------------
    @GetMapping("/minstatement/{accNo}")
    public String historyAlias(@PathVariable String accNo, Model model) {
        // Delegate to the primary handler so that behaviour stays identical
        return history(accNo, model);
    }

    // -------------------------------------------------
    // Primary handler for Mini Statement (case‑sensitive URL)
    // -------------------------------------------------
    @GetMapping("/MinStatement/{accNo}")
    public String history(@PathVariable String accNo, Model model) {
        logger.info("Fetching transaction history for account: {}", accNo);
        List<Transaction> list = transactionRepository.findByAccountNumberOrderByDateTimeDesc(accNo.trim());
        logger.info("Found {} transactions", list.size());
        // Log each transaction's details for debugging
        for (Transaction tx : list) {
            logger.debug("Tx ID: {}, Acc: {}, Type: {}, Amount: {}, Date: {}",
                    tx.getId(), tx.getAccountNumber(), tx.getType(), tx.getAmount(), tx.getDateTime());
        }
        model.addAttribute("transactions", list);
        model.addAttribute("accNo", accNo);
        return "MinStatement";
    }

    // -------------------------------------------------
    // CSV download for Mini Statement
    // -------------------------------------------------
    @GetMapping("/statement/{accNo}")
    public void downloadStatement(@PathVariable String accNo,
            HttpServletResponse response) throws IOException {
        // Fetch transactions (no trim needed as we store trimmed values)
        List<Transaction> list = transactionRepository
                .findByAccountNumberOrderByDateTimeDesc(accNo);

        // Set CSV response headers
        response.setContentType("text/csv");
        response.setHeader("Content-Disposition", "attachment; filename=statement_" + accNo + ".csv");

        // Write CSV header
        PrintWriter writer = response.getWriter();
        writer.println("ID,AccountNumber,DateTime,Type,Amount");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        for (Transaction t : list) {
            writer.printf("%d,%s,%s,%s,%.2f%n",
                    t.getId(),
                    t.getAccountNumber(),
                    t.getDateTime().format(formatter),
                    t.getType(),
                    t.getAmount());
        }
        writer.flush();
    }

    /*
     * @GetMapping("/photo/{id}")
     * 
     * @ResponseBody
     * public byte[] getPhoto(@PathVariable Long id){
     * 
     * AccountOpeningEntity user = repo.findById(id).orElse(null);
     * 
     * return user.getPhoto();
     * }
     */
    @GetMapping("/photo/{id}")
    @ResponseBody
    public byte[] getPhoto(@PathVariable Long id) {

        AccountOpeningEntity user = repo.findById(id).orElse(null);

        if (user == null || user.getPhoto() == null) {
            return new byte[0];
        }

        return user.getPhoto();
    }

    @Autowired
    private AccountOpeningRepository accountRepo;

    @GetMapping("/all-data")
    public String getAllData(Model model) {

        List<Object[]> list = accountRepo.getUserAccountData();

        model.addAttribute("data", list);

        return "all-data";
    }

}
