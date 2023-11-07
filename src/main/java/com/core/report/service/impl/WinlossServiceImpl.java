package com.core.report.service.impl;

import com.core.report.dto.*;
import com.core.report.client.MemberClient;
import com.core.report.service.WinlossService;
//import com.core.report.service.dto.*;


import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.*;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
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

import static org.springframework.data.domain.Sort.Direction.ASC;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.*;

@Service
@AllArgsConstructor
public class WinlossServiceImpl implements WinlossService {

 /*   private final WinlossAgentRepository winlossAgentRepository;
    private final WinlossMemberRepository winlossMemberRepository;
    private final UserServiceClient userServiceClient;*/

    private MongoTemplate mongoTemplate;
    private MemberClient memberClient;

    /*  @Autowired
      public WinlossService(WinlossAgentRepository winlossAgentRepository,
                            WinlossMemberRepository winlossMemberRepository,
                            UserServiceClient userServiceClient) {
          this.winlossAgentRepository = winlossAgentRepository;
          this.winlossMemberRepository = winlossMemberRepository;
          this.userServiceClient = userServiceClient;
      }
  */
    @Override
    public WinlossResponse reportWinloss(GetWinLossRequest request) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startDate = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 0, 0, 0);
        LocalDateTime endDate = LocalDateTime.of(now.getYear(), now.getMonth(), now.getDayOfMonth(), 23, 59, 59);

        if (request.getStartDate() != null && !request.getStartDate().isEmpty() && request.getEndDate() != null && !request.getEndDate().isEmpty()) {
            try {
                startDate = LocalDateTime.parse(request.getStartDate() + "T00:00:00");
                endDate = LocalDateTime.parse(request.getEndDate() + "T23:59:59");
            } catch (DateTimeParseException e) {
                //throw new BadRequestException("Failed to parse start_date", e);
            }
        }

        List<Criteria> criteriaList = new ArrayList<>();
        criteriaList.add(Criteria.where("startDate").gte(startDate).lte(endDate));

        String username = request.getUsername();
        String upline = request.getUpline();
        String collectionName;

        if (StringUtils.isNotEmpty(username)) {
            if ("account168".equals(username)) {
                criteriaList.add(Criteria.where("userType").is("company"));
            } else {
                criteriaList.add(Criteria.where("username").is(username));
            }
        }

        if (StringUtils.isNotEmpty(upline)) {
            upline = upline.replace("-member", "");
            criteriaList.add(Criteria.where("upline").is(upline));
        }

        if (username == null || username.isEmpty()) {
         //   throw new BadRequestException("Invalid username");
        }

        GetUserRequest userRequest = new GetUserRequest(username);

        GetUserResponse userDetails = memberClient.getUser(userRequest);

        if ("member".equals(userDetails.getType())) {
            collectionName = "winlossMembers";
        } else {
            collectionName = "winlossAgents";
        }

        if (!request.isViewSkipMember()) {
            collectionName = "winlossMembers";
        }

        if (StringUtils.isNotEmpty(request.getProductID() )) {
            criteriaList.add(Criteria.where("productId").is(request.getProductID()));
        }

        if (!CollectionUtils.isEmpty(request.getGameCategory())) {
            criteriaList.add(Criteria.where("gameCategory").in(request.getGameCategory()));
        }

        Criteria criteria = new Criteria().andOperator(criteriaList.toArray(new Criteria[0]));

        // Implement your filter and data retrieval logic here

        // Sample usage of calling gRPC service
        // UserResponse userDetails = userServiceClient.getUser(UserRequest.newBuilder().setUsername(request.getUsername()).build());
        List<Bson> pipeline = Arrays.asList(
                Aggregates.match(criteria.getCriteriaObject()),
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

List<Document> combinedResults=new ArrayList<>();

        if (collectionName.equals("winlossMembers")) {
            MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection("winlossMembers");
            MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
            while (cursor.hasNext()) {
                combinedResults.add(cursor.next());
            }
            cursor.close();

        } else {
            MongoCollection<Document> winLossAgentCollection = mongoTemplate.getCollection("winlossAgents");

            MongoCursor<Document> cursor = winLossAgentCollection.aggregate(pipeline).iterator();
            while (cursor.hasNext()) {
                combinedResults.add(cursor.next());
            }
            cursor.close();

        }
        if (combinedResults.isEmpty() && !"winlossMembers".equals(collectionName)) {
            MongoCollection<Document> winLossMemberCollection = mongoTemplate.getCollection("winlossMembers");
            MongoCursor<Document> cursor = winLossMemberCollection.aggregate(pipeline).iterator();
            while (cursor.hasNext()) {
                combinedResults.add(cursor.next());
            }
            cursor.close();
        }

        return formatResponse(combinedResults,request);
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
        baseCriteria.and("startDate").gte(startDate).lte(endDate);
    }

    if (request.getUsername() != null) {

        if ("account168".equals(request.getUsername())) {
            baseCriteria.and("userType").is("company");
        }else{
            baseCriteria.and("username").is(request.getUsername());
        }

    }

    if (request.getProductID() != null) {
        baseCriteria.and("productId").is(request.getProductID());
}

    MatchOperation match = Aggregation.match(baseCriteria);

    GroupOperation group = group("productId")
            .last("$$ROOT").as("doc")
            .sum("betCount").as("betCount")
            .sum("betAmount").as("betAmount")
            .sum("betTransferIn").as("betTransferIn")
            .sum("betTransferOut").as("betTransferOut")
            .sum("memberWinloss").as("memberWinloss")
            .sum("winCompany").as("winCompany")
            .sum("winAgentUp").as("winAgentUp")
            .sum("winProvider").as("winProvider");
    SortOperation sort = sort(ASC, "winCompany");
    ProjectionOperation project = project()
            .andExclude("_id")
            .andInclude("productId")
            .and("doc.productName").as("productName")
            .andInclude("betCount")
            .and(ArithmeticOperators.Round.roundValueOf("betAmount").place(2)).as("betAmount")
            .and(ArithmeticOperators.Round.roundValueOf("betTransferIn").place(2)).as("betTransferIn")
            .and(ArithmeticOperators.Round.roundValueOf("betTransferOut").place(2)).as("betTransferOut")
            .and(ArithmeticOperators.Round.roundValueOf("memberWinloss").place(2)).as("memberWinloss")
            .and(ArithmeticOperators.Round.roundValueOf("winAgentUp").place(2)).as("agent.remark")
            .and(ArithmeticOperators.Round.roundValueOf("winAgentUp").place(2)).as("agent.winloss")
            .and("0").as("agent.commission")
            .and(ArithmeticOperators.Round.roundValueOf("winAgentUp").place(2)).as("agent.winlossAfterCommission")
            .and("doc.upline").concat( " ","winloss").concat("doc.upline").as("company.remark")
            .and(ArithmeticOperators.Round.roundValueOf("winCompany").place(2)).as("company.winloss")
            .and("0").as("company.commission")
            .and(ArithmeticOperators.Round.roundValueOf("winCompany").place(2)).as("company.winlossAfterCommission")
            .and("provider winloss").as("provider.remark")
            .and(ArithmeticOperators.Round.roundValueOf("winProvider").place(2)).as("provider.winloss")
            .and("0").as("provider.commission")
            .and(ArithmeticOperators.Round.roundValueOf("winProvider").place(2)).as("provider.winlossAfterCommission")
            .and("doc.lastUpdate").as("lastUpdate");
    Aggregation aggregation = newAggregation(match, group, sort, project);
    AggregationResults<Document> results = mongoTemplate.aggregate(aggregation, "winlossAgents", Document.class);
    return formatResponse(results.getMappedResults(),request);
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
                    int betCount = item.getInteger("betCount");
                    totalBetCount += betCount;
                }
                if (item.containsKey("betAmount")) {
                    double betAmount = item.getDouble("betAmount");
                    totalBetAmount += betAmount;
                }
                if (item.containsKey("memberWinloss")) {
                    double memberWinloss = item.getDouble("memberWinloss");
                    totalMemberWinloss += memberWinloss;
                }
                if (item.containsKey("betTransferIn")) {
                    double betTransferIn = item.getDouble("betTransferIn");
                    totalBetTransferIn += betTransferIn;
                }
                if (item.containsKey("betTransferOut")) {
                    double betTransferOut = item.getDouble("betTransferOut");
                    totalBetTransferOut += betTransferOut;
                }
                if (item.containsKey("agent")) {
                    Document agentData = item.get("agent", Document.class);
                    if (agentData.containsKey("winloss")) {
                        double winloss = agentData.getDouble("winloss");
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
                    .totalProviderWinloss(totalProviderWinloss).request(request).lastUpdate(lastUpdate).build();

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
            Query query=new Query();
            Criteria criteria=Criteria.where("username").is(user.getUsername());
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
        entry.put("company",companyData);
        return entry;
  }



}
