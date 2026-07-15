package com.example.bank.repository;

import com.example.bank.entity.QueryEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface QueryRepository extends JpaRepository<QueryEntity, Long> {
    List<QueryEntity> findByCustomerId(Long customerId);
    List<QueryEntity> findByStatus(String status);
}
