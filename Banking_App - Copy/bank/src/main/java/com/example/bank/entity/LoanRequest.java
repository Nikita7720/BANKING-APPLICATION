package com.example.bank.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class LoanRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private String accountNumber;
    private Double amount;
    private String purpose;
    private Integer durationMonths;
    private String status = "PENDING"; // PENDING, STAFF_VERIFIED, MANAGER_APPROVED, REJECTED
    private String remark;
    private LocalDateTime requestDate;
    
    private LocalDateTime approvalDate;
    private Double interestRate;
    private Double emiAmount;
    private java.time.LocalDate nextEmiDate;

    private String employmentType; // Salaried, Business
    private String pan;
    private String aadhar;
    
    @Transient
    private String customerName;
    @Transient
    private String customerEmail;
    @Transient
    private String ifscCode;

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }
    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
    public String getIfscCode() { return ifscCode; }
    public void setIfscCode(String ifscCode) { this.ifscCode = ifscCode; }
    
    private Double monthlyIncome;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] document; // Salary slip or Bank statement

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] aadharCardPhoto;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] panCardPhoto;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] userPhoto;

    @Lob
    @Column(columnDefinition = "LONGBLOB")
    private byte[] signature;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }
    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }
    public String getPurpose() { return purpose; }
    public void setPurpose(String purpose) { this.purpose = purpose; }
    public Integer getDurationMonths() { return durationMonths; }
    public void setDurationMonths(Integer durationMonths) { this.durationMonths = durationMonths; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getRequestDate() { return requestDate; }
    public void setRequestDate(LocalDateTime requestDate) { this.requestDate = requestDate; }
    public LocalDateTime getApprovalDate() { return approvalDate; }
    public void setApprovalDate(LocalDateTime approvalDate) { this.approvalDate = approvalDate; }
    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }
    public Double getEmiAmount() { return emiAmount; }
    public void setEmiAmount(Double emiAmount) { this.emiAmount = emiAmount; }
    public java.time.LocalDate getNextEmiDate() { return nextEmiDate; }
    public void setNextEmiDate(java.time.LocalDate nextEmiDate) { this.nextEmiDate = nextEmiDate; }
    public String getEmploymentType() { return employmentType; }
    public void setEmploymentType(String employmentType) { this.employmentType = employmentType; }
    public String getPan() { return pan; }
    public void setPan(String pan) { this.pan = pan; }
    public String getAadhar() { return aadhar; }
    public void setAadhar(String aadhar) { this.aadhar = aadhar; }
    public Double getMonthlyIncome() { return monthlyIncome; }
    public void setMonthlyIncome(Double monthlyIncome) { this.monthlyIncome = monthlyIncome; }
    public byte[] getDocument() { return document; }
    public void setDocument(byte[] document) { this.document = document; }
    public byte[] getAadharCardPhoto() { return aadharCardPhoto; }
    public void setAadharCardPhoto(byte[] aadharCardPhoto) { this.aadharCardPhoto = aadharCardPhoto; }
    public byte[] getPanCardPhoto() { return panCardPhoto; }
    public void setPanCardPhoto(byte[] panCardPhoto) { this.panCardPhoto = panCardPhoto; }
    public byte[] getUserPhoto() { return userPhoto; }
    public void setUserPhoto(byte[] userPhoto) { this.userPhoto = userPhoto; }
    public byte[] getSignature() { return signature; }
    public void setSignature(byte[] signature) { this.signature = signature; }
}
