package com.example.bank.repository;

import com.example.bank.entity.ServiceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ServiceRequestRepository extends JpaRepository<ServiceRequest, Long> {
    List<ServiceRequest> findByCustomerId(Long customerId);
    List<ServiceRequest> findByStatus(String status);
}
