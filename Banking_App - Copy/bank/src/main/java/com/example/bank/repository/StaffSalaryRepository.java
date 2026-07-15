package com.example.bank.repository;

import com.example.bank.entity.StaffSalary;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StaffSalaryRepository extends JpaRepository<StaffSalary, Long> {
    List<StaffSalary> findByStaffIdOrderByPaymentDateDesc(Long staffId);
}
