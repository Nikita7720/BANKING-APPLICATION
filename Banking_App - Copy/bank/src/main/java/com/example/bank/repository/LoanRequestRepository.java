package com.example.bank.repository;

import com.example.bank.entity.LoanRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LoanRequestRepository extends JpaRepository<LoanRequest, Long> {
    List<LoanRequest> findByCustomerId(Long customerId);
    List<LoanRequest> findByStatus(String status);
}
