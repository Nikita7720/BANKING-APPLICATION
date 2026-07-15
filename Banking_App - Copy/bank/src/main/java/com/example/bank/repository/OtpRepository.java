package com.example.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.example.bank.entity.OtpVerification;

@Repository
public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    void deleteByEmail(String email);

    OtpVerification findByEmail(String email);

    OtpVerification findTopByEmailOrderByIdDesc(String email);

}
