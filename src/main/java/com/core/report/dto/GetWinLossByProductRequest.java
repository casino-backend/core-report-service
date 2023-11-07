package com.core.report.dto;

public class GetWinLossByProductRequest {
    private String productID;
    private String startDate;
    private String endDate;
    private String username;

    // Getters and setters for all fields

    public String getProductID() {
        return productID;
    }

    public void setProductID(String productID) {
        this.productID = productID;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    @Override
    public String toString() {
        return "GetWinLossByProductRequest{" +
                "productID='" + productID + '\'' +
                ", startDate='" + startDate + '\'' +
                ", endDate='" + endDate + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}