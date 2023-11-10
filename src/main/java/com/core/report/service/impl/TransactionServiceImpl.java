package com.core.report.service.impl;

import com.core.report.entities.Transaction;
import com.core.report.repositories.TransactionRepository;
import com.core.report.service.TransactionService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
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

    private static Criteria getTransactionsFilterCriteria(Transaction transaction) {
        Criteria criteria = Criteria.where("username").is(transaction.getUsername())
                .and("roundId").is(transaction.getRoundId());

        if (StringUtils.isNotEmpty(transaction.getProductId())) {
            criteria = criteria.and("productId").is(transaction.getProductId());
        }

        if (StringUtils.isNotEmpty(transaction.getUpline())) {
            criteria = criteria.and("upline").is(transaction.getUpline());
        }
        return criteria;
    }

    private static List<Bson> getAggregationPipelineForTransactions(Criteria criteria) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(criteria.getCriteriaObject()),
                Aggregates.group("$roundId",
                        Accumulators.last("actionType", "$actionType"),
                        Accumulators.last("action", "$action"),
                        Accumulators.first("beforeBalance", "$beforeBalance"),
                        Accumulators.last("afterBalance", "$afterBalance"),
                        Accumulators.first("doc", "$$ROOT"),
                        Accumulators.first("isFeatureBuy", "$isFeatureBuy"),
                        Accumulators.last("endRound", "$endRound"),
                        Accumulators.last("createdAt", "$createdAt"),
                        Accumulators.sum("amountDeposit", new Document("$cond", Arrays.asList(new Document("$eq", Arrays.asList("$actionType", "deposit")), "$amount", 0))),
                        Accumulators.sum("amountWithdraw", new Document("$cond", Arrays.asList(new Document("$eq", Arrays.asList("$actionType", "withdraw")), "$amount", 0))),
                        Accumulators.sum("betTransferOut", new Document("$cond", Arrays.asList(new Document("$eq", Arrays.asList("$actionType", "betTransferOut")), "$amount", 0))),
                        Accumulators.sum("betTransferIn", new Document("$cond", Arrays.asList(new Document("$eq", Arrays.asList("$actionType", "betTransferIn")), "$amount", 0))),
                        Accumulators.sum("betAmount", new Document("$cond", Arrays.asList(new Document("$or", List.of(new Document("$eq", Arrays.asList("$actionType", "payOut")))), "$amount", 0))),
                        Accumulators.sum("betResult", new Document("$cond", Arrays.asList(new Document("$or", List.of(new Document("$eq", Arrays.asList("$actionType", "payIn")))), "$amount", 0)))
                ),
                Aggregates.project(
                        Projections.fields(
                                Projections.excludeId(),
                                Projections.computed("username", "$doc.username"),
                                Projections.computed("upline", "$doc.upline"),
                                Projections.computed("refSale", "$doc.refSale"),
                                Projections.include("beforeBalance", "amountDeposit", "amountWithdraw", "betTransferOut", "betTransferIn", "betAmount", "betResult", "afterBalance"),
                                Projections.computed("gameId", "$doc.gameId"),
                                Projections.computed("gameName", "$doc.gameName"),
                                Projections.computed("gameCategory", "$doc.gameCategory"),
                                Projections.computed("productId", "$doc.productId"),
                                Projections.computed("productName", "$doc.productName"),
                                Projections.computed("provider", "$doc.provider"),
                                Projections.computed("percentage", "$doc.percentage"),
                                Projections.computed("createdAt", "$doc.createdAt"),
                                Projections.computed("createdIso", "$doc.createdIso"),
                                Projections.computed("created", "$doc.created"),
                                Projections.include("actionType", "action"),
                                Projections.computed("description", "$doc.description"),
                                Projections.computed("txnid", "$doc._id"),
                                Projections.computed("ip", "$doc.ip"),
                                Projections.include("isFeatureBuy", "endRound")
                        )
                )
        );
        return pipeline;
    }

    @Override
    public void processTransaction(Transaction transaction) {
        transactionRepository.save(transaction);
        Criteria criteria = getTransactionsFilterCriteria(transaction);

        List<Bson> pipeline = getAggregationPipelineForTransactions(criteria);
        List<Document> combinedResults = new ArrayList<>();
        getAggreatedDocuments("transactions", pipeline, combinedResults);
        createTransactionReport(transaction, combinedResults);
    }

    private void getAggreatedDocuments(String collection, List<Bson> pipeline, List<Document> combinedResults) {
        MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection(collection);
        MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            combinedResults.add(cursor.next());
        }
        cursor.close();
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
