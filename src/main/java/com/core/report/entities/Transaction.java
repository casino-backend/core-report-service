package com.core.report.entities;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Document;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Document(collection = "transactions")
public class Transaction {
    private String actionType;
    private String action;
    private double beforeBalance;
    private double amount;
    private double afterBalance;
    private String roundId;
    private String gameId;
    private String gameName;
    private String gameCategory;
    private boolean isFeatureBuy;
    private boolean endRound;
    private String username;
    private String upline;
    private String refSale;
    private String description;
    private String productId;
    private String productName;
    private String provider;
    private double percentage;
    private String syncDate;
    private String createdAt;
    private String createdAtIso;
    private String created;
    private String ip;
    private boolean fRunning;
    private String fRunningDate;

    // Getters and setters for all fields

    @Override
    public String toString() {
        return "Transaction{" +
                "actionType='" + actionType + '\'' +
                ", action='" + action + '\'' +
                ", beforeBalance=" + beforeBalance +
                ", amount=" + amount +
                ", afterBalance=" + afterBalance +
                ", roundId='" + roundId + '\'' +
                ", gameId='" + gameId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", gameCategory='" + gameCategory + '\'' +
                ", isFeatureBuy=" + isFeatureBuy +
                ", endRound=" + endRound +
                ", username='" + username + '\'' +
                ", upline='" + upline + '\'' +
                ", refSale='" + refSale + '\'' +
                ", description='" + description + '\'' +
                ", productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", provider='" + provider + '\'' +
                ", percentage=" + percentage +
                ", syncDate='" + syncDate + '\'' +
                ", createdAt='" + createdAt + '\'' +
                ", createdAtIso='" + createdAtIso + '\'' +
                ", created='" + created + '\'' +
                ", ip='" + ip + '\'' +
                ", fRunning=" + fRunning +
                ", fRunningDate='" + fRunningDate + '\'' +
                '}';
    }
}


