package com.core.report.repositories;

import com.core.report.entities.WinLossTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface WinLossRepository extends MongoRepository<WinLossTransaction, String> {
    // You can add custom query methods here if needed
}
