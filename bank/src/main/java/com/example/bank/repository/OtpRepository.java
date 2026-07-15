package com.example.bank.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.example.bank.entity.OtpVerification;


    public interface OtpRepository extends JpaRepository<OtpVerification, Long> {

    void deleteByEmail(String email);

    OtpVerification findByEmail(String email);

    OtpVerification findTopByEmailOrderByIdDesc(String email);

}
    

