package com.example.bank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.Transaction;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {

    List<Transaction> findByAccountNumberOrderByDateTimeDesc(String accountNumber);

    List<Transaction> findByDateTimeAfter(java.time.LocalDateTime dateTime);
}