package com.example.bank.runner;

import com.example.bank.entity.Transaction;
import com.example.bank.repository.TransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TransactionDataCleaner implements CommandLineRunner {

    private static final Logger logger = LoggerFactory.getLogger(TransactionDataCleaner.class);
    private final TransactionRepository transactionRepository;

    public TransactionDataCleaner(TransactionRepository transactionRepository) {
        this.transactionRepository = transactionRepository;
    }

    @Override
    public void run(String... args) {
        logger.info("Cleaning existing transaction account numbers (trimming whitespace)...");
        List<Transaction> all = transactionRepository.findAll();
        int updated = 0;
        for (Transaction t : all) {
            String acc = t.getAccountNumber();
            if (acc != null) {
                String trimmed = acc.trim();
                if (!acc.equals(trimmed)) {
                    t.setAccountNumber(trimmed);
                    transactionRepository.save(t);
                    updated++;
                }
            }
        }
        logger.info("Transaction cleaning completed. Updated {} records.", updated);
    }
}
