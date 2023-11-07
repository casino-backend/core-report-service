package com.core.report.repositories;

import com.core.report.entities.Transaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TransactionRepository extends MongoRepository<Transaction, String> {
    // You can add custom query methods here if needed
}
