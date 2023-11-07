package com.core.report.service.impl;

import com.core.report.constants.Constants;
import com.core.report.dto.*;
import com.core.report.client.MemberClient;
import com.core.report.service.WinlossAgentService;


import com.core.report.utils.Transformer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Accumulators;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Service
@Slf4j
@AllArgsConstructor
public class WinlossAgentServiceImpl implements WinlossAgentService {

    private MongoTemplate mongoTemplate;
    private MemberClient memberClient;
@Override
    public void sumWinLossAgent(Date oldStartDate, Date oldEndDate, String upline) throws Exception {
        String startDate, endDate;
        SimpleDateFormat dateTimeFormatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        if (oldStartDate != null && oldEndDate != null) {
            startDate = dateTimeFormatter.format(oldStartDate);
            endDate = dateTimeFormatter.format(oldEndDate);
        } else {
            Date now = new Date();
            startDate = dateTimeFormatter.format(getStartOfDay(now));
            endDate = dateTimeFormatter.format(getEndOfDay(now));
        }

        List<Document> pipeline = new ArrayList<>();

        List<Document> matchFilter = new ArrayList<>();
        Document dateFilter = new Document("playdate", new Document("$gte", startDate).append("$lte", endDate));
        matchFilter.add(dateFilter);

        if (!upline.isEmpty()) {
            Document uplineFilter = new Document("upline", upline);
            matchFilter.add(uplineFilter);
        }

        Document andCondition = new Document("$and", matchFilter);
        pipeline.add(new Document("$match", andCondition));

        Document groupFields = new Document("_id", new Document("upline", "$upline").append("productId", "$productId"));
        groupFields.append("betCount", new Document("$sum", 1));
        groupFields.append("doc", new Document("$last", "$$ROOT"));
        groupFields.append("betAmount", new Document("$sum", "$betAmount"));
        groupFields.append("betTransferIn", new Document("$sum", "$betTransferIn"));
        groupFields.append("betTransferOut", new Document("$sum", "$betTransferOut"));
        groupFields.append("betWinloss", new Document("$sum", "$betWinloss"));
        groupFields.append("memberWinloss", new Document("$sum", "$memberWinloss"));

        pipeline.add(new Document("$group", groupFields));

        Document projectFields = new Document("betCount", 1);
        projectFields.append("refSale", "$doc.refSale");
        projectFields.append("betAmount", 1);
        projectFields.append("betTransferIn", 1);
        projectFields.append("betTransferOut", 1);
        projectFields.append("memberWinloss", 1);
        projectFields.append("betWinloss", 1);
        projectFields.append("createdAt", "$doc.createdAt");
        projectFields.append("playdate", "$doc.playdate");
        projectFields.append("productName", "$doc.productName");
        projectFields.append("gameCategory", "$doc.gameCategory");
        projectFields.append("gameProvider", "$doc.gameProvider");

        pipeline.add(new Document("$project", projectFields));
        List<Document> combinedResults=new ArrayList<>();
        MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection("winlossMembers");
        MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            combinedResults.add(cursor.next());
        }
        cursor.close();

        for (Map<String, Object> result : combinedResults) {
            String username = (String) result.get("username");
            if (username == null) {
                log.info("Error: username missing or not a string");
                continue;
            }

            String productId = (String) result.get("productId");
            if (productId == null) {
                log.info("Error: productId missing or not a string");
                continue;
            }

            GetUserRequest getAgentRequest = new GetUserRequest(username);
            GetUserResponse agentDetails = memberClient.getUser(getAgentRequest);
            if (agentDetails == null) {
                throw new Exception("failed to fetch upline games");
            }

            String userType = agentDetails.getType();
            String userUpline = agentDetails.getUpline();
            String uCompany = agentDetails.getUCompany();
            double rate = 0.0;

            for (Game game : agentDetails.getGames()) {
                if (game.getProductId().equals(productId) && game.getStatus().equals("A")) {
                    rate = game.getRate();
                    break;
                }
            }
            if (rate == 0.0) {
                throw new Exception("failed to get upline game rate");
            }

            Double memberWinloss = (Double) result.get("memberWinloss");
            if (memberWinloss == null) {
                log.info("Error: memberWinloss missing or not a float64");
                continue;
            }

            double winCompany = (memberWinloss / 100) * rate * -1;
            double winProvider = (memberWinloss / 100) * (100 - rate) * -1;

            LocalDateTime parsedStartDate = LocalDateTime.parse(startDate, Constants.DATE_TIME_FORMAT);
            LocalDateTime parsedEndDate = LocalDateTime.parse(endDate, Constants.DATE_TIME_FORMAT);
            Instant instantStartDate = parsedStartDate.atZone(ZoneId.systemDefault()).toInstant();
            Instant instantEndDate = parsedEndDate.atZone(ZoneId.systemDefault()).toInstant();

            // Convert Instant to Date
            Map<String, Object> newData = Transformer.winlossSchemaTransformer( Date.from(instantStartDate), Date.from(instantEndDate), productId, result);

            newData.put("level", 1);
            newData.put("uCompany", uCompany);
            newData.put("username", username);
            newData.put("userType", userType);
            newData.put("upline", userUpline);
            newData.put("hasMember", "");

            newData.put("rate", rate);
            newData.put("winCompany", winCompany);
            newData.put("winAgentValid", 0.0);
            newData.put("winAgentUp", 0.0);
            newData.put("winProvider", winProvider);

            Double uplineWinloss = getWinLoss(userUpline, productId, rate, memberWinloss);

            newData.put("uplineWinloss", uplineWinloss);
            // TODO: fraud check

            Bson filter = new Document("username", username)
                    .append("productId", productId)
                    .append("startDate", startDate)
                    .append("endDate", endDate);

            insertOrUpdateWinlossAgent(filter, new Document(newData));
            // Rest of your Java code here
    }


        if (upline != null && !upline.isEmpty()) {
            GetUserRequest userRequest = new GetUserRequest(upline); // Assume GetUserRequest is a defined class
            GetUserResponse userDetails = memberClient.getUser(userRequest); // Assume getUser method is defined
            if (userDetails == null) {
                throw new Exception("Failed to fetch upline games");
            }

            for (int i = 0; i < 5; i++) {
                if (!sumWinLossAgentFurther(startDate, endDate, userDetails.getUCompany())) {
                    throw new Exception("Something went wrong during summing win/loss data");
                }
            }
        } else {
            Bson match = Aggregates.match(
                    and(
                            Filters.gte("playdate", startDate),
                            Filters.lte("playdate", endDate)
                    )
            );
            Bson group = Aggregates.group("$uCompany", Accumulators.first("uCompany", "$uCompany"));

            List<Bson> pipeline1 = Arrays.asList(match, group);
            MongoCollection<Document> winlossAgentsCollection = mongoTemplate.getCollection("winlossAgents");
            MongoCursor<Document> cursor1 = winLossMemberCollection.aggregate(pipeline1).iterator();

            if (cursor1 == null) {
                throw new Exception("Failed to aggregate win/loss data");
            }

            while (cursor1.hasNext()) {
                Document result = cursor.next();
                String userCompany = result.getString("uCompany");
                if (userCompany == null) {
                    throw new Exception("Error converting u_company to string");
                }
                for (int i = 0; i < 5; i++) {


                    if (!(sumWinLossAgentFurther(startDate, endDate, userCompany))) {
                        throw new Exception("Error summing win/loss data for company: " + userCompany);
                    }
                }
            }
        }
    }


    public boolean sumWinLossAgentFurther(String startDate, String endDate, String uCompany) throws Exception {
        Criteria criteria = Criteria.where("uCompany").is(uCompany)
                .andOperator(
                        Criteria.where("upline").ne(""),
                        Criteria.where("playdate").gte(startDate).lte(endDate)
                );

        MatchOperation matchOperation = Aggregation.match(criteria);
        GroupOperation groupOperation = Aggregation.group("upline", "productId", "rate")
                .count().as("betCount")
                .last("$$ROOT").as("doc")
                .sum("betAmount").as("betAmount")
                .sum("betTransferIn").as("betTransferIn")
                .sum("betTransferOut").as("betTransferOut")
                .sum("betWinloss").as("betWinloss")
                .sum("memberWinloss").as("memberWinloss")
                .sum("winCompany").as("winCompany")
                .sum("winAgentValid").as("winAgentValid")
                .sum("winAgentUp").as("winAgentUp")
                .sum("winProvider").as("winProvider");

        ProjectionOperation projectionOperation = Aggregation.project()
                .andInclude("betCount", "betAmount", "betTransferIn", "betTransferOut", "memberWinloss", "betWinloss", "winCompany", "winAgentValid", "winAgentUp", "winProvider")
                .and("doc.refSale").as("refSale")
                .and("doc.createdAt").as("createdAt")
                .and("doc.playdate").as("playdate")
                .and("doc.productName").as("productName")
                .and("doc.gameCategory").as("gameCategory")
                .and("doc.gameProvider").as("gameProvider")
                .and("doc.level").as("level");

        Aggregation aggregation = Aggregation.newAggregation(matchOperation, groupOperation, projectionOperation);

        // Assuming the collection name is "winloss_agents" and the mapped class is WinLossAgent.class
        AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "winloss_agents", Document.class);

        // Here you would handle the results
        List<Document> winLossAgents = results.getMappedResults();

        for (Map result : results) {
            String username = (String) result.get("username");
            String productId = (String) result.get("productId");
            Double rateDownline = (Double) result.get("rateDownline");
            Integer level = (Integer) result.get("level");
            Double memberWinloss = (Double) result.get("memberWinloss");
            Double keyWinCompany = (Double) result.get("winCompany");
            Double winProvider = (Double) result.get("winProvider");

            GetUserRequest getAgentRequest = GetUserRequest.builder()
                    .username(username)
                    .build();
            GetUserResponse agentDetails = memberClient.getUser(getAgentRequest);
            String userType = agentDetails.getType();
            String userUpline = agentDetails.getUpline();

            String userCompany = userType.equals("company") ? uCompany : agentDetails.getUCompany();
            double rate = 0.0;
            for (Game game : agentDetails.getGames()) {
                if (game.getProductId().equals(productId) && game.getStatus().equals("A")) {
                    rate = game.getRate();
                    break;
                }
            }

            double rateValid = rate - rateDownline;
            double winCompany = (memberWinloss / 100) * rateValid * -1;

            LocalDateTime parsedStartDate = LocalDateTime.parse(startDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            LocalDateTime parsedEndDate = LocalDateTime.parse(endDate, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

            Instant instantStartDate = parsedStartDate.atZone(ZoneId.systemDefault()).toInstant();
            Instant instantEndDate = parsedEndDate.atZone(ZoneId.systemDefault()).toInstant();


            Map<String, Object> newData = Transformer.winlossSchemaTransformer(Date.from(instantStartDate), Date.from(instantEndDate), productId, result);

            // Setting additional fields
            newData.put("level", level);
            newData.put("uCompany", userCompany);
            newData.put("username", username);
            newData.put("userType", userType);
            newData.put("upline", userUpline);
            newData.put("rate", rate);
            newData.put("rateValid", rateValid);
            newData.put("winCompany", winCompany);
            newData.put("winAgentValid", keyWinCompany);
            newData.put("winAgentUp", winProvider);
            newData.put("winProvider", winProvider - winCompany);

            // Insert or update in winloss_agent_temps

            Bson bsonFilter = and(
                    eq("username", username),
                    eq("productId", productId),
                    eq("rateDownline", rateDownline),
                    eq("startDate", startDate),
                    eq("endDate", endDate)
            );

            Query query = new Query(criteria);
            insertOrUpdateWinlossAgentTemp(bsonFilter, new Document(newData));
        }
      sumWinlossFinalStep(startDate,endDate,uCompany);
        // Further processing...
        //need to check
        return true;
    }

    public void sumWinlossFinalStep(String startDate, String endDate, String uCompany) throws Exception {
        Bson filter = Filters.and(
                Filters.eq("uCompany", uCompany),
                Filters.gte("playdate", startDate),
                Filters.lte("playdate", endDate)
        );

        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(filter),
                Aggregates.group(
                        new Document("username", "$username").append("productId", "$productId"),
                        Accumulators.sum("betCount", 1),
                        Accumulators.last("doc", "$$ROOT"),
                        Accumulators.sum("betAmount", "$betAmount"),
                        Accumulators.sum("betTransferIn", "$betTransferIn"),
                        Accumulators.sum("betTransferOut", "$betTransferOut"),
                        Accumulators.sum("betWinloss", "$betWinloss"),
                        Accumulators.sum("memberWinloss", "$memberWinloss"),
                        Accumulators.sum("winCompany", "$winCompany"),
                        Accumulators.sum("winAgentValid", "$winAgentValid"),
                        Accumulators.sum("winAgentUp", "$winAgentUp"),
                        Accumulators.sum("winProvider", "$winProvider")
                ),
                Aggregates.project(
                        new Document("betCount", 1)
                                .append("refSale", "$doc.refSale")
                                .append("betAmount", 1)
                                .append("betTransferIn", 1)
                                .append("betTransferOut", 1)
                                .append("memberWinloss", 1)
                                .append("betWinloss", 1)
                                .append("winCompany", 1)
                                .append("winAgentValid", 1)
                                .append("winAgentUp", 1)
                                .append("winProvider", 1)
                                .append("createdAt", "$doc.createdAt")
                                .append("playdate", "$doc.playdate")
                                .append("productName", "$doc.productName")
                                .append("gameCategory", "$doc.gameCategory")
                                .append("gameProvider", "$doc.gameProvider")
                                .append("level", "$doc.level")
                )
        );


            List<Document> documents = aggregateWinLossAgentTemp(pipeline);

        for (Document result : documents) {
            Document id = (Document) result.get("_id");
            String username = id.getString("username");
            String productId = id.getString("productId");

            if (username == null || productId == null) {
                System.out.println("Error: username or productId missing or not a string");
                continue;
            }

            GetUserRequest getAgentRequest = GetUserRequest.builder()
                    .username(username)
                    .build();
            GetUserResponse agentDetails = memberClient.getUser(getAgentRequest); // Handle this call properly

            String userType = agentDetails.getType();
            String userUpline = agentDetails.getUpline();
            String userCompany = userType.equals("company") ? uCompany : agentDetails.getUCompany();
            double rate = 0.0;
            for (Game game : agentDetails.getGames()) {
                if (productId.equals(game.getProductId()) && "A".equals(game.getStatus())) {
                    rate = game.getRate();
                    break;
                }
            }

            Double memberWinloss = result.getDouble("memberWinloss");
            if (memberWinloss == null) {
                log.error("Error: memberWinloss missing or not a double");
                return;
            }

            Integer level = result.getInteger("level");
            if (level == null) {
                log.error("Error: level missing or not an integer");
                return;
            }

            Double winCompany = result.getDouble("winCompany");
            if (winCompany == null) {
                log.error("Error: winCompany missing or not a double");
                return;
            }

            Double winAgentValid = result.getDouble("winAgentValid");
            if (winAgentValid == null) {
                log.error("Error: winAgentValid missing or not a double");
                return;
            }

            Double winAgentUp = result.getDouble("winAgentUp");
            if (winAgentUp == null) {
                log.error("Error: winAgentUp missing or not a double");
                return;
            }

            Double winProvider = result.getDouble("winProvider");
            if (winProvider == null) {
                log.error("Error: winProvider missing or not a double");
                return;
            }

            LocalDateTime parsedStartDate;
            LocalDateTime parsedEndDate;
            try {
                parsedStartDate = LocalDateTime.parse(startDate, Constants.DATE_TIME_FORMAT);
                parsedEndDate = LocalDateTime.parse(endDate,  Constants.DATE_TIME_FORMAT);
            } catch (DateTimeParseException e) {
                log.error("Failed to parse dates", e);
                throw new RuntimeException("Failed to parse dates", e);
            }

            Instant instantStartDate = parsedStartDate.atZone(ZoneId.systemDefault()).toInstant();
            Instant instantEndDate = parsedEndDate.atZone(ZoneId.systemDefault()).toInstant();

            Document newData = new Document( Transformer.winlossSchemaTransformer(Date.from(instantStartDate), Date.from(instantEndDate), productId, result));// You'll need to implement this

            newData.append("level", result.getInteger("level", 0) + 1);
            newData.append("uCompany", userCompany);
            newData.append("username", username);
            newData.append("userType", userType);
            newData.append("upline", userUpline);

            // ... set other fields ...

            double uplineWinloss;
            if ("company".equals(userType)) {
                uplineWinloss = (memberWinloss / 100.0) * (100.0 - rate) * -1.0;
            } else {
                uplineWinloss = getWinLoss(userUpline, productId, rate, memberWinloss); // Implement this method
            }

            newData.append("uplineWinloss", uplineWinloss);

            Bson filter1 = Filters.and(
                    Filters.eq("username", username),
                    Filters.eq("productId", productId),
                    Filters.eq("startDate", startDate),
                    Filters.eq("endDate", endDate)
            );
            insertOrUpdateWinlossAgent(filter1,newData);
            //			return errors.New("failed to insert winloss agent at final step")
            Bson tempFilter = Filters.and(
                    Filters.eq("uCompany", uCompany),
                    Filters.gte("playdate", startDate),
                    Filters.lte("playdate", endDate)
            );
            deleteWinLossTemp(tempFilter);
            //			return errors.New("failed to clear temp data")

        }
    }

    public List<Document> aggregateWinLossAgentTemp(List<Bson> pipeline) throws Exception {
        List<Document> combinedResults=new ArrayList<>();

        MongoCollection<Document> collection = mongoTemplate.getCollection("winlossAgentsTemp");

        MongoCursor<Document> cursor = collection.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            combinedResults.add(cursor.next());
        }
        cursor.close();

        return combinedResults;
    }
    public void insertOrUpdateWinlossAgentTemp(Bson filter, Bson transaction) throws Exception {
        MongoCollection<Document> collection = mongoTemplate.getCollection("winlossAgentsTemp");
        UpdateOptions options = new UpdateOptions().upsert(true);

        try {
            // The $set operator is used to update the value of a field in a document
            Bson updateOperation = new Document("$set", transaction);
            UpdateResult result = collection.updateOne(filter, updateOperation, options);

            if (result.getMatchedCount() == 0 && result.getUpsertedId() == null) {
                throw new Exception("No document was updated and no new document was inserted.");
            }
        } catch (Exception e) {
            // Log the exception, handle it, or rethrow as appropriate
            throw e;
        }
    }
    public void insertOrUpdateWinlossAgent(Bson filter, Bson transaction) throws Exception {
        UpdateOptions options = new UpdateOptions().upsert(true);

        try {
            // Context with timeout is handled automatically in the Java driver with the `maxTime` method.
            MongoCollection<Document> winlossAgents = mongoTemplate.getCollection("winlossAgents");

            winlossAgents.updateOne(filter, new Document("$set", transaction), options);
        } catch (Exception e) {
            // Rethrow the exception or handle it as per your error handling policy
            throw e;
        }
    }
    private Date getStartOfDay(Date date) {
        // Set the time to the beginning of the day
        // For example, "2023-11-02 00:00:00"
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = dateFormatter.format(date);
        String startOfDayString = dateString + " 00:00:00";

        try {
            return dateFormatter.parse(startOfDayString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Double getWinLoss(String username, String productId, double rate, double memberWinloss) throws Exception {
        GetUserRequest getAgentRequest = new GetUserRequest(username);
        GetUserResponse agentDetails = memberClient.getUser(getAgentRequest);

        if (agentDetails == null) {
            throw new Exception("Failed to fetch upline games");
        }

        double uplineRate = 0.0;
        for (Game game : agentDetails.getGames()) {
            if (productId.equals(game.getProductId())) {
                uplineRate = game.getRate();
                break;
            }
        }

        double uplineRateValid = uplineRate - rate;
        double result = (memberWinloss / 100) * uplineRateValid * -1;

        return result;
    }

    private Date getEndOfDay(Date date) {
        // Set the time to the end of the day
        // For example, "2023-11-02 23:59:59"
        SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd");
        String dateString = dateFormatter.format(date);
        String endOfDayString = dateString + " 23:59:59";

        try {
            return dateFormatter.parse(endOfDayString);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void deleteWinLossTemp(Bson filter) {
        MongoCollection<Document> collection = mongoTemplate.getCollection("winlossAgentsTemp");
        try {
            DeleteResult result = collection.deleteMany(filter);
            if (result.getDeletedCount() == 0) {
                log.info("No documents found that match the filter, nothing was deleted.");
            }
        } catch (Exception e) {
            log.error("Error deleting documents:", e);
            throw e; // Rethrow the exception or handle it based on your error handling policy
        }
    }
}