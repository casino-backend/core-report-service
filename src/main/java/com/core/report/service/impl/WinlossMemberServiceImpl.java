package com.core.report.service.impl;

import com.core.report.dto.Game;
import com.core.report.dto.GetUserRequest;
import com.core.report.dto.GetUserResponse;
import com.core.report.dto.User;
import com.core.report.client.MemberClient;
import com.core.report.service.WinlossMemberService;
//import com.core.report.service.dto.*;


import com.core.report.utils.Transformer;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Service;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class WinlossMemberServiceImpl implements WinlossMemberService {
   @Autowired
    private MongoTemplate mongoTemplate;
@Autowired
MemberClient memberClient;
 @Override
 public void sumWinLossMember(Date startDate, Date endDate, Document filter, boolean byHour) throws Exception {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");

        if (!dateFormat.format(startDate).equals(dateFormat.format(endDate))) {
            throw new Exception("Date range exceeds one day");
        }

        List<Document> pipeline = new ArrayList<>();

        Document matchStage = new Document("$match", filter);
        pipeline.add(matchStage);

        Document groupStage = new Document("$group", new Document("_id", new Document("username", "$username")
                .append("productId", "$productId"))
                .append("betCount", new Document("$sum", 1))
                .append("doc", new Document("$last", "$$ROOT"))
                .append("betAmount", new Document("$sum", "$betAmount"))
                .append("betTransferIn", new Document("$sum", "$betTransferIn"))
                .append("betTransferOut", new Document("$sum", "$betTransferOut"))
                .append("betWinloss", new Document("$sum", "$betWinloss")));
        pipeline.add(groupStage);

        Document projectStage = new Document("$project", new Document("betCount", 1)
                .append("upline", "$doc.upline")
                .append("refSale", "$doc.refSale")
                .append("betAmount", 1)
                .append("betTransferIn", 1)
                .append("betTransferOut", 1)
                .append("betWinloss", 1)
                .append("createdAt", "$doc.createdAt")
                .append("playdate", "$doc.playdate")
                .append("productName", "$doc.productName")
                .append("gameCategory", "$doc.gameCategory")
                .append("gameProvider", "$doc.provider")
                .append("percentage", "$doc.percentage"));
        pipeline.add(projectStage);

        List<Document> combinedResults=new ArrayList<>();

        MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection("transactionReports");
        MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            combinedResults.add(cursor.next());
        }
        cursor.close();

        for (Document result:combinedResults) {

            Document _id = result.get("_id", Document.class);
            if (_id == null) {
                System.out.println("Error: _id field is missing or not a document");
                continue;
            }

            String username = _id.getString("username");
            if (username == null) {
                System.out.println("Error: username field is missing or not a string");
                continue;
            }

            String productId = _id.getString("productId");
            if (productId == null) {
                System.out.println("Error: productId field is missing or not a string");
                continue;
            }

            String userUpline = result.getString("upline");
            if (userUpline == null) {
                System.out.println("Error: userUpline field is missing or not a string");
                continue;
            }
//need to create utility

            double betTransferIn = Double.parseDouble(result.get("betTransferIn", 0.0).toString());
            double betTransferOut = Double.parseDouble(result.get("betTransferOut", 0.0).toString());
            double betWinLoss = Double.parseDouble(result.get("betWinloss", 0.0).toString());

            double winlossInOut = betTransferIn - betTransferOut;
            double memberWinloss = betWinLoss + winlossInOut;

            int level = 0;
            String userType = "";  // You need to retrieve this value from your data source
            userUpline = "";  // You need to retrieve this value from your data source
            String uCompany = "";  // You need to retrieve this value from your data source

            Document newData = new Document(Transformer.winlossSchemaTransformer(startDate, endDate, productId, result));
            if (newData == null) {
                System.out.println("Error: failed to transform schema");
                continue;
            }

            newData.append("level", level);
            newData.append("uCompany", uCompany);
            newData.append("username", username);
            newData.append("userType", userType);
            newData.append("upline", userUpline);
            newData.append("betWinloss", betWinLoss);
            newData.append("memberWinloss", memberWinloss);

            if (!byHour) {
                 userUpline = result.getString("upline"); // Make sure to retrieve upline from the result
                 productId = _id.getString("productId");

                GetUserRequest getAgentRequest = GetUserRequest.builder().username(userUpline).build();

                GetUserResponse agentGameDetails;
                try {
                    agentGameDetails = memberClient.getAgentUser(getAgentRequest);
                } catch (Exception e) {
                    throw new RuntimeException("failed to fetch upline games", e);
                }

                double rate = 0.0;
                for (Game games : agentGameDetails.getGames()) {
                    if (games.getProductId().equals(productId) && games.getStatus().equals("A")) {
                        rate = games.getRate();
                        break;
                    }
                }

                if (rate == 0.0) {
                    throw new RuntimeException("failed to get upline game rate");
                }

                double winCompany = (memberWinloss / 100) * rate * -1;
                int winAgentValid = 0; // Make sure to retrieve or calculate winAgentValid
                int winAgentUp = 0; // Make sure to retrieve or calculate winAgentUp
                double winProvider = (memberWinloss / 100) * (100 - rate) * -1;

                newData.put("rate", rate);
                newData.put("winCompany", winCompany);
                newData.put("winAgentValid", winAgentValid);
                newData.put("winAgentUp", winAgentUp);
                newData.put("winProvider", winProvider);

                // Create a filter Document for MongoDB
                Document filter1 = new Document("username", username)
                        .append("productId", productId)
                        .append("startDate", startDate.toInstant().toString())
                        .append("endDate", endDate.toInstant().toString());
                insertOrUpdateWinlossMember(filter1,newData);
                // Insert or update the document with the new data in MongoDB
                // Use the MongoDB Java driver methods to interact with your database
            } else {

                Document filter2 = new Document("username", username)
                        .append("productId", productId)
                        .append("startDate", startDate.toInstant().toString())
                        .append("endDate", endDate.toInstant().toString());
                insertOrUpdateWinLossMemberByHour(filter2,newData);
            }
        }
    }

   @Override
   public void processDailyWinLoss(String sumDate) {
        ZonedDateTime startDate, endDate;

        if (sumDate != null && !sumDate.isEmpty()) {
            Instant parsedTime = Instant.parse(sumDate);
            startDate = ZonedDateTime.ofInstant(parsedTime, ZoneId.of("UTC"));
            startDate = startDate.withHour(0).withMinute(0).withSecond(0).withNano(0);
            endDate = startDate.withHour(23).withMinute(59).withSecond(59).withNano(0);
        } else {
            ZonedDateTime now = ZonedDateTime.now(ZoneId.of("UTC"));
            startDate = now.withHour(0).withMinute(0).withSecond(0).withNano(0);
            endDate = now.withHour(23).withMinute(59).withSecond(59).withNano(0);
        }

        Bson filter = Filters.and(
                Filters.gte("playdate", Date.from(startDate.toInstant())),
                Filters.lte("playdate", Date.from(endDate.toInstant()))
        );


        Bson groupStage = Aggregates.group(
                new Document("_id", new Document("username", "$username").append("productId", "$productId")),
                Accumulators.sum("betCount", 1),
                Accumulators.last("doc", "$$ROOT"),
                Accumulators.sum("betAmount", "$betAmount"),
                Accumulators.sum("betTransferIn", "$betTransferIn"),
                Accumulators.sum("betTransferOut", "$betTransferOut"),
                Accumulators.sum("betWinloss", "$betWinloss"),
                Accumulators.sum("memberWinloss", "$memberWinloss")
        );

        Bson projectStage = Projections.fields(
                Projections.include("betCount"),
                Projections.computed("upline", "$doc.upline"),
                Projections.computed("refSale", "$doc.refSale"),
                Projections.include("betAmount", "betTransferIn", "betTransferOut", "memberWinloss", "betWinloss"),
                Projections.computed("createdAt", "$doc.createdAt"),
                Projections.computed("playdate", "$doc.playdate"),
                Projections.computed("productName", "$doc.productName"),
                Projections.computed("gameCategory", "$doc.gameCategory"),
                Projections.computed("gameProvider", "$doc.gameProvider"),
                Projections.computed("percentage", "$doc.percentage")
        );

        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(filter),
                groupStage,
                projectStage
        );
        List<Document> combinedResults = aggregateWinLossMemberByHour(pipeline);
        for(Document result:combinedResults){

            String username = result.getString("_id.username");
            if (username == null) {
                log.info("Error: username missing or not a string");
                continue;
            }

            String upline = result.getString("upline");
            if (upline == null) {
                log.info("Error: upline missing or not a string");
                continue;
            }
            String productId = result.getString("_id.productId");

            double betTransferIn = getDoubleValue(result, "betTransferIn");
            double betTransferOut = getDoubleValue(result, "betTransferOut");
            double betWinloss = getDoubleValue(result, "betWinloss");
            double memberWinloss = getDoubleValue(result, "memberWinloss");



            // Fetch agent game details
            double rate = memberClient.fetchAgentGameRate(upline,productId);

            if (rate == 0.0) {
                throw new RuntimeException("Failed to get upline game rate");
            }

            double winCompany = (memberWinloss / 100) * rate * -1;
            int winAgentValid = 0;
            int winAgentUp = 0;
            double winProvider = (memberWinloss / 100) * (100 - rate) * -1;

            // Fetch member details
            Document memberDetails = fetchMemberDetails(username);
            String userType = memberDetails.getString("Type");
            String userUpline = memberDetails.getString("Upline");
            String uCompany = memberDetails.getString("UCompany");

            Document newData = new Document(Transformer.winlossSchemaTransformer( Date.from(startDate.toInstant()), Date.from(endDate.toInstant()), productId, result));
            int level = 0;
            newData.put("level", level);
            newData.put("username", username);
            newData.put("userType", userType);
            newData.put("upline", userUpline);
            newData.put("uCompany", uCompany);
            newData.put("memberWinloss", memberWinloss);
            newData.put("rate", rate);
            newData.put("winCompany", winCompany);
            newData.put("winAgentValid", winAgentValid);
            newData.put("winAgentUp", winAgentUp);
            newData.put("winProvider", winProvider);

            Bson upsertFilter = Filters.and(
                    Filters.eq("username", username),
                    Filters.eq("productId", productId),
                    Filters.eq("startDate", startDate),
                    Filters.eq("endDate", endDate)
            );

            // Insert or update the document
            insertOrUpdateWinlossMember(upsertFilter, newData);
        }


        // Use the filter in your MongoDB query
    }

    private double fetchAgentGameRate(String productId) {
        // Implement logic to fetch agent game rate based on productId
        return 0.0;
    }

    private Document fetchMemberDetails(String username) {
        // Implement logic to fetch member details based on username
        return new Document("Type", "userTypeValue")
                .append("Upline", "userUplineValue")
                .append("UCompany", "uCompanyValue");
    }

    private Document transformWinlossSchema(String startDate, String endDate, String productId, Document result) {
        // Implement logic to transform the winloss schema
        return new Document("transformedData", "value");
    }


    public List<Document> aggregateWinLossMemberByHour(List<Bson> pipeline) {
        List<Document> combinedResults=new ArrayList<>();

            MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection("winlossMembers");
            MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
            while (cursor.hasNext()) {
                combinedResults.add(cursor.next());
            }
            cursor.close();

        return combinedResults;
    }


        private void insertOrUpdateWinlossMember(Bson filter, Document newData) {

        MongoCollection<Document> collection = mongoTemplate.getCollection("winlossMembers");

        // Create an instance of UpdateOptions to enable upsert (insert if not found)
        UpdateOptions options = new UpdateOptions().upsert(true);

        // Perform the insert or update operation
        collection.updateOne(filter, new Document("$set", newData), options);

        // Close the MongoClient when done
    }

    private void insertOrUpdateWinLossMemberByHour(Bson filter, Document newData) {
        try{
            MongoCollection<Document> collection = mongoTemplate.getCollection("winlossMembersByHour");

            UpdateOptions options = new UpdateOptions().upsert(true);

            collection.updateOne(filter, new Document("$set", newData), options);
        } catch (Exception e) {
            // Handle exceptions
            e.printStackTrace();
        }
    }

    private double getDoubleValue(Document document, String field) {
        Double value = document.getDouble(field);
        if (value == null) {
            log.info("Error: " + field + " missing or not a double");
            return 0.0;
        }
        return value;
    }

    private double getAgentGameRate(String productId, String upline) {
        // Implement logic to fetch agent game rate based on productId and upline
        return 0.0; // Replace with actual logic
    }

}