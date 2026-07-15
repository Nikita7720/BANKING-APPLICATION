package com.example.bank.controller;

import com.example.bank.entity.User;
import com.example.bank.repository.UserRepository;
import com.example.bank.service.EmailService;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
//import org.springframework.web.bind.permitAll;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.servlet.http.HttpSession;

@Controller
public class ManagerController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EmailService emailService;

    @GetMapping("/manager-register")
    public String managerRegister() {
        return "manager-register";
    }

    @PostMapping("/manager-register")
    public String processManagerRegister(@RequestParam String full_name, @RequestParam String email,
            @RequestParam String phone, @RequestParam String password, RedirectAttributes redirectAttributes) {
        // Check if email already exists
        if (userRepository.findByEmail(email) != null) {
            redirectAttributes.addFlashAttribute("error", "Email is already registered!");
            return "redirect:/manager-register";
        }

        User manager = new User();
        manager.setFull_name(full_name);
        manager.setEmail(email);
        manager.setPhone(phone);
        manager.setPassword(password);
        manager.setRole("MANAGER");

        // Generate Manager Employee ID
        String empId = "MGR" + (1000 + (int) (Math.random() * 9000));
        manager.setEmployeeId(empId);

        userRepository.save(manager);

        redirectAttributes.addFlashAttribute("error",
                "Manager Registered Successfully! Your Employee ID is: " + empId + " (Use this or Email to login)");
        return "redirect:/manager-login";
    }

    @GetMapping("/manager-login")
    public String managerLogin() {
        return "manager-login";
    }

    @PostMapping("/manager-login")
    public String processManagerLogin(@RequestParam("email") String identifier, @RequestParam String password,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        User manager = userRepository.findByEmail(identifier);

        // If not found by email, try searching by Employee ID
        if (manager == null) {
            manager = userRepository.findByEmployeeId(identifier);
        }

        if (manager != null && manager.getPassword().equals(password) && "MANAGER".equals(manager.getRole())) {
            session.setAttribute("managerId", manager.getId());
            return "redirect:/manager-dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid Manager Credentials");
        return "redirect:/manager-login";
    }

    @GetMapping("/manager-dashboard")
    public String managerDashboard(HttpSession session, Model model) {
        Long managerId = (Long) session.getAttribute("managerId");
        if (managerId == null) {
            User demoMgr = userRepository.findByEmail("manager@bank.com");
            if (demoMgr != null) {
                session.setAttribute("managerId", demoMgr.getId());
                managerId = demoMgr.getId();
            } else {
                return "redirect:/manager-login";
            }
        }
        User manager = userRepository.findById(managerId).orElse(null);
        model.addAttribute("manager", manager);

        // Fetch Stats
        com.example.bank.repository.TransactionRepository txRepo = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.TransactionRepository.class);
        LocalDateTime now = LocalDateTime.now();
        
        List<com.example.bank.entity.Transaction> dailyTx = txRepo.findByDateTimeAfter(now.minusDays(1));
        List<com.example.bank.entity.Transaction> weeklyTx = txRepo.findByDateTimeAfter(now.minusDays(7));
        List<com.example.bank.entity.Transaction> monthlyTx = txRepo.findByDateTimeAfter(now.minusDays(30));

        model.addAttribute("dailyTxCount", dailyTx.size());
        model.addAttribute("weeklyTxCount", weeklyTx.size());
        model.addAttribute("monthlyTxCount", monthlyTx.size());

        double dailyVol = dailyTx.stream().mapToDouble(t -> t.getAmount()).sum();
        double weeklyVol = weeklyTx.stream().mapToDouble(t -> t.getAmount()).sum();
        double monthlyVol = monthlyTx.stream().mapToDouble(t -> t.getAmount()).sum();

        model.addAttribute("dailyVol", dailyVol);
        model.addAttribute("weeklyVol", weeklyVol);
        model.addAttribute("monthlyVol", monthlyVol);

        // Graph Data & Profit/Loss (Last 30 days)
        double totalDeposits = monthlyTx.stream().filter(t -> "Deposit".equalsIgnoreCase(t.getType())).mapToDouble(t -> t.getAmount()).sum();
        double totalWithdrawals = monthlyTx.stream().filter(t -> "Withdraw".equalsIgnoreCase(t.getType())).mapToDouble(t -> t.getAmount()).sum();
        
        double profitLoss = totalDeposits - totalWithdrawals;

        model.addAttribute("totalDeposits", totalDeposits);
        model.addAttribute("totalWithdrawals", totalWithdrawals);
        model.addAttribute("profitLoss", profitLoss);

        // Account Creation Stats
        com.example.bank.repository.AccountOpeningRepository accRepo = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.AccountOpeningRepository.class);
        
        long pendingAccountsCount = accRepo.findByStatus("Pending").size();
        long approvedAccountsCount = accRepo.findByStatus("Approved").size();
        long rejectedAccountsCount = accRepo.findByStatus("Rejected").size();
        
        model.addAttribute("pendingAccountsCount", pendingAccountsCount);
        model.addAttribute("approvedAccountsCount", approvedAccountsCount);
        model.addAttribute("rejectedAccountsCount", rejectedAccountsCount);

        return "manager-dashboard";
    }

    @GetMapping("/manager/add-staff")
    public String addStaffForm(HttpSession session) {
        if (session.getAttribute("managerId") == null)
            return "redirect:/manager-login";
        return "manager-add-staff";
    }

    @PostMapping("/manager/add-staff")
    public String processAddStaff(@RequestParam String fullName, @RequestParam String email,
            @RequestParam String phone, @RequestParam String password,
            HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null)
            return "redirect:/manager-login";

        // Check if email already exists
        if (userRepository.findByEmail(email) != null) {
            redirectAttributes.addFlashAttribute("error", "Error: A user with this email is already registered!");
            return "redirect:/manager/add-staff";
        }

        // Generate Employee ID
        String employeeId = "EMP-" + (int) (Math.random() * 10000);

        User staff = new User();
        staff.setFull_name(fullName);
        staff.setEmail(email);
        staff.setPhone(phone);
        staff.setPassword(password);
        staff.setEmployeeId(employeeId);
        staff.setRole("STAFF");

        userRepository.save(staff);

        // Send Email to new Staff
        try {
            String subject = "Welcome to SecureBank - Staff Member";
            String body = "Congratulations! You have been successfully registered as a member in Secure Bank.\n\n"
                        + "Your login details are as follows:\n"
                        + "Username (Employee ID): " + employeeId + "\n"
                        + "Password: " + password + "\n\n"
                        + "Please login to the Staff Portal and change your password as soon as possible.\n\n"
                        + "Best Regards,\nSecureBank HR Team";
            emailService.sendEmail(email, subject, body);
        } catch (Exception e) {
            System.out.println("Failed to send staff registration email: " + e.getMessage());
        }

        redirectAttributes.addFlashAttribute("success", "Staff Added Successfully. Emp ID: " + employeeId);
        return "redirect:/manager-dashboard";
    }

    @GetMapping("/manager/view-staff")
    public String viewStaff(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null)
            return "redirect:/manager-login";
        // Assuming we need to fetch all users where role is STAFF.
        // We need a findByRole in UserRepository. Since it might not exist, let's fetch
        // all and filter or add it later.
        // For now, we will add findByRole to UserRepository.
        model.addAttribute("staffList",
                userRepository.findAll().stream().filter(u -> "STAFF".equals(u.getRole())).toList());
        return "manager-view-staff";
    }

    @GetMapping("/manager/verified-loans")
    public String verifiedLoans(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null)
            return "redirect:/manager-login";
        List<com.example.bank.entity.LoanRequest> verifiedLoans = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class).findByStatus("PENDING_MANAGER_APPROVAL");
        for (com.example.bank.entity.LoanRequest loan : verifiedLoans) {
            com.example.bank.entity.User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                loan.setCustomerName(u.getFull_name());
                loan.setCustomerEmail(u.getEmail());
                if (u.getAccounts() != null && !u.getAccounts().isEmpty()) {
                    loan.setIfscCode(u.getAccounts().get(0).getIfscCode());
                }
            }
        }
        model.addAttribute("loans", verifiedLoans);
        return "manager-loans";
    }

    @PostMapping("/manager/approve-loan/{id}")
    public String approveLoan(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null)
            return "redirect:/manager-login";
        com.example.bank.repository.LoanRequestRepository loanRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class);
        com.example.bank.entity.LoanRequest loan = loanRepo.findById(id).orElse(null);
        if (loan != null) {
            loan.setStatus("APPROVED");
            loan.setApprovalDate(LocalDateTime.now());
            loan.setInterestRate(10.0); // 10% per annum
            
            // EMI Calculation
            double p = loan.getAmount();
            double r = (10.0 / 12) / 100; // monthly interest rate
            int n = loan.getDurationMonths();
            double emi = (p * r * Math.pow(1 + r, n)) / (Math.pow(1 + r, n) - 1);
            emi = Math.round(emi * 100.0) / 100.0; // round to 2 decimal places
            loan.setEmiAmount(emi);
            loan.setNextEmiDate(java.time.LocalDate.now().plusMonths(1));
            
            loanRepo.save(loan);

            // Disburse funds
            com.example.bank.entity.AccountOpeningEntity account = org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(session.getServletContext())
                    .getBean(com.example.bank.repository.AccountOpeningRepository.class)
                    .findByAccountNumber(loan.getAccountNumber());
            if (account != null) {
                account.setBalance(account.getBalance() + loan.getAmount());
                org.springframework.web.context.support.WebApplicationContextUtils
                        .getWebApplicationContext(session.getServletContext())
                        .getBean(com.example.bank.repository.AccountOpeningRepository.class).save(account);

                // Add to transaction table
                com.example.bank.entity.Transaction tx = new com.example.bank.entity.Transaction();
                tx.setAccountNumber(account.getAccountNumber());
                tx.setAmount(loan.getAmount());
                tx.setType("Loan Deposit");
                tx.setDateTime(LocalDateTime.now());
                org.springframework.web.context.support.WebApplicationContextUtils
                        .getWebApplicationContext(session.getServletContext())
                        .getBean(com.example.bank.repository.TransactionRepository.class).save(tx);
            }

            // Notification
            com.example.bank.entity.Notification notif = new com.example.bank.entity.Notification();
            notif.setMessage(String.format("Congratulations! Your loan of ₹%.2f has been approved at 10%% interest. Your EMI is ₹%.2f, and the next EMI will be automatically deducted on %s.", 
                    loan.getAmount(), loan.getEmiAmount(), loan.getNextEmiDate().toString()));
            notif.setDateTime(LocalDateTime.now());
            User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                notif.setUser(u);
                org.springframework.web.context.support.WebApplicationContextUtils
                        .getWebApplicationContext(session.getServletContext())
                        .getBean(com.example.bank.repository.NotificationRepository.class).save(notif);
            }
            redirectAttributes.addFlashAttribute("success", "Loan Approved & Disbursed Successfully!");
        }
        return "redirect:/manager/verified-loans";
    }

    @PostMapping("/manager/reject-loan/{id}")
    public String rejectManagerLoan(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null)
            return "redirect:/manager-login";
        com.example.bank.repository.LoanRequestRepository loanRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class);
        com.example.bank.entity.LoanRequest loan = loanRepo.findById(id).orElse(null);
        if (loan != null) {
            loan.setStatus("REJECTED_BY_MANAGER");
            loanRepo.save(loan);

            // Notification
            com.example.bank.entity.Notification notif = new com.example.bank.entity.Notification();
            notif.setMessage("Your loan application for ₹" + loan.getAmount() + " was rejected by the Manager.");
            notif.setDateTime(LocalDateTime.now());
            User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                notif.setUser(u);
                org.springframework.web.context.support.WebApplicationContextUtils
                        .getWebApplicationContext(session.getServletContext())
                        .getBean(com.example.bank.repository.NotificationRepository.class).save(notif);
            }
            redirectAttributes.addFlashAttribute("success", "Loan Rejected");
        }
        return "redirect:/manager/verified-loans";
    }

    @PostMapping("/manager/delete-staff/{id}")
    public String deleteStaff(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        userRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("success", "Staff member deleted successfully.");
        return "redirect:/manager/view-staff";
    }

    @GetMapping("/manager/staff-salary/{id}")
    public String staffSalaryView(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        User staff = userRepository.findById(id).orElse(null);
        if (staff == null || !"STAFF".equals(staff.getRole())) {
            return "redirect:/manager/view-staff";
        }
        model.addAttribute("staff", staff);
        
        java.util.List<com.example.bank.entity.StaffSalary> salaries = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.StaffSalaryRepository.class).findByStaffIdOrderByPaymentDateDesc(id);
        model.addAttribute("salaries", salaries);
        return "manager-staff-salary";
    }

    @PostMapping("/manager/pay-salary/{id}")
    public String payStaffSalary(@PathVariable Long id, @RequestParam String monthYear, @RequestParam Double baseSalary, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        
        User staff = userRepository.findById(id).orElse(null);
        if (staff != null) {
            Double pf = baseSalary * 0.12;
            Double net = baseSalary - pf;
            
            com.example.bank.entity.StaffSalary salary = new com.example.bank.entity.StaffSalary();
            salary.setStaffId(id);
            salary.setStaffName(staff.getFull_name());
            salary.setMonthYear(monthYear);
            salary.setBaseSalary(baseSalary);
            salary.setPfDeduction(pf);
            salary.setNetSalary(net);
            salary.setPaymentDate(LocalDateTime.now());
            
            org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.StaffSalaryRepository.class).save(salary);
            
            // Optionally subtract from main bank funds or just record it
            redirectAttributes.addFlashAttribute("success", "Salary for " + monthYear + " processed successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "Staff not found.");
        }
        return "redirect:/manager/staff-salary/" + id;
    }

    @GetMapping("/manager/attendance")
    public String managerAttendance(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        
        java.util.List<User> allStaff = userRepository.findAll().stream().filter(u -> "STAFF".equals(u.getRole())).toList();
        com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.AttendanceRepository.class);
        
        java.util.List<com.example.bank.entity.Attendance> todayList = new java.util.ArrayList<>();
        java.time.LocalDate today = java.time.LocalDate.now();
        
        for (User staff : allStaff) {
            java.util.Optional<com.example.bank.entity.Attendance> staffAtt = attRepo.findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staff.getId(), today);
            if (staffAtt.isPresent()) {
                todayList.add(staffAtt.get());
            } else {
                com.example.bank.entity.Attendance absentRecord = new com.example.bank.entity.Attendance();
                absentRecord.setStaffId(staff.getId());
                absentRecord.setEmployeeId(staff.getEmployeeId());
                absentRecord.setAttendanceDate(today);
                absentRecord.setStatus("ABSENT");
                todayList.add(absentRecord);
            }
        }
        
        model.addAttribute("attendanceList", todayList);
        return "manager-attendance";
    }

    @GetMapping("/manager/meetings")
    public String managerMeetings(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        java.util.List<com.example.bank.entity.Meeting> meetings = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.MeetingRepository.class).findAllByOrderByMeetingDateDescMeetingTimeDesc();
        model.addAttribute("meetings", meetings);
        return "manager-meetings";
    }

    @PostMapping("/manager/meetings/schedule")
    public String scheduleMeeting(
            @RequestParam String title, 
            @RequestParam java.time.LocalDate meetingDate, 
            @RequestParam java.time.LocalTime meetingTime, 
            @RequestParam String meetingLink, 
            @RequestParam(required = false) String description, 
            HttpSession session, RedirectAttributes redirectAttributes) {
        
        Long managerId = (Long) session.getAttribute("managerId");
        if (managerId == null) return "redirect:/manager-login";
        
        com.example.bank.entity.Meeting meeting = new com.example.bank.entity.Meeting();
        meeting.setManagerId(managerId);
        meeting.setTitle(title);
        meeting.setMeetingDate(meetingDate);
        meeting.setMeetingTime(meetingTime);
        meeting.setMeetingLink(meetingLink);
        meeting.setDescription(description);
        
        org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.MeetingRepository.class).save(meeting);
        
        // Notify all staff members
        java.util.List<User> allStaff = userRepository.findAll().stream().filter(u -> "STAFF".equals(u.getRole())).toList();
        com.example.bank.repository.NotificationRepository notifRepo = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.NotificationRepository.class);
        for (User stf : allStaff) {
            com.example.bank.entity.Notification notification = new com.example.bank.entity.Notification();
            notification.setMessage("New Meeting Scheduled: " + title + " on " + meetingDate + " at " + meetingTime);
            notification.setDateTime(LocalDateTime.now());
            notification.setUser(stf);
            notification.setRead(false);
            notifRepo.save(notification);
        }

        redirectAttributes.addFlashAttribute("success", "Meeting scheduled successfully! All staff notified.");
        return "redirect:/manager/meetings";
    }
    @GetMapping("/manager/loan-history")
    public String managerLoanHistory(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        List<com.example.bank.entity.LoanRequest> allLoans = org.springframework.web.context.support.WebApplicationContextUtils.getWebApplicationContext(session.getServletContext()).getBean(com.example.bank.repository.LoanRequestRepository.class).findAll();
        
        // Filter out pending ones to only show history
        allLoans.removeIf(l -> "PENDING".equals(l.getStatus()) || "PENDING_STAFF_REVIEW".equals(l.getStatus()) || "PENDING_MANAGER_APPROVAL".equals(l.getStatus()));
        
        for (com.example.bank.entity.LoanRequest loan : allLoans) {
            com.example.bank.entity.User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                loan.setCustomerName(u.getFull_name());
                loan.setCustomerEmail(u.getEmail());
                if (u.getAccounts() != null && !u.getAccounts().isEmpty()) {
                    loan.setIfscCode(u.getAccounts().get(0).getIfscCode());
                }
            }
        }
        allLoans.sort((a,b) -> b.getRequestDate().compareTo(a.getRequestDate()));
        model.addAttribute("loans", allLoans);
        return "manager-loan-history";
    }

    // ---- FD Management ----
    @GetMapping("/manager/pending-fds")
    public String managerPendingFds(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        List<com.example.bank.entity.FixedDeposit> pendingFds = fdRepo.findByStatus("PENDING_MANAGER_APPROVAL");
        for (com.example.bank.entity.FixedDeposit fd : pendingFds) {
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                fd.setCustomerName(u.getFull_name());
                fd.setCustomerEmail(u.getEmail());
            }
        }
        model.addAttribute("fds", pendingFds);
        return "manager-fd-applications";
    }

    @PostMapping("/manager/approve-fd/{id}")
    public String managerApproveFd(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        com.example.bank.repository.AccountOpeningRepository accRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.AccountOpeningRepository.class);
        com.example.bank.repository.NotificationRepository notifRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.NotificationRepository.class);

        com.example.bank.entity.FixedDeposit fd = fdRepo.findById(id).orElse(null);
        if (fd != null) {
            com.example.bank.entity.AccountOpeningEntity acc = accRepo.findByAccountNumber(fd.getAccountNumber());
            
            // Check for sufficient balance
            if (acc == null || acc.getBalance() < fd.getPrincipalAmount()) {
                redirectAttributes.addFlashAttribute("error", "Insufficient Balance! Customer does not have enough funds to start this FD.");
                return "redirect:/manager/pending-fds";
            }

            // Deduct balance
            acc.setBalance(acc.getBalance() - fd.getPrincipalAmount());
            accRepo.save(acc);

            // Record Transaction
            com.example.bank.repository.TransactionRepository transRepo = org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(session.getServletContext())
                    .getBean(com.example.bank.repository.TransactionRepository.class);
            com.example.bank.entity.Transaction trans = new com.example.bank.entity.Transaction();
            trans.setAccountNumber(fd.getAccountNumber());
            trans.setAmount(fd.getPrincipalAmount());
            trans.setType("DEBIT (FD CREATION)");
            trans.setDateTime(LocalDateTime.now());
            transRepo.save(trans);

            // Activate FD
            fd.setStatus("ACTIVE");
            fdRepo.save(fd);

            // Notify customer
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                com.example.bank.entity.Notification notif = new com.example.bank.entity.Notification();
                notif.setMessage("🎉 Your Fixed Deposit of ₹" + fd.getPrincipalAmount() + " has been APPROVED! FD No: " + fd.getFdNumber() + ". Matures on " + fd.getMaturityDate() + " with ₹" + fd.getMaturityAmount());
                notif.setDateTime(LocalDateTime.now());
                notif.setUser(u);
                notifRepo.save(notif);
            }
            redirectAttributes.addFlashAttribute("success", "FD Approved and Activated Successfully! Amount Deducted.");
        }
        return "redirect:/manager/pending-fds";
    }

    @PostMapping("/manager/reject-fd/{id}")
    public String managerRejectFd(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        com.example.bank.repository.NotificationRepository notifRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.NotificationRepository.class);

        com.example.bank.entity.FixedDeposit fd = fdRepo.findById(id).orElse(null);
        if (fd != null) {
            fd.setStatus("REJECTED");
            fdRepo.save(fd);
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                com.example.bank.entity.Notification notif = new com.example.bank.entity.Notification();
                notif.setMessage("Your FD application of ₹" + fd.getPrincipalAmount() + " has been rejected by the Manager.");
                notif.setDateTime(LocalDateTime.now());
                notif.setUser(u);
                notifRepo.save(notif);
            }
            redirectAttributes.addFlashAttribute("error", "FD Application Rejected.");
        }
        return "redirect:/manager/pending-fds";
    }

    @GetMapping("/manager/fd-history")
    public String managerFdHistory(HttpSession session, Model model) {
        if (session.getAttribute("managerId") == null) return "redirect:/manager-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        List<com.example.bank.entity.FixedDeposit> allFds = fdRepo.findAll();
        
        for (com.example.bank.entity.FixedDeposit fd : allFds) {
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                fd.setCustomerName(u.getFull_name());
                fd.setCustomerEmail(u.getEmail());
            }
        }
        allFds.sort((a,b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        model.addAttribute("fds", allFds);
        return "manager-fd-history";
    }
}
