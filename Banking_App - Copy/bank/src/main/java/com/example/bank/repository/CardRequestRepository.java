package com.example.bank.repository;

import com.example.bank.entity.CardRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CardRequestRepository extends JpaRepository<CardRequest, Long> {
    List<CardRequest> findByUserId(Long userId);
    List<CardRequest> findByStatus(String status);
}
