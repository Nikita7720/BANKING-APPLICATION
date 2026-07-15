package com.example.bank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.repository.AccountOpeningRepository;

 
   
@Service
public class AccontOpeningService {

    @Autowired
    AccountOpeningRepository repo;

    public void saveAccount(AccountOpeningEntity a){

        

repo.save(a);
System.out.println("DATA SAVED");

}

}