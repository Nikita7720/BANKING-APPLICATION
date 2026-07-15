package com.example.bank.repository;

import com.example.bank.entity.QueryMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface QueryMessageRepository extends JpaRepository<QueryMessage, Long> {
    List<QueryMessage> findByReceiverId(Long receiverId);
    List<QueryMessage> findByTargetRole(String targetRole);
}
