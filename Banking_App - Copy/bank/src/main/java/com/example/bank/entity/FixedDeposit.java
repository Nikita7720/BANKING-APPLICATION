package com.example.bank.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "fixed_deposits")
public class FixedDeposit {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long customerId;
    private String accountNumber;
    private String fdNumber;

    private Double principalAmount;
    private Integer tenureMonths;
    private Double interestRate;
    private Double maturityAmount;

    private LocalDate startDate;
    private LocalDate maturityDate;

    private String status = "ACTIVE"; // ACTIVE, MATURED, CLOSED

    private LocalDateTime createdAt;

    // Nominee
    private String nomineeName;
    private String nomineeRelation;

    @jakarta.persistence.Transient
    private String customerName;
    @jakarta.persistence.Transient
    private String customerEmail;

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getCustomerId() { return customerId; }
    public void setCustomerId(Long customerId) { this.customerId = customerId; }

    public String getAccountNumber() { return accountNumber; }
    public void setAccountNumber(String accountNumber) { this.accountNumber = accountNumber; }

    public String getFdNumber() { return fdNumber; }
    public void setFdNumber(String fdNumber) { this.fdNumber = fdNumber; }

    public Double getPrincipalAmount() { return principalAmount; }
    public void setPrincipalAmount(Double principalAmount) { this.principalAmount = principalAmount; }

    public Integer getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(Integer tenureMonths) { this.tenureMonths = tenureMonths; }

    public Double getInterestRate() { return interestRate; }
    public void setInterestRate(Double interestRate) { this.interestRate = interestRate; }

    public Double getMaturityAmount() { return maturityAmount; }
    public void setMaturityAmount(Double maturityAmount) { this.maturityAmount = maturityAmount; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getMaturityDate() { return maturityDate; }
    public void setMaturityDate(LocalDate maturityDate) { this.maturityDate = maturityDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public String getNomineeName() { return nomineeName; }
    public void setNomineeName(String nomineeName) { this.nomineeName = nomineeName; }

    public String getNomineeRelation() { return nomineeRelation; }
    public void setNomineeRelation(String nomineeRelation) { this.nomineeRelation = nomineeRelation; }

    public String getCustomerName() { return customerName; }
    public void setCustomerName(String customerName) { this.customerName = customerName; }

    public String getCustomerEmail() { return customerEmail; }
    public void setCustomerEmail(String customerEmail) { this.customerEmail = customerEmail; }
}
