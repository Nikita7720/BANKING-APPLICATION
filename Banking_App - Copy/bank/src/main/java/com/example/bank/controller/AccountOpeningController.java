package com.example.bank.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.entity.Transaction;
import com.example.bank.entity.User;
import com.example.bank.repository.AccountOpeningRepository;
import com.example.bank.repository.TransactionRepository;
import com.example.bank.repository.UserRepository;

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

    @GetMapping("/setup-demo-data")
    @ResponseBody
    public String setupDemoData() {
        // Create manager
        User mgr = UserRepo.findByEmail("manager@bank.com");
        if (mgr == null) {
            mgr = new User();
            mgr.setFull_name("Demo Manager");
            mgr.setEmail("manager@bank.com");
            mgr.setPhone("9876543210");
            mgr.setPassword("manager123");
            mgr.setRole("MANAGER");
            mgr.setEmployeeId("MGR1001");
            UserRepo.save(mgr);
        }

        // Create staff
        User staff = UserRepo.findByEmail("staff@bank.com");
        if (staff == null) {
            staff = new User();
            staff.setFull_name("Demo Staff");
            staff.setEmail("staff@bank.com");
            staff.setPhone("9876543211");
            staff.setPassword("staff123");
            staff.setRole("STAFF");
            staff.setEmployeeId("EMP1001");
            UserRepo.save(staff);
        }

        // Create customer
        User cust = UserRepo.findByEmail("customer@bank.com");
        if (cust == null) {
            cust = new User();
            cust.setFull_name("Demo Customer");
            cust.setEmail("customer@bank.com");
            cust.setPhone("9876543212");
            cust.setPassword("customer123");
            cust.setRole("CUSTOMER");
            UserRepo.save(cust);
        }

        // Create AccountOpeningEntity for customer
        AccountOpeningEntity acc = accountRepository.findByUserId(cust.getId());
        if (acc == null) {
            acc = new AccountOpeningEntity();
            acc.setUser(cust);
            acc.setFullname("Demo Customer");
            acc.setEmail("customer@bank.com");
            acc.setStatus("Approved");
            acc.setAccountNumber("SECU123456");
            acc.setIfscCode("SECU0001234");
            acc.setBalance(50000.0);
            accountRepository.save(acc);

            cust.setAccountNumber("SECU123456");
            cust.setIfscCode("SECU0001234");
            cust.setStatus("Approved");
            UserRepo.save(cust);
        }
        return "Demo data setup successful! Customer ID: " + acc.getId();
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
        
        if(acc == null){
            model.addAttribute("msg","Invalid Account ❌");
            return "Existinglogin";
        }
        
        User user = acc.getUser();
        
        if(user == null || !password.equals(user.getPassword())){
            model.addAttribute("msg","Wrong Password ❌");
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
        
        // Ensure UPI ID exists for approved accounts
        if (app.getStatus() != null && app.getStatus().equalsIgnoreCase("Approved") && app.getUpiId() == null && app.getAccountNumber() != null) {
            app.setUpiId(app.getAccountNumber() + "@securebank");
            repo.save(app);
            if (app.getUser() != null) {
                app.getUser().setUpiId(app.getUpiId());
                UserRepo.save(app.getUser());
            }
        }
        
        // Fetch notifications
        if (app.getUser() != null) {
            java.util.List<com.example.bank.entity.Notification> notifications = notificationRepo.findByUserIdOrderByDateTimeDesc(app.getUser().getId());
            model.addAttribute("notifications", notifications);
            model.addAttribute("notifCount", notifications.stream().filter(n -> !n.isRead()).count());
            
            jakarta.servlet.ServletContext ctx = jakarta.servlet.http.HttpServletRequest.class.cast(
                org.springframework.web.context.request.RequestContextHolder.currentRequestAttributes()
                    .resolveReference(org.springframework.web.context.request.RequestAttributes.REFERENCE_REQUEST)
            ).getServletContext();
            org.springframework.context.ApplicationContext appCtx = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(ctx);
            
            // Fetch Loans
            java.util.List<com.example.bank.entity.LoanRequest> myLoans = appCtx
                    .getBean(com.example.bank.repository.LoanRequestRepository.class).findByCustomerId(app.getUser().getId());
            model.addAttribute("myLoans", myLoans);

            // Fetch FDs
            java.util.List<com.example.bank.entity.FixedDeposit> myFds = appCtx
                    .getBean(com.example.bank.repository.FixedDepositRepository.class).findByCustomerId(app.getUser().getId());
            model.addAttribute("myFds", myFds);
        }

        return "customer-dashboard";
    }

    @Autowired
    AccountOpeningRepository repo;

    @PostMapping("/saveUser")
    public String saveAccount(AccountOpeningEntity account,
            @RequestParam("file") MultipartFile file, HttpSession session,
            Model model) throws Exception {

        if (!file.isEmpty()) {
            account.setPhoto(file.getBytes());
        }

        try {
            if (!file.isEmpty()) {
                account.setPhoto(file.getBytes()); // 🔥 FIX
            }
            // 🔥 THIS IS YOUR LINK
            Long userId = (Long) session.getAttribute("userId");

            // 🔥 STEP 2: user fetch कर
            User user = UserRepo.findById(userId).orElse(null);

            // 🔥 STEP 3: LINK कर
            account.setUser(user);

            repo.save(account);

            model.addAttribute("msg", "Application Sent Successfully");

        } catch (IOException e) {
            model.addAttribute("msg", "Error uploading image");
        }
        return "AccountOpening";
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
    @GetMapping("/approve/{id}")
    public String approve(@PathVariable Long id) {
        // ... disabled ...
        return "redirect:/staff";
    }
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
    public String showDepositPage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            AccountOpeningEntity acc = accountRepository.findByUserId(userId);
            if (acc != null) {
                model.addAttribute("loggedInAccount", acc.getAccountNumber());
                model.addAttribute("loggedInUpi", acc.getUpiId());
                model.addAttribute("fullname", acc.getFullname());
                model.addAttribute("balance", acc.getBalance());
                model.addAttribute("redirectId", acc.getId());
            }
        }
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

        AccountOpeningEntity acc = null;
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            if (accountNumber.contains("@")) {
                acc = accountRepository.findByUpiId(accountNumber.trim());
            } else {
                acc = accountRepository.findByAccountNumber(accountNumber.trim());
            }
        }

        // ❌ Account not found
        if (acc == null) {
            model.addAttribute("msg", "Invalid Account or UPI ID ❌");
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

        // ✅ Save account
        accountRepository.save(acc);

        // ✅ Save transaction
        Transaction t = new Transaction();
        t.setAccountNumber(accountNumber);
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
    public String withdrawpage(HttpSession session, Model model) {
        Long userId = (Long) session.getAttribute("userId");
        if (userId != null) {
            AccountOpeningEntity acc = accountRepository.findByUserId(userId);
            if (acc != null) {
                model.addAttribute("loggedInAccount", acc.getAccountNumber());
                model.addAttribute("loggedInUpi", acc.getUpiId());
                model.addAttribute("fullname", acc.getFullname());
                model.addAttribute("balance", acc.getBalance());
                model.addAttribute("redirectId", acc.getId());
            }
        }
        return "withdraw";
    }

    @PostMapping("/savewithdraw")
    public String withdraw(@RequestParam String accountNumber,
            @RequestParam Double amount,
            Model model,
            // RedirectAttributes redirectAttributes
            HttpSession session

    ) {

        AccountOpeningEntity acc = null;
        if (accountNumber != null && !accountNumber.trim().isEmpty()) {
            if (accountNumber.contains("@")) {
                acc = accountRepository.findByUpiId(accountNumber.trim());
            } else {
                acc = accountRepository.findByAccountNumber(accountNumber.trim());
            }
        }

        // ❌ Account not found
        if (acc == null) {
            model.addAttribute("msg", "Invalid Account or UPI ID ❌");
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
        t.setAccountNumber(accountNumber);
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

    @GetMapping("/MinStatement/{accNo}")
    public String history(@PathVariable String accNo, Model model) {

        List<Transaction> list = transactionRepository.findByAccountNumberOrderByDateTimeDesc(accNo);

        model.addAttribute("transactions", list);
        model.addAttribute("accNo", accNo);

        return "MinStatement";
    }

    @GetMapping("/statement/{accNo}")
    public String printPage(@PathVariable String accNo, Model model) {

        List<Transaction> list = transactionRepository.findByAccountNumberOrderByDateTimeDesc(accNo);

        model.addAttribute("transactions", list);
        model.addAttribute("accNo", accNo);

        return "statment";
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
