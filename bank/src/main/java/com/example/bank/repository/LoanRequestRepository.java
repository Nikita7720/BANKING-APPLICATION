package com.example.bank.repository;

import com.example.bank.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
    List<LoanRequest> findByCustomerId(Long customerId);
    List<LoanRequest> findByStatus(String status);
}
