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

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.repository.AccountOpeningRepository;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

import com.example.bank.entity.Notification;
import com.example.bank.repository.NotificationRepository;
import java.time.LocalDateTime;

@Controller
public class StaffController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountOpeningRepository accountOpeningRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;

    @GetMapping("/staff-login")
    public String staffLogin() {
        return "staff-login";
    }

    @PostMapping("/staff-login")
    public String processStaffLogin(@RequestParam String employeeId, @RequestParam String password, HttpSession session,
            RedirectAttributes redirectAttributes) {
        User staff = userRepository.findByEmployeeId(employeeId);
        if (staff != null && staff.getPassword().equals(password) && "STAFF".equals(staff.getRole())) {
            session.setAttribute("staffId", staff.getId());
            return "redirect:/staff-dashboard";
        }
        redirectAttributes.addFlashAttribute("error", "Invalid Staff Credentials");
        return "redirect:/staff-login";
    }

    @GetMapping("/staff-dashboard")
    public String staffDashboard(HttpSession session, Model model) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null) {
            User demoStaff = userRepository.findByEmail("staff@bank.com");
            if (demoStaff != null) {
                session.setAttribute("staffId", demoStaff.getId());
                staffId = demoStaff.getId();
            } else {
                return "redirect:/staff-login";
            }
        }
        User staff = userRepository.findById(staffId).orElse(null);
        model.addAttribute("staff", staff);

        java.util.List<com.example.bank.entity.Meeting> meetings = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.MeetingRepository.class)
                .findByMeetingDateAfterOrderByMeetingDateAscMeetingTimeAsc(java.time.LocalDate.now().minusDays(1));
        model.addAttribute("meetings", meetings);

        java.util.List<com.example.bank.entity.Notification> notifications = notificationRepository
                .findByUserIdOrderByDateTimeDesc(staffId);
        model.addAttribute("notifications", notifications);
        long unreadCount = notifications.stream().filter(n -> !n.isRead()).count();
        model.addAttribute("unreadCount", unreadCount);

        // Face Attendance Check
        com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.AttendanceRepository.class);
        java.util.Optional<com.example.bank.entity.Attendance> todayAtt = attRepo
                .findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staffId, java.time.LocalDate.now());

        if (todayAtt.isPresent() && todayAtt.get().getCheckOutTime() == null) {
            model.addAttribute("hasCheckedIn", true);
            model.addAttribute("checkInTime", todayAtt.get().getCheckInTime().toString().substring(0, 5)); // HH:mm
            model.addAttribute("hasCheckedOut", false);
        } else {
            model.addAttribute("hasCheckedIn", false);
            model.addAttribute("hasCheckedOut", false);
        }

        // Fetch Top 5 Recent Accounts
        List<AccountOpeningEntity> allAccounts = accountOpeningRepository.findAll();
        allAccounts.sort((a, b) -> b.getId().compareTo(a.getId()));
        List<AccountOpeningEntity> recentAccounts = allAccounts.size() > 5 ? allAccounts.subList(0, 5) : allAccounts;
        model.addAttribute("recentPendingAccounts", recentAccounts);

        return "staff-dashboard";
    }

    @GetMapping("/staff/pending-accounts")
    public String pendingAccounts(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        List<AccountOpeningEntity> allAccounts = accountOpeningRepository.findAll();
        allAccounts.sort((a, b) -> b.getId().compareTo(a.getId()));
        model.addAttribute("accounts", allAccounts);
        return "staff-pending-accounts";
    }

    @PostMapping("/staff/approve-account/{id}")
    public String approveAccount(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        AccountOpeningEntity account = accountOpeningRepository.findById(id).orElse(null);
        if (account != null) {
            String accNumber = "SECU" + (int) (Math.random() * 1000000);
            String ifsc = "SECU0001234";
            String tempPassword = UUID.randomUUID().toString().substring(0, 8);
            String upiId = accNumber + "@securebank";

            account.setAccountNumber(accNumber);
            account.setIfscCode(ifsc);
            account.setUpiId(upiId);
            account.setStatus("Approved");
            account.setBalance(0.0);

            if (account.getUser() != null) {
                User u = account.getUser();
                u.setAccountNumber(accNumber);
                u.setIfscCode(ifsc);
                u.setUpiId(upiId);
                u.setPassword(tempPassword);
                u.setStatus("Approved");
                // Copy PAN and Aadhar
                u.setPan(account.getPan());
                u.setAadhar(account.getAadhar());
                userRepository.save(u);

                // Save Notification
                Notification notif = new Notification();
                notif.setMessage("Congratulations! Your account has been approved. Account No: " + accNumber);
                notif.setDateTime(LocalDateTime.now());
                notif.setUser(u);
                notificationRepository.save(notif);
            }

            accountOpeningRepository.save(account);

            // Send Email
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setTo(account.getEmail());
                message.setSubject("Account Approved - Secure Bank");
                message.setText("Congratulations! Your account is approved.\n" +
                        "Account No: " + accNumber + "\n" +
                        "IFSC: " + ifsc + "\n" +
                        "Temporary Password: " + tempPassword + "\n" +
                        "Please login and change your password.");
                mailSender.send(message);
            } catch (Exception e) {
                // log error, but continue
            }

            redirectAttributes.addFlashAttribute("success", "Account Approved Successfully");
        }
        return "redirect:/staff/pending-accounts";
    }

    @GetMapping("/staff/view-account/{id}")
    public String viewAccount(@PathVariable Long id, HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        AccountOpeningEntity account = accountOpeningRepository.findById(id).orElse(null);
        if (account != null) {
            model.addAttribute("app", account);
            return "staff-view-account";
        }
        return "redirect:/staff/pending-accounts";
    }

    @PostMapping("/staff/reject-account/{id}")
    public String rejectAccount(@PathVariable Long id, @RequestParam String rejectionReason, HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        AccountOpeningEntity account = accountOpeningRepository.findById(id).orElse(null);
        if (account != null) {
            account.setStatus("Rejected");
            account.setRejectionReason(rejectionReason);
            accountOpeningRepository.save(account);

            // Also update User status if user is linked (it should be)
            if (account.getUser() != null) {
                User u = account.getUser();
                u.setStatus("Rejected");
                userRepository.save(u);

                // Send Notification
                Notification notif = new Notification();
                notif.setMessage("Your account application was rejected. Reason: " + rejectionReason);
                notif.setDateTime(LocalDateTime.now());
                notif.setUser(u);
                notificationRepository.save(notif);
            }

            // Send Email
            try {
                org.springframework.mail.SimpleMailMessage message = new org.springframework.mail.SimpleMailMessage();
                message.setTo(account.getEmail());
                message.setSubject("Account Application Update - NexaBank");
                message.setText("Dear " + account.getFullname() + ",\n\n" +
                        "We regret to inform you that your account application has been rejected.\n" +
                        "Reason for rejection: " + rejectionReason + "\n\n" +
                        "Please login to your customer dashboard for more details or contact support.\n\n" +
                        "Regards,\nNexaBank Support Team");
                mailSender.send(message);
            } catch (Exception e) {
                // log error, but continue
            }

            redirectAttributes.addFlashAttribute("error", "Account Application Rejected.");
        }
        return "redirect:/staff/pending-accounts";
    }

    @GetMapping("/staff/pending-loans")
    public String pendingLoans(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        List<com.example.bank.entity.LoanRequest> pendingLoans = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class).findByStatus("PENDING_STAFF_REVIEW");
        for (com.example.bank.entity.LoanRequest loan : pendingLoans) {
            com.example.bank.entity.User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                loan.setCustomerName(u.getFull_name());
                loan.setCustomerEmail(u.getEmail());
                if (u.getIfscCode() != null) {
                    loan.setIfscCode(u.getIfscCode());
                }
            }
        }
        model.addAttribute("loans", pendingLoans);
        return "staff-loans";
    }

    @PostMapping("/staff/verify-loan/{id}")
    public String verifyLoan(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        com.example.bank.repository.LoanRequestRepository loanRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class);
        com.example.bank.entity.LoanRequest loan = loanRepo.findById(id).orElse(null);
        if (loan != null) {
            loan.setStatus("PENDING_MANAGER_APPROVAL");
            loanRepo.save(loan);

            // Notification
            Notification notif = new Notification();
            notif.setMessage("Your loan application for ₹" + loan.getAmount()
                    + " is verified and sent for final manager approval.");
            notif.setDateTime(LocalDateTime.now());
            User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                notif.setUser(u);
                notificationRepository.save(notif);
            }
            redirectAttributes.addFlashAttribute("success", "Loan Verified Successfully");
        }
        return "redirect:/staff/pending-loans";
    }

    @GetMapping("/staff/loan-history")
    public String loanHistory(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        List<com.example.bank.entity.LoanRequest> allLoans = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class).findAll();

        // Filter out pending ones to only show history
        allLoans.removeIf(l -> "PENDING_STAFF_REVIEW".equals(l.getStatus()));

        for (com.example.bank.entity.LoanRequest loan : allLoans) {
            com.example.bank.entity.User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                loan.setCustomerName(u.getFull_name());
                loan.setCustomerEmail(u.getEmail());
                if (u.getIfscCode() != null) {
                    loan.setIfscCode(u.getIfscCode());
                }
            }
        }
        allLoans.sort((a, b) -> b.getRequestDate().compareTo(a.getRequestDate()));
        model.addAttribute("loans", allLoans);
        return "staff-loan-history";
    }

    @PostMapping("/staff/reject-loan/{id}")
    public String rejectLoan(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        com.example.bank.repository.LoanRequestRepository loanRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.LoanRequestRepository.class);
        com.example.bank.entity.LoanRequest loan = loanRepo.findById(id).orElse(null);
        if (loan != null) {
            loan.setStatus("REJECTED_BY_STAFF");
            loanRepo.save(loan);

            // Notification
            Notification notif = new Notification();
            notif.setMessage("Your loan application for ₹" + loan.getAmount() + " was rejected by verification staff.");
            notif.setDateTime(LocalDateTime.now());
            User u = userRepository.findById(loan.getCustomerId()).orElse(null);
            if (u != null) {
                notif.setUser(u);
                notificationRepository.save(notif);
            }
            redirectAttributes.addFlashAttribute("success", "Loan Rejected");
        }
        return "redirect:/staff/pending-loans";
    }


    @PostMapping("/staff/mark-attendance")
    public String markAttendance(@RequestParam String faceData,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            HttpSession session, RedirectAttributes ra) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        // --- SECURE WORKSPACE CHECK ---
        double bankLat = 18.651900;
        double bankLon = 73.797100;
        double maxDistanceMeters = 50000.0;

        if (latitude != null && longitude != null) {
            double distance = calculateDistance(latitude, longitude, bankLat, bankLon);
            if (distance > maxDistanceMeters) {
                ra.addFlashAttribute("error",
                        "Attendance Rejected: You must be at the bank branch to check in. (Distance: "
                                + String.format("%.0f", distance) + "m)");
                return "redirect:/staff-dashboard";
            }
        } else {
            ra.addFlashAttribute("error", "Attendance Rejected: Location data is missing.");
            return "redirect:/staff-dashboard";
        }
        // ------------------------------

        Long staffId = (Long) session.getAttribute("staffId");
        User staff = userRepository.findById(staffId).orElse(null);
        if (staff != null) {
            com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(session.getServletContext())
                    .getBean(com.example.bank.repository.AttendanceRepository.class);

            // Check if already checked in and not checked out
            java.util.Optional<com.example.bank.entity.Attendance> existingAtt = attRepo
                    .findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staffId, java.time.LocalDate.now());
            if (existingAtt.isPresent() && existingAtt.get().getCheckOutTime() == null) {
                ra.addFlashAttribute("error", "You are already checked in. Please check out first.");
                return "redirect:/staff-dashboard";
            }

            com.example.bank.entity.Attendance attendance = new com.example.bank.entity.Attendance();
            attendance.setStaffId(staffId);
            attendance.setEmployeeId(staff.getEmployeeId());
            attendance.setAttendanceDate(java.time.LocalDate.now());
            attendance.setCheckInTime(java.time.LocalTime.now());
            attendance.setStatus("PRESENT");

            attRepo.save(attendance);

            // Log to console (in real app, we might verify faceData here)
            System.out.println("Face Attendance Marked for: " + staff.getEmployeeId());

            ra.addFlashAttribute("success", "Attendance Marked Successfully!");
        }
        return "redirect:/staff-dashboard";
    }

    @PostMapping("/staff/mark-checkout")
    public String markCheckout(HttpSession session, RedirectAttributes redirectAttributes) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null)
            return "redirect:/staff-login";

        com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.AttendanceRepository.class);

        java.util.Optional<com.example.bank.entity.Attendance> todayAtt = attRepo
                .findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staffId, java.time.LocalDate.now());
        if (todayAtt.isPresent() && todayAtt.get().getCheckOutTime() == null) {
            com.example.bank.entity.Attendance attendance = todayAtt.get();
            attendance.setCheckOutTime(java.time.LocalTime.now());
            attRepo.save(attendance);
            redirectAttributes.addFlashAttribute("success", "Checked out successfully!");
        } else {
            redirectAttributes.addFlashAttribute("error", "You must check in first before checking out.");
        }
        return "redirect:/staff-dashboard";
    }

    @GetMapping("/staff/attendance-history")
    public String attendanceHistory(HttpSession session, Model model) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null)
            return "redirect:/staff-login";

        com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.AttendanceRepository.class);

        java.util.List<com.example.bank.entity.Attendance> attendanceHistory = attRepo.findByStaffId(staffId);
        attendanceHistory.sort((a, b) -> b.getAttendanceDate().compareTo(a.getAttendanceDate()));
        model.addAttribute("attendanceHistory", attendanceHistory);

        return "staff-attendance-history";
    }

    // ---- FD Management ----
    @GetMapping("/staff/pending-fds")
    public String pendingFds(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        List<com.example.bank.entity.FixedDeposit> pendingFds = fdRepo.findByStatus("PENDING_STAFF_REVIEW");
        for (com.example.bank.entity.FixedDeposit fd : pendingFds) {
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                fd.setCustomerName(u.getFull_name());
                fd.setCustomerEmail(u.getEmail());
            }
        }
        model.addAttribute("fds", pendingFds);
        return "staff-fd-applications";
    }

    @PostMapping("/staff/verify-fd/{id}")
    public String verifyFd(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        com.example.bank.entity.FixedDeposit fd = fdRepo.findById(id).orElse(null);
        if (fd != null) {
            fd.setStatus("PENDING_MANAGER_APPROVAL");
            fdRepo.save(fd);
            // Notify customer
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                Notification notif = new Notification();
                notif.setMessage("Your FD application of ₹" + fd.getPrincipalAmount()
                        + " has been verified by staff and sent for Manager approval.");
                notif.setDateTime(LocalDateTime.now());
                notif.setUser(u);
                notificationRepository.save(notif);
            }
            redirectAttributes.addFlashAttribute("success", "FD Verified and sent to Manager.");
        }
        return "redirect:/staff/pending-fds";
    }

    @PostMapping("/staff/reject-fd/{id}")
    public String rejectFd(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
        com.example.bank.repository.FixedDepositRepository fdRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.FixedDepositRepository.class);
        com.example.bank.entity.FixedDeposit fd = fdRepo.findById(id).orElse(null);
        if (fd != null) {
            fd.setStatus("REJECTED");
            fdRepo.save(fd);
            com.example.bank.entity.User u = userRepository.findById(fd.getCustomerId()).orElse(null);
            if (u != null) {
                Notification notif = new Notification();
                notif.setMessage("Your FD application of ₹" + fd.getPrincipalAmount() + " has been rejected by staff.");
                notif.setDateTime(LocalDateTime.now());
                notif.setUser(u);
                notificationRepository.save(notif);
            }
            redirectAttributes.addFlashAttribute("error", "FD Application Rejected.");
        }
        return "redirect:/staff/pending-fds";
    }

    @GetMapping("/staff/fd-history")
    public String staffFdHistory(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";
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
        allFds.sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()));
        model.addAttribute("fds", allFds);
        return "staff-fd-history";
    }

    // ACTIVE CUSTOMERS LIST
    @GetMapping("/staff/attendance")
    public String markAttendanceForm(HttpSession session, Model model, RedirectAttributes ra) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null) return "redirect:/staff-login";
        
        User staff = userRepository.findById(staffId).orElse(null);
        if (staff == null || staff.getFaceData() == null || staff.getFaceData().trim().isEmpty()) {
            ra.addFlashAttribute("error", "Face ID not registered! Please register your face first.");
            return "redirect:/staff-dashboard";
        }
        
        System.out.println("DEBUG: Sending face data. Length: " + staff.getFaceData().length());
        model.addAttribute("registeredFace", staff.getFaceData());
        model.addAttribute("isCheckOut", false);
        return "staff-face-attendance";
    }

    @GetMapping("/staff/checkout")
    public String markCheckoutForm(HttpSession session, Model model, RedirectAttributes ra) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null) return "redirect:/staff-login";
        
        User staff = userRepository.findById(staffId).orElse(null);
        if (staff == null || staff.getFaceData() == null || staff.getFaceData().trim().isEmpty()) {
            ra.addFlashAttribute("error", "Face ID not registered! Please register your face first.");
            return "redirect:/staff-dashboard";
        }
        
        System.out.println("DEBUG: Sending face data for checkout. Length: " + staff.getFaceData().length());
        model.addAttribute("registeredFace", staff.getFaceData());
        model.addAttribute("isCheckOut", true);
        return "staff-face-attendance";
    }

    @GetMapping("/staff/register-face")
    public String showRegisterFace(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null) return "redirect:/staff-login";
        return "register-face";
    }

    @PostMapping("/staff/save-face")
    public String saveFace(@RequestParam String faceData, HttpSession session, RedirectAttributes ra) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null) return "redirect:/staff-login";
        
        if (faceData == null || faceData.trim().isEmpty()) {
            ra.addFlashAttribute("error", "Failed to capture face data. Please try again.");
            return "redirect:/staff/register-face";
        }

        User staff = userRepository.findById(staffId).orElse(null);
        if (staff != null) {
            staff.setFaceData(faceData);
            userRepository.save(staff);
            System.out.println("DEBUG: Face ID registered for staff: " + staffId + " (Length: " + faceData.length() + ")");
            ra.addFlashAttribute("success", "Face ID registered successfully! ✅");
        }
        return "redirect:/staff-dashboard";
    }

    @Autowired
    private com.example.bank.repository.StaffSalaryRepository salaryRepo;

    @GetMapping("/staff/salary")
    public String viewMySalary(HttpSession session, Model model) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null) return "redirect:/staff-login";
        
        java.util.List<com.example.bank.entity.StaffSalary> salaries = salaryRepo.findByStaffIdOrderByPaymentDateDesc(staffId);
        double totalEarned = salaries.stream().mapToDouble(s -> s.getNetSalary()).sum();
        
        model.addAttribute("salaries", salaries);
        model.addAttribute("totalEarned", String.format("%.2f", totalEarned));
        return "staff-salary";
    }

    @GetMapping("/staff/customers")
    public String viewActiveCustomers(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        List<AccountOpeningEntity> activeCustomers = accountOpeningRepository.findAll().stream()
                .filter(acc -> "Approved".equals(acc.getStatus()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("customers", activeCustomers);
        return "staff-customer-list";
    }

    // KYC REQUESTS FOR STAFF
    @GetMapping("/staff/kyc-requests")
    public String viewKycRequests(HttpSession session, Model model) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        List<AccountOpeningEntity> allAccs = accountOpeningRepository.findAll();

        List<AccountOpeningEntity> pendingKyc = allAccs.stream()
                .filter(acc -> "Verification Pending".equals(acc.getKycStatus()))
                .collect(java.util.stream.Collectors.toList());

        List<AccountOpeningEntity> historyKyc = allAccs.stream()
                .filter(acc -> "Verified".equals(acc.getKycStatus()) || "Rejected".equals(acc.getKycStatus()))
                .collect(java.util.stream.Collectors.toList());

        model.addAttribute("pending", pendingKyc);
        model.addAttribute("history", historyKyc);
        return "staff-kyc-requests";
    }

    @PostMapping("/staff/approve-kyc/{id}")
    public String approveKyc(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        AccountOpeningEntity acc = accountOpeningRepository.findById(id).orElse(null);
        if (acc != null) {
            acc.setKycStatus("Verified");
            accountOpeningRepository.save(acc);

            // Notify user
            if (acc.getUser() != null) {
                Notification n = new Notification();
                n.setUser(acc.getUser());
                n.setMessage("Your KYC verification has been successful! All banking features are now unlocked.");
                n.setDateTime(LocalDateTime.now());
                notificationRepository.save(n);
            }
            redirectAttributes.addFlashAttribute("success", "KYC Approved Successfully!");
        }
        return "redirect:/staff/kyc-requests";
    }

    @PostMapping("/staff/reject-kyc/{id}")
    public String rejectKyc(@PathVariable Long id, HttpSession session, RedirectAttributes redirectAttributes) {
        if (session.getAttribute("staffId") == null)
            return "redirect:/staff-login";

        AccountOpeningEntity acc = accountOpeningRepository.findById(id).orElse(null);
        if (acc != null) {
            acc.setKycStatus("Rejected");
            accountOpeningRepository.save(acc);

            if (acc.getUser() != null) {
                Notification n = new Notification();
                n.setUser(acc.getUser());
                n.setMessage("Your KYC verification was rejected. Please re-submit correct details.");
                n.setDateTime(LocalDateTime.now());
                notificationRepository.save(n);
            }
            redirectAttributes.addFlashAttribute("error", "KYC Rejected.");
        }
        return "redirect:/staff/kyc-requests";
    }

    // Haversine formula to calculate distance between two coordinates in meters
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Radius of the earth in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters
        return distance;
    }
}
