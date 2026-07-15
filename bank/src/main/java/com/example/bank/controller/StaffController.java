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

    // Hard‑coded branch coordinates (example: 19.949826, 73.838928)

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountOpeningRepository accountOpeningRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private JavaMailSender mailSender;
    // Maximum allowed distance (in metres) from the branch for attendance checks
    // Realistic limit: 200 meters
    private static final double MAX_DISTANCE_METERS = 200; // 200 m limit (realistic max distance for check‑in/out)
    // Hard‑coded branch coordinates (used for distance validation)
    private static final double BRANCH_LAT = 19.949749;
    private static final double BRANCH_LON = 73.839256;

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
            return "redirect:/staff-login";
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
            String accNumber = "NEXA" + (int) (Math.random() * 1000000);
            String ifsc = "NEXA0001234";
            String tempPassword = UUID.randomUUID().toString().substring(0, 8);

            account.setAccountNumber(accNumber);
            account.setIfscCode(ifsc);
            account.setStatus("Approved");
            account.setBalance(0.0);

            if (account.getUser() != null) {
                User u = account.getUser();
                u.setAccountNumber(accNumber);
                u.setIfscCode(ifsc);
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

            // Generate OTP for account details
            String otp = String.format("%06d", (int) (Math.random() * 1000000));
            // Send Email with OTP, account number and IFSC
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(account.getEmail());
            message.setSubject("Account Approved - NexaBank");
            message.setText("Congratulations! Your account is approved.\n" +
                    "Account No: " + accNumber + "\n" +
                    "IFSC: " + ifsc + "\n" +
                    "OTP: " + otp + "\n" +
                    "Please use this OTP to access your account details.");
            mailSender.send(message);

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
                if (u.getAccounts() != null && !u.getAccounts().isEmpty()) {
                    loan.setIfscCode(u.getAccounts().get(0).getIfscCode());
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
                if (u.getAccounts() != null && !u.getAccounts().isEmpty()) {
                    loan.setIfscCode(u.getAccounts().get(0).getIfscCode());
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

    @GetMapping("/staff/face-attendance")
    public String faceAttendance(HttpSession session, Model model) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null)
            return "redirect:/staff-login";

        com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.AttendanceRepository.class);
        java.util.Optional<com.example.bank.entity.Attendance> todayAtt = attRepo
                .findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staffId, java.time.LocalDate.now());

        if (todayAtt.isPresent() && todayAtt.get().getCheckOutTime() == null) {
            model.addAttribute("isCheckOut", true);
        } else {
            model.addAttribute("isCheckOut", false);
        }

        return "staff-face-attendance";
    }

    @PostMapping("/staff/mark-attendance")
    public String markAttendance(
            @RequestParam(required = false) String faceData,
            @RequestParam double latitude,
            @RequestParam(required = false) String location,
            HttpSession session, RedirectAttributes redirectAttributes) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null)
            return "redirect:/staff-login";

        // Prefer latitude/longitude parameters, fallback to parsed location string if they are zero
        double staffLat = latitude;
        double staffLon = 0.0;
        if ((staffLat == 0.0 && staffLon == 0.0) && location != null && !location.isEmpty()) {
            String[] parts = location.split(",");
            if (parts.length == 2) {
                try {
                    staffLat = Double.parseDouble(parts[0].trim());
                    staffLon = Double.parseDouble(parts[1].trim());
                } catch (NumberFormatException e) {
                    // keep defaults and let distance check fail
                }
            }
        }

        // Compute distance (km) using the branch coordinates supplied by the client
        double distanceKm = haversineDistance(staffLat, staffLon, BRANCH_LAT, BRANCH_LON);
        double distanceMeters = distanceKm * 1000.0;
        // Debug logging – useful for verifying distance calculations
        System.out.println("[DEBUG] Staff location: " + staffLat + "," + staffLon);
        System.out.println("[DEBUG] Branch location: " + BRANCH_LAT + "," + BRANCH_LON);
        System.out.println("[DEBUG] Calculated distance (m): " + distanceMeters);

        if (distanceMeters > MAX_DISTANCE_METERS) {
            // Include the allowed radius in the message for clarity
            String msg = String.format("Out of Workspace (%d m away). Please be within %d m of the branch.",
                    (int) distanceMeters, (int) MAX_DISTANCE_METERS);
            redirectAttributes.addFlashAttribute("error", msg);
            return "redirect:/staff/face-attendance";
        }

        User staff = userRepository.findById(staffId).orElse(null);
        if (staff != null) {
            com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                    .getWebApplicationContext(session.getServletContext())
                    .getBean(com.example.bank.repository.AttendanceRepository.class);

            // Check if already checked in and not checked out
            java.util.Optional<com.example.bank.entity.Attendance> existingAtt = attRepo
                    .findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staffId, java.time.LocalDate.now());
            if (existingAtt.isPresent() && existingAtt.get().getCheckOutTime() == null) {
                redirectAttributes.addFlashAttribute("error", "You are already checked in. Please check out first.");
                return "redirect:/staff-dashboard";
            }

            com.example.bank.entity.Attendance attendance = new com.example.bank.entity.Attendance();
            attendance.setStaffId(staffId);
            attendance.setEmployeeId(staff.getEmployeeId());
            attendance.setAttendanceDate(java.time.LocalDate.now());
            // Store location as "lat,lon" string for reference
            attendance.setLocation(String.format("%f,%f", staffLat, staffLon));
            // Also store latitude and longitude
            attendance.setLatitude(staffLat);
            attendance.setLongitude(staffLon);
            attendance.setStatus("PRESENT");

            attRepo.save(attendance);

            // Log to console (in real app, we might verify faceData here)
            System.out.println("Face Attendance Marked for: " + staff.getEmployeeId());

            redirectAttributes.addFlashAttribute("success", "Attendance Marked Successfully!");
        }
        return "redirect:/staff-dashboard";
    }

    @PostMapping("/staff/mark-checkout")
    public String markCheckout(@RequestParam String location,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        Long staffId = (Long) session.getAttribute("staffId");
        if (staffId == null)
            return "redirect:/staff-login";

        // ------------ Location validation (same as check‑in) ------------
        if (location == null || location.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Location is required.");
            return "redirect:/staff/face-attendance";
        }
        String[] parts = location.split(",");
        if (parts.length != 2) {
            redirectAttributes.addFlashAttribute("error", "Invalid location format.");
            return "redirect:/staff/face-attendance";
        }
        double staffLat;
        double staffLon;
        try {
            staffLat = Double.parseDouble(parts[0].trim());
            staffLon = Double.parseDouble(parts[1].trim());
        } catch (NumberFormatException e) {
            redirectAttributes.addFlashAttribute("error", "Unable to parse location coordinates.");
            return "redirect:/staff/face-attendance";
        }
        // Use class-level branch coordinates and max distance

        double distanceKm = haversineDistance(staffLat, staffLon, BRANCH_LAT, BRANCH_LON);
        double distanceMeters = distanceKm * 1000.0;
        System.out.println("[DEBUG] Checkout Staff location: " + staffLat + "," + staffLon);
        System.out.println("[DEBUG] Checkout Branch location: " + BRANCH_LAT + "," + BRANCH_LON);
        System.out.println("[DEBUG] Checkout Distance meters: " + distanceMeters);

        if (distanceMeters > MAX_DISTANCE_METERS) {
            redirectAttributes.addFlashAttribute("error",
                    "Out of Workspace (" + (int) distanceMeters + "m away). Go to Bank Branch.");
            return "redirect:/staff/face-attendance";
        }
        // --------------------------------------------------------------

        com.example.bank.repository.AttendanceRepository attRepo = org.springframework.web.context.support.WebApplicationContextUtils
                .getWebApplicationContext(session.getServletContext())
                .getBean(com.example.bank.repository.AttendanceRepository.class);

        java.util.Optional<com.example.bank.entity.Attendance> todayAtt = attRepo
                .findFirstByStaffIdAndAttendanceDateOrderByCheckInTimeDesc(staffId, java.time.LocalDate.now());
        if (todayAtt.isPresent() && todayAtt.get().getCheckOutTime() == null) {
            com.example.bank.entity.Attendance attendance = todayAtt.get();
            attendance.setCheckOutTime(java.time.LocalTime.now());
            // Store checkout location and coordinates
            attendance.setLocation(location);
            attendance.setLatitude(staffLat);
            attendance.setLongitude(staffLon);
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

    // Helper method to calculate distance between two lat/lon points using the
    // Haversine formula
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371.01; // Earth's radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c; // distance in kilometers
    }

}
