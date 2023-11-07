package com.core.report.entities;

public class TransactionReport {
    private String id;
    private String roundId;
    private String actionType;
    private String action;
    private double afterBalance;
    private double beforeBalance;
    private boolean isFeatureBuy;
    private boolean endRound;
    private double amountDeposit;
    private double amountWithdraw;
    private double betTransferIn;
    private double betTransferOut;
    private double betAmount;
    private double betResult;
    private double betWinLoss;
    private String username;
    private String upline;
    private String refSale;
    private String gameId;
    private String gameName;
    private String gameCategory;
    private double memberWinLoss;
    private double uplineWinLoss;
    private String productId;
    private String productName;
    private String provider;
    private double percentage;
    private String createdAt;
    private String created;
    private String createdIso;
    private String playDate;
    private String description;
    private String txnid;
    private String ip;
    private String status;

    // Getters and setters for all fields

    @Override
    public String toString() {
        return "TransactionReport{" +
                "id='" + id + '\'' +
                ", roundId='" + roundId + '\'' +
                ", actionType='" + actionType + '\'' +
                ", action='" + action + '\'' +
                ", afterBalance=" + afterBalance +
                ", beforeBalance=" + beforeBalance +
                ", isFeatureBuy=" + isFeatureBuy +
                ", endRound=" + endRound +
                ", amountDeposit=" + amountDeposit +
                ", amountWithdraw=" + amountWithdraw +
                ", betTransferIn=" + betTransferIn +
                ", betTransferOut=" + betTransferOut +
                ", betAmount=" + betAmount +
                ", betResult=" + betResult +
                ", betWinLoss=" + betWinLoss +
                ", username='" + username + '\'' +
                ", upline='" + upline + '\'' +
                ", refSale='" + refSale + '\'' +
                ", gameId='" + gameId + '\'' +
                ", gameName='" + gameName + '\'' +
                ", gameCategory='" + gameCategory + '\'' +
                ", memberWinLoss=" + memberWinLoss +
                ", uplineWinLoss=" + uplineWinLoss +
                ", productId='" + productId + '\'' +
                ", productName='" + productName + '\'' +
                ", provider='" + provider + '\'' +
                ", percentage=" + percentage +
                ", createdAt='" + createdAt + '\'' +
                ", created='" + created + '\'' +
                ", createdIso='" + createdIso + '\'' +
                ", playDate='" + playDate + '\'' +
                ", description='" + description + '\'' +
                ", txnid='" + txnid + '\'' +
                ", ip='" + ip + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}