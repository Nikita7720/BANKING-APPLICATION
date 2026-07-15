package com.example.bank.repository;

import com.example.bank.entity.FixedDeposit;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FixedDepositRepository extends JpaRepository<FixedDeposit, Long> {
    List<FixedDeposit> findByCustomerId(Long customerId);
    List<FixedDeposit> findByCustomerIdAndStatus(Long customerId, String status);
    List<FixedDeposit> findByStatus(String status);
}
