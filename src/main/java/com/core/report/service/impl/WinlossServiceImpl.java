package com.core.report.service.impl;

import com.core.report.client.MemberClient;
import com.core.report.dto.*;
import com.core.report.service.WinlossService;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
@AllArgsConstructor
public class WinlossServiceImpl implements WinlossService {

 /*   private final WinlossAgentRepository winlossAgentRepository;
    private final WinlossMemberRepository winlossMemberRepository;
    private final UserServiceClient userServiceClient;*/

    private MongoTemplate mongoTemplate;
    private MemberClient memberClient;

    private static List<Bson> getAggregationPipelineWinlossMemberOrAgent(Result result) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(result.criteria().getCriteriaObject()),
                Aggregates.group("$username",
                        Accumulators.last("doc", "$$ROOT"),
                        Accumulators.sum("betCount", "$betCount"),
                        Accumulators.sum("betAmount", "$betAmount"),
                        Accumulators.sum("betTransferIn", "$betTransferIn"),
                        Accumulators.sum("betTransferOut", "$betTransferOut"),
                        Accumulators.sum("memberWinloss", "$memberWinloss"),
                        Accumulators.sum("winCompany", "$winCompany"),
                        Accumulators.sum("winAgentUp", "$winAgentUp"),
                        Accumulators.sum("winAgentValid", "$winAgentValid"),
                        Accumulators.sum("winProvider", "$winProvider")
                ),
                Aggregates.sort(Sorts.ascending("winCompany")),
                Aggregates.project(Projections.fields(
                        Projections.excludeId(),
                        Projections.include("betCount"),
                        Projections.computed("username", "$doc.username"),
                        Projections.computed("upline", "$doc.upline"),
                        Projections.computed("userType",
                                new Document("$switch", new Document()
                                        .append("branches", Arrays.asList(
                                                new Document("case", new Document("$eq", Arrays.asList("$doc.userType", "share_holder")))
                                                        .append("then", "share holder"),
                                                new Document("case", new Document("$eq", Arrays.asList("$doc.userType", "super_senior")))
                                                        .append("then", "super senior")
                                        ))
                                        .append("default", "$doc.userType")
                                )
                        ),
                        Projections.computed("betAmount", new Document("$round", Arrays.asList("$betAmount", 2))),
                        Projections.computed("betTransferIn", new Document("$round", Arrays.asList("$betTransferIn", 2))),
                        Projections.computed("betTransferOut", new Document("$round", Arrays.asList("$betTransferOut", 2))),
                        Projections.computed("memberWinloss", new Document("$round", Arrays.asList("$memberWinloss", 2))
                        ), Projections.computed("agent", new Document()
                                .append("remark", "downline billing")
                                .append("winAgentValid", new Document("$round", Arrays.asList("$winAgentValid", 2)))
                                .append("winloss", new Document("$round", Arrays.asList("$winAgentUp", 2)))
                                .append("commission", "0")
                                .append("winlossAfterCommission", new Document("$round", Arrays.asList("$winAgentUp", 2))
                                )), Projections.computed("company", new Document()
                                .append("remark", new Document("$concat", Arrays.asList("$winloss", " ", "$doc.upline")))
                                .append("winloss", new Document("$round", Arrays.asList("$winCompany", 2)))
                                .append("commission", "0")
                                .append("winlossAfterCommission", new Document("$round", Arrays.asList("$winCompany", 2))
                                )), Projections.computed("provider", new Document()
                                .append("remark", "provider winloss")
                                .append("winloss", new Document("$round", Arrays.asList("$winProvider", 2)))
                                .append("commission", "0")
                                .append("winlossAfterCommission", new Document("$round", Arrays.asList("$winProvider", 2))
                                )), Projections.computed("lastUpdate", "$doc.lastUpdate"))
                ));
        return pipeline;
    }

    private static String getCollectionName(GetWinLossRequest request, GetUserResponse userDetails) {
        String collectionName;
        if ("member".equals(userDetails.getType())) {
            collectionName = "winlossMembers";
        } else {
            collectionName = "winlossAgents";
        }
        if (!request.isViewSkipMember()) {
            collectionName = "winlossMembers";
        }

        return collectionName;
    }


    private static void addUsernameOrUserTypeInFilterCriteriaList(String username, List<Criteria> criteriaList) {
        if (StringUtils.isNotEmpty(username)) {
            if ("account168".equals(username)) {
                criteriaList.add(Criteria.where("userType").is("company"));
            } else {
                criteriaList.add(Criteria.where("username").is(username));
            }
        }
    }

    private static LocalDateTime getEndDate(LocalDateTime now, String endDate) {
        if (endDate != null && !endDate.isEmpty()) {
            try {
                return LocalDateTime.parse(endDate + "T23:59:59");
            } catch (DateTimeParseException e) {
                //throw new BadRequestException("Failed to parse start_date", e);
            }
        }
        return LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59, 59);
    }

    private static LocalDateTime getStartDate(LocalDateTime now, String startDate) {

        if (startDate != null && startDate.isEmpty()) {
            try {
                return LocalDateTime.parse(startDate + "T00:00:00");
            } catch (DateTimeParseException e) {
                //throw new BadRequestException("Failed to parse start_date", e);
            }
        }
        return LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0, 0);
    }

    private static List<Bson> getAggregationPipelinForWinlossAgents(Criteria baseCriteria) {
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(baseCriteria.getCriteriaObject()), // Replace with actual base criteria field and value
                Aggregates.group("$productId",
                        Accumulators.last("doc", "$$ROOT"),
                        Accumulators.sum("betCount", "$betCount"),
                        Accumulators.sum("betAmount", "$betAmount"),
                        Accumulators.sum("betTransferIn", "$betTransferIn"),
                        Accumulators.sum("betTransferOut", "$betTransferOut"),
                        Accumulators.sum("memberWinloss", "$memberWinloss"),
                        Accumulators.sum("winCompany", "$winCompany"),
                        Accumulators.sum("winAgentUp", "$winAgentUp"),
                        Accumulators.sum("winProvider", "$winProvider")
                ),
                Aggregates.sort(Sorts.ascending("winCompany")),
                Aggregates.project(Projections.fields(
                        Projections.excludeId(),
                        Projections.include("productId"),
                        Projections.computed("productName", "$doc.productName"),
                        Projections.include("betCount"),
                        Projections.computed("betAmount", Document.parse("{ $round: ['$betAmount', 2] }")),
                        Projections.computed("betTransferIn", Document.parse("{ $round: ['$betTransferIn', 2] }")),
                        Projections.computed("betTransferOut", Document.parse("{ $round: ['$betTransferOut', 2] }")),
                        Projections.computed("memberWinloss", Document.parse("{ $round: ['$memberWinloss', 2] }")),
                        Projections.computed("agent.remark", Document.parse("{ $round: ['$winAgentUp', 2] }")),
                        Projections.computed("agent.winloss", Document.parse("{ $round: ['$winAgentUp', 2] }")),
                        // Projections.computed("agent.commission", 0),
                        Projections.computed("agent.winlossAfterCommission", Document.parse("{ $round: ['$winAgentUp', 2] }")),
                        Projections.computed("company.remark", Document.parse("{ $concat: ['$doc.upline', ' ', 'winloss', ' ', '$doc.upline'] }")),
                        Projections.computed("company.winloss", Document.parse("{ $round: ['$winCompany', 2] }")),
                        //Projections.computed("company.commission", 0),
                        Projections.computed("company.winlossAfterCommission", Document.parse("{ $round: ['$winCompany', 2] }")),
                        Projections.computed("provider.remark", "provider winloss"),
                        Projections.computed("provider.winloss", Document.parse("{ $round: ['$winProvider', 2] }")),
                        //  Projections.computed("provider.commission", 0),
                        Projections.computed("provider.winlossAfterCommission", Document.parse("{ $round: ['$winProvider', 2] }")),
                        Projections.computed("lastUpdate", "$doc.lastUpdate")
                ))
        );
        return pipeline;
    }

    @Override
    public WinlossResponse reportWinloss(GetWinLossRequest request) {

        Result result = getCriteriaList(request);

        List<Bson> pipeline = getAggregationPipelineWinlossMemberOrAgent(result);

        List<Document> combinedResults = new ArrayList<>();

        if (result.collectionName().equals("winlossMembers")) {
            getAggreatedDocuments("winlossMembers", pipeline, combinedResults);

        } else {
            getAggreatedDocuments("winlossAgents", pipeline, combinedResults);

        }
        if (combinedResults.isEmpty() && !"winlossMembers".equals(result.collectionName())) {
            getAggreatedDocuments("winlossMembers", pipeline, combinedResults);
        }

        return formatResponse(combinedResults, request);
    }

    private void getAggreatedDocuments(String collection, List<Bson> pipeline, List<Document> combinedResults) {
        MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection(collection);
        MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
        while (cursor.hasNext()) {
            combinedResults.add(cursor.next());
        }
        cursor.close();
    }

    private Result getCriteriaList(GetWinLossRequest request) {
        List<Criteria> criteriaList = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = getStartDate(now, request.getStartDate());
        LocalDateTime endDate = getEndDate(now, request.getEndDate());

        criteriaList.add(Criteria.where("startDate").gte(startDate).lte(endDate));
        String username = request.getUsername();
        String upline = request.getUpline();
        String collectionName;
        addUsernameOrUserTypeInFilterCriteriaList(username, criteriaList);
        if (StringUtils.isNotEmpty(upline)) {
            upline = upline.replace("-member", "");
            criteriaList.add(Criteria.where("upline").is(upline));
        }

        if (username == null || username.isEmpty()) {
            //   throw new BadRequestException("Invalid username");
        }

        if (StringUtils.isNotEmpty(request.getProductID())) {
            criteriaList.add(Criteria.where("productId").is(request.getProductID()));
        }

        if (!CollectionUtils.isEmpty(request.getGameCategory())) {
            criteriaList.add(Criteria.where("gameCategory").in(request.getGameCategory()));
        }

        GetUserRequest userRequest = new GetUserRequest(username);

        GetUserResponse userDetails = memberClient.getUser(userRequest);

        collectionName = getCollectionName(request, userDetails);

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));
        Result result = new Result(collectionName, criteria);
        return result;
    }

    @Override
    public WinlossResponse reportWinlossByProduct(GetWinLossByProductRequest request) {
        Date now = new Date();
        Date startDate = getStartOfDay(now);
        Date endDate = getEndOfDay(now);

        if (request.getStartDate() != null && request.getEndDate() != null) {
            startDate = parseDate(request.getStartDate() + " 00:00:00");
            endDate = parseDate(request.getEndDate() + " 23:59:59");
        }
        Criteria baseCriteria = new Criteria();

        if (startDate != null && endDate != null) {
            //  baseCriteria.and("startDate").gte(startDate).lte(endDate);
        }

        if (request.getUsername() != null) {

            if ("account168".equals(request.getUsername())) {
                baseCriteria.and("userType").is("company");
            } else {
                baseCriteria.and("username").is(request.getUsername());
            }

        }

        if (request.getProductID() != null) {
            baseCriteria.and("productId").is(request.getProductID());
        }
        List<Bson> pipeline = getAggregationPipelinForWinlossAgents(baseCriteria);
        List<Document> combinedResults = new ArrayList<>();

        getAggreatedDocuments("winlossAgents", pipeline, combinedResults);
        return formatResponse(combinedResults, request);
    }

    public WinlossResponse formatResponse(List<Document> documents, Object request) {
        String username = "";
        if (request instanceof GetWinLossRequest) {
            username = ((GetWinLossRequest) request).getUsername();
        } else if (request instanceof GetWinLossByProductRequest) {
            username = ((GetWinLossByProductRequest) request).getUsername();
        } else {
            throw new IllegalArgumentException("request is not of the expected type");
        }

        if (documents.isEmpty()) {
            GetUserResponse user = getUser(username);
            String lastUpdate = getLastUpdate(user);
            Map<String, Object> defaultEntry = createDefaultEntry(username, lastUpdate);
            documents.add(new Document(defaultEntry));
        }
        int totalBetCount = 0;
        double totalBetAmount = 0.0;
        double totalMemberWinloss = 0.0;
        double totalBetTransferIn = 0.0;
        double totalBetTransferOut = 0.0;
        double totalAgentWinloss = 0.0;
        double totalAgentWinAgentValid = 0.0;
        double totalCompanyWinloss = 0.0;
        double totalProviderWinloss = 0.0;

        String lastUpdate = "RealTime";

        if (!documents.isEmpty()) {
            Object lastUpdateObject = documents.get(0).get("lastUpdate");
            if (lastUpdateObject instanceof String) {
                lastUpdate = (String) lastUpdateObject;
            }
        }
        for (Document item : documents) {
            if (item.containsKey("betCount")) {
                int betCount = (int) item.get("betCount");
                totalBetCount += betCount;
            }
            if (item.containsKey("betAmount")) {
                double betAmount = (double) item.get("betAmount");
                totalBetAmount += betAmount;
            }
            if (item.containsKey("memberWinloss")) {
                double memberWinloss = (double) item.get("memberWinloss");
                totalMemberWinloss += memberWinloss;
            }
            if (item.containsKey("betTransferIn")) {
                double betTransferIn = (double) item.get("betTransferIn");
                totalBetTransferIn += betTransferIn;
            }
            if (item.containsKey("betTransferOut")) {
                double betTransferOut = (double) item.get("betTransferOut");
                totalBetTransferOut += betTransferOut;
            }
            if (item.containsKey("agent")) {
                Document agentData = item.get("agent", Document.class);
                if (agentData.containsKey("winloss")) {
                    double winloss = (double) agentData.get("winloss");
                    totalAgentWinloss += winloss;
                }
                if (agentData.containsKey("winAgentValid")) {
                    double winAgentValid = agentData.getDouble("winAgentValid");
                    totalAgentWinAgentValid += winAgentValid;
                }
            }
            if (item.containsKey("company")) {
                Document companyData = item.get("company", Document.class);
                if (companyData.containsKey("winloss")) {
                    double winloss = companyData.getDouble("winloss");
                    totalCompanyWinloss += winloss;
                }
            }
            if (item.containsKey("provider")) {
                Document providerData = item.get("provider", Document.class);
                if (providerData.containsKey("winloss")) {
                    double winloss = providerData.getDouble("winloss");
                    totalProviderWinloss += winloss;
                }
            }
        }

        WinlossResponse winlossResponse = WinlossResponse.builder().totalBetCount(totalBetCount).totalBetAmount(totalBetAmount)
                .totalMemberWinloss(totalMemberWinloss).totalBetTransferIn(totalBetTransferIn).totalBetTransferOut(totalBetTransferOut)
                .totalAgentWinloss(totalAgentWinloss).totalAgentWinAgentValid(totalAgentWinAgentValid).totalCompanyWinloss(totalCompanyWinloss)
                .totalProviderWinloss(totalProviderWinloss).request(request).items(documents).lastUpdate(lastUpdate).build();

        return winlossResponse;
    }

    private Date getStartOfDay(Date date) {
        // Implement logic to get the start of the day for the given date
        return date;
    }

    private Date getEndOfDay(Date date) {
        // Implement logic to get the end of the day for the given date
        return date;
    }

    private Date parseDate(String dateString) {
        // Implement logic to parse the date from the provided string using dateTimeLayout
        return new Date();
    }

    private GetUserResponse getUser(String username) {
        GetUserRequest userRequest = new GetUserRequest();
        userRequest.setUsername(username);

        GetUserResponse user;
        try {
            user = memberClient.getUser(userRequest);
        } catch (Exception e) {
            Logger.getGlobal().log(Level.SEVERE, "gRPC Error: " + e.getMessage());
            return null; // Handle the error accordingly
        }

        if (user == null) {
            GetUserRequest uplineRequest = new GetUserRequest();
            uplineRequest.setUsername(username);

            try {
                user = memberClient.getUser(uplineRequest);
            } catch (Exception e) {
                Logger.getGlobal().log(Level.SEVERE, "gRPC Error: " + e.getMessage());
                return null; // Handle the error accordingly
            }
        }

        return user;
    }

    private String getLastUpdate(GetUserResponse user) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        String lastUpdate = dateFormat.format(new Date());

        if (user != null) {
            Query query = new Query();
            Criteria criteria = Criteria.where("username").is(user.getUsername());
            query.addCriteria(criteria);
            Bson filter = Filters.eq("username", user.getUsername());

            List<Document> winlossAgents = mongoTemplate.find(query, Document.class, "winlossAgents");
            String lastUpdateFromDB = winlossAgents.get(0).getString("lastUpdate");

            if (StringUtils.isNotEmpty(lastUpdateFromDB)) {
                lastUpdate = lastUpdateFromDB;
            }
        }

        return lastUpdate;
    }

    private Map<String, Object> createDefaultEntry(String username, String lastUpdate) {
        Map<String, Object> entry = new HashMap<>();
        entry.put("lastUpdate", lastUpdate);
        entry.put("betCount", 0);
        entry.put("betAmount", 0.0);
        entry.put("memberWinloss", 0.0);
        entry.put("betTransferIn", 0.0);
        entry.put("betTransferOut", 0.0);
        entry.put("username", username);
        entry.put("userType", "");
        entry.put("upline", "");

        Map<String, Object> agentData = new HashMap<>();
        agentData.put("remark", "Waiting for update");
        agentData.put("winloss", 0.0);
        agentData.put("commission", 0.0);
        agentData.put("winlossAfterCommission", 0.0);
        entry.put("agent", agentData);

        Map<String, Object> companyData = new HashMap<>();
        companyData.put("remark", "Waiting for update");
        companyData.put("winloss", 0.0);
        entry.put("company", companyData);
        return entry;
    }

    private record Result(String collectionName, Criteria criteria) {
    }


}
