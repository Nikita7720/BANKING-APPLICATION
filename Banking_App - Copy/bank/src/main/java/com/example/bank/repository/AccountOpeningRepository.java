package com.example.bank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import com.example.bank.entity.AccountOpeningEntity;

public interface AccountOpeningRepository
    extends JpaRepository<AccountOpeningEntity, Long> {

  AccountOpeningEntity findByAccountNumber(String accountNumber);

  List<AccountOpeningEntity> findByCustomerId(String customerId);

  List<AccountOpeningEntity> findByStatus(String status);

  List<AccountOpeningEntity> findByPanAndAadhar(String pan, String aadhar);

  List<AccountOpeningEntity> findByAccountNumberAndPan(String accountNumber, String pan);

  AccountOpeningEntity findByAccountNumberAndIfscCode(String accountNumber, String ifscCode);

  AccountOpeningEntity findByUpiId(String upiId);
  AccountOpeningEntity findByUserId(Long userId);

  @Query(value = "SELECT * FROM user u JOIN account_opening ao ON u.id = ao.user_id", nativeQuery = true)
  List<Object[]> getUserAccountData();
}
