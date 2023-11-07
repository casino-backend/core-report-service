package com.core.report.dto;

import java.util.List;
import java.util.Map;

public class GetWinLossRequest {
    private String username;
    private String upline;
    private String productID;
    private boolean viewSkipMember;
    private List<String> gameCategory;
    private String startDate;
    private String endDate;

    // Getters and setters for all fields

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getUpline() {
        return upline;
    }

    public void setUpline(String upline) {
        this.upline = upline;
    }

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
    }

    public boolean isViewSkipMember() {
        return viewSkipMember;
    }

    public void setViewSkipMember(boolean viewSkipMember) {
        this.viewSkipMember = viewSkipMember;
    }

    public List<String> getGameCategory() {
        return gameCategory;
    }

    public void setGameCategory(List<String> gameCategory) {
        this.gameCategory = gameCategory;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    @Override
    public String toString() {
        return "GetWinLossRequest{" +
                "username='" + username + '\'' +
                ", upline='" + upline + '\'' +
                ", productID='" + productID + '\'' +
                ", viewSkipMember=" + viewSkipMember +
                ", gameCategory=" + gameCategory +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                '}';
    }
}




