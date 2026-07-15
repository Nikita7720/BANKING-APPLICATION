package com.example.bank.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.bank.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {

    // 🔥 login sathi
    List<User> findByEmailAndPassword(String email, String password);
    
    User findByEmail(String email);
    
    User findByEmployeeId(String employeeId);
}



    

