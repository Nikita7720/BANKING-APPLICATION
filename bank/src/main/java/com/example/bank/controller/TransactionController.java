package com.example.bank.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import com.example.bank.service.TransactionService;
@Controller
public class TransactionController {

   

    @GetMapping("/transfer/{accNo}")
    public String showTransferPage(@PathVariable String accNo, Model model){

    model.addAttribute("senderAcc", accNo);

    return "transfer";
    }
     @Autowired
    private TransactionService transactionService;

    @PostMapping("/transfer")
    public String transfer(String senderAcc,
                       String receiverAcc,
                       double amount,
                       String pin,
                       Model model){

    String msg = transactionService.transferMoney(senderAcc, receiverAcc, amount);

    model.addAttribute("msg", msg);

    return "transfer";

}

   /* * @GetMapping("/transfer")
    public String showTransferPage() {
        return "transfer";
    }

    @PostMapping("/transfer")
    public String transferMoney(
            @RequestParam String senderAcc,
            @RequestParam String receiverAcc,
            @RequestParam double amount,
            Model model) {

        String msg = transactionService.transferMoney(senderAcc, receiverAcc, amount);
        model.addAttribute("msg", msg);

        return "transfer";
    }*/

    public TransactionService getTransactionService() {
        return transactionService;
    }

    public void setTransactionService(TransactionService transactionService) {
        this.transactionService = transactionService;
    }
}