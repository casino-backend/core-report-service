package com.core.report.dto;

public class AgentGameDetails {
    private Long id;
    private String username;
    private String upline;
    private Double rate;
    private Double rateLimit;
    private String productId;
    private String status;
    private String productName;
    private String category;
    private String provider;
    private String callbackUrl;
    private String note;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Double getRateLimit() {
        return rateLimit;
    }

    public void setRateLimit(Double rateLimit) {
        this.rateLimit = rateLimit;
    }

    public String getProductId() {
        return productId;
    }

    public void setProductId(String productId) {
        this.productId = productId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getCallbackUrl() {
        return callbackUrl;
    }

    public void setCallbackUrl(String callbackUrl) {
        this.callbackUrl = callbackUrl;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    @Override
    public String toString() {
        return "AgentGameDetails{" +
                "id=" + id +
                ", username='" + username + '\'' +
                ", upline='" + upline + '\'' +
                ", rate=" + rate +
                ", rateLimit=" + rateLimit +
                ", productId='" + productId + '\'' +
                ", status='" + status + '\'' +
                ", productName='" + productName + '\'' +
                ", category='" + category + '\'' +
                ", provider='" + provider + '\'' +
                ", callbackUrl='" + callbackUrl + '\'' +
                ", note='" + note + '\'' +
                '}';
    }
}
