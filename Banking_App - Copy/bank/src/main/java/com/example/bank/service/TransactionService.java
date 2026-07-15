package com.example.bank.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.bank.entity.AccountOpeningEntity;
import com.example.bank.repository.AccountOpeningRepository;

import jakarta.transaction.Transactional;

@Service
@Transactional   // VERY IMPORTANT
public class TransactionService {

    @Autowired
    private AccountOpeningRepository accountRepository;

    public String transferMoney(String senderAcc, String receiverAcc, double amount) {
        AccountOpeningEntity sender = accountRepository.findByAccountNumber(senderAcc);
        
        AccountOpeningEntity receiver = null;
        if (receiverAcc != null && receiverAcc.contains("@")) {
            receiver = accountRepository.findByUpiId(receiverAcc);
        } else {
            receiver = accountRepository.findByAccountNumber(receiverAcc);
        }

        if (sender != null && receiver != null && sender.getAccountNumber().equals(receiver.getAccountNumber())) {
            return "Same account transfer not allowed";
        }

        if(sender == null || receiver == null){
            return "Invalid account number";
        }
            if(sender.getKycStatus() == null || 
   !sender.getKycStatus().equals("Verified")){
    return "Complete KYC first ❌";
}

        if(amount <= 0){
            return "Invalid amount";
        }

        if(sender.getBalance() < amount){
            return "Insufficient balance";
        }

        // Transfer logic
        sender.setBalance(sender.getBalance() - amount);
        receiver.setBalance(receiver.getBalance() + amount);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        return "Money transferred successfully";
    }


}