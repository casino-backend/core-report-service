package com.core.report.entities;

public class Downline {
    private double rate;
    private double rateValid;
    private String ID;
    private double winloss;
    private String username;
    private String userType;

    public Downline(double rate, double rateValid, String ID, double winloss, String username, String userType) {
        this.rate = rate;
        this.rateValid = rateValid;
        this.ID = ID;
        this.winloss = winloss;
        this.username = username;
        this.userType = userType;
    }

    public double getRate() {
        return rate;
    }

    public double getRateValid() {
        return rateValid;
    }

    public String getID() {
        return ID;
    }

    public double getWinloss() {
        return winloss;
    }

    public String getUsername() {
        return username;
    }

    public String getUserType() {
        return userType;
    }

    public void setRate(double rate) {
        this.rate = rate;
    }

    public void setRateValid(double rateValid) {
        this.rateValid = rateValid;
    }

    public void setID(String ID) {
        this.ID = ID;
    }

    public void setWinloss(double winloss) {
        this.winloss = winloss;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setUserType(String userType) {
        this.userType = userType;
    }

    }