package com.core.report.service.impl;

import com.core.report.repositories.TransactionRepository;
import com.core.report.service.TransactionService;
import com.core.report.entities.Transaction;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
public class TransactionServiceImpl implements TransactionService {
    @Autowired
    private TransactionRepository transactionRepository;
    private MongoCollection<Document> transactionCollection;

    @Autowired
    private MongoTemplate mongoTemplate;
    @Override
    public void processTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
        Criteria criteria = Criteria.where("username").is(transaction.getUsername())
                .and("roundId").is(transaction.getRoundId());

        if (StringUtils.isNotEmpty(transaction.getProductId())) {
            criteria = criteria.and("productId").is(transaction.getProductId());
        }

        if (StringUtils.isNotEmpty(transaction.getUpline())) {
            criteria = criteria.and("upline").is(transaction.getUpline());
        }

// Create a MatchOperation with the criteria
        MatchOperation matchOperation = Aggregation.match(criteria);

        GroupOperation groupOperation = Aggregation.group("roundId")
                .last("actionType").as("actionType")
                .last("action").as("action")
                .first("beforeBalance").as("beforeBalance")
                .last("afterBalance").as("afterBalance")
                .first("$$ROOT").as("doc")
                .first("isFeatureBuy").as("isFeatureBuy")
                .last("endRound").as("endRound")
                .last("createdAt").as("createdAt")
                .sum(ConditionalOperators.when(ComparisonOperators.valueOf("actionType").equalToValue("deposit"))
                        .then("amount").otherwise(0)).as("amountDeposit")
                .sum(ConditionalOperators.when(ComparisonOperators.valueOf("actionType").equalToValue("withdraw"))
                        .then("amount").otherwise(0)).as("amountWithdraw")
                .sum(ConditionalOperators.when(ComparisonOperators.valueOf("actionType").equalToValue("betTransferOut"))
                        .then("amount").otherwise(0)).as("betTransferOut")
                .sum(ConditionalOperators.when(ComparisonOperators.valueOf("actionType").equalToValue("betTransferIn"))
                        .then("amount").otherwise(0)).as("betTransferIn")
                .sum(ConditionalOperators.when(Criteria.where("actionType").in("payOut", "payIn"))
                        .then("amount").otherwise(0)).as("betAmount")
                .sum(ConditionalOperators.when(Criteria.where("actionType").in("payOut", "payIn"))
                        .then("amount").otherwise(0)).as("betResult");


        List<String> includedFields = Arrays.asList(
                "username",
                "upline",
                "refSale",
                "beforeBalance",
                "amountDeposit",
                "amountWithdraw",
                "betTransferOut",
                "betTransferIn",
                "betAmount",
                "betResult",
                "afterBalance",
                "gameId",
                "gameName",
                "gameCategory",
                "productId",
                "productName",
                "provider",
                "percentage",
                "createdAt",
                "createdIso",
                "created",
                "actionType",
                "action",
                "description",
                "txnid",
                "ip",
                "isFeatureBuy",
                "endRound"
        );

        ProjectionOperation projectionOperation = Aggregation.project(includedFields.toArray(new String[]{}));

        Aggregation aggregation=Aggregation.newAggregation(matchOperation,groupOperation,projectionOperation);
        AggregationResults<Document> transactions = mongoTemplate.aggregate(aggregation, "transactions", Document.class);

        createTransactionReport(transaction,transactions.getMappedResults());
    }


    public void createTransactionReport(Transaction latestTransaction, List<Document> transactions) {
        List<Document> reports = new ArrayList<>();

        for (Document key : transactions) {
            String roundId = key.getString("roundId").replace(" ", "");
            String username = key.getString("username").replace(" ", "");

            Double betAmount = key.getDouble("betAmount");
            Double betTransferIn = key.getDouble("betTransferIn");
            Double betTransferOut = key.getDouble("betTransferOut");

            String productId = key.getString("productId").replace(" ", "");
            String productName = key.getString("productName");
            String gameCategory = key.getString("gameCategory");
            String provider = key.getString("provider");
            boolean endRound = key.getBoolean("endRound");

            Double betWinLoss = key.getDouble("betResult") - betAmount;
            Double winLossInOut = betTransferIn - betTransferOut;
            Double memberWinLoss = betWinLoss + winLossInOut;
            Double percentage = key.getDouble("percentage");
            Double uplineWinLoss = (betWinLoss / 100) * percentage * -1;
            String status = "";

            if (productId.equals("withdrawal")) {
                betWinLoss = 0.0;
                uplineWinLoss = 0.0;
                status = "OK";
            } else {
                if (!endRound) {
                    status = "Running";
                } else {
                    if (betWinLoss == 0) {
                        status = "Eq";
                    } else if (betWinLoss > 0) {
                        status = "Win";
                    } else {
                        status = "Loss";
                    }
                }

                if (latestTransaction.isEndRound()) {
                    if (betWinLoss == 0) {
                        status = "Eq";
                    } else if (betWinLoss > 0) {
                        status = "Win";
                    } else {
                        status = "Loss";
                    }
                }
            }

            String playDate = key.getString("createdAt");
            String created = key.getString("created");
            String createdISO = key.getString("createdISO");
            String refSale = key.getString("refSale");

            if (refSale == null || refSale.isEmpty()) {
                refSale = "";
            }

            Document report = new Document("roundId", roundId)
                    .append("username", username)
                    .append("actionType", key.getInteger("actionType"))
                    .append("action", key.getString("action"))
                    .append("description", key.getString("description"))
                    .append("beforeBalance", key.getDouble("beforeBalance"))
                    .append("amountDeposit", key.getDouble("amountDeposit"))
                    .append("amountWithdraw", key.getDouble("amountWithdraw"))
                    .append("betAmount", betAmount)
                    .append("betResult", key.getDouble("betResult"))
                    .append("betWinLoss", betWinLoss)
                    .append("memberWinLoss", memberWinLoss)
                    .append("betTransferIn", betTransferIn)
                    .append("betTransferOut", betTransferOut)
                    .append("afterBalance", key.getDouble("afterBalance"))
                    .append("status", status)
                    .append("txnid", key.getString("txnid"))
                    .append("refSale", refSale)
                    .append("upline", key.getString("upline"))
                    .append("uplineWinLoss", uplineWinLoss)
                    .append("gameId", key.getString("gameId"))
                    .append("gameName", key.getString("gameName"))
                    .append("gameCategory", gameCategory)
                    .append("productId", productId)
                    .append("productName", productName)
                    .append("provider", provider)
                    .append("percentage", percentage)
                    .append("isFeatureBuy", key.getInteger("isFeatureBuy"))
                    .append("endRound", endRound)
                    .append("created", created)
                    .append("createdAt", Instant.now())
                    .append("createdISO", createdISO)
                    .append("ip", "")
                    .append("playDate", playDate);

            // timeBet, timeSettle
            reports.add(report);

            Document filter = new Document("username", latestTransaction.getUsername())
                    .append("roundId", latestTransaction.getRoundId())
                    .append("productId", latestTransaction.getProductId());

            Document update = new Document("$set", report);

            UpdateOptions updateOptions = new UpdateOptions();
            updateOptions.upsert(true);

            transactionCollection.updateOne(filter, update, updateOptions);

            transactionCollection.updateOne(Filters.and(Filters.eq("username", latestTransaction.getUsername()), Filters.eq("roundId", latestTransaction.getRoundId())), update);
            String pattern = "yyyy-MM-dd HH:mm:ss"; // Specify your date pattern

            SimpleDateFormat dateFormat = new SimpleDateFormat(pattern);
            Date date = null;

            try {
                date = dateFormat.parse(playDate);
            } catch (ParseException e) {
                e.printStackTrace();
                // Handle the parse exception as needed
            }


            Date now = new Date();

            long durationMillis = now.getTime() - date.getTime();
            long playdateDiff = TimeUnit.MILLISECONDS.toDays(durationMillis);

            if (playdateDiff > 0) {
                transactionCollection.updateOne(Filters.and(Filters.eq("username", latestTransaction.getUsername()), Filters.eq("roundId", latestTransaction.getRoundId())), update);
            }
        }
    }
}
