package com.core.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;
@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class WinlossResponse {
    private int totalBetCount;
    private double totalBetAmount;
    private double totalMemberWinloss;
    private double totalBetTransferIn;
    private double totalBetTransferOut;
    private double totalAgentWinloss;
    private double totalAgentWinAgentValid;
    private double totalCompanyWinloss;
    private double totalProviderWinloss;
    private String lastUpdate;
    private List<Map<String, Object>> items;
    private Object request;

    // Getters and setters for all fields
    @Override
    public String toString() {
        return "WinlossResponse{" +
                "totalBetCount=" + totalBetCount +
                ", totalBetAmount=" + totalBetAmount +
                ", totalMemberWinloss=" + totalMemberWinloss +
                ", totalBetTransferIn=" + totalBetTransferIn +
                ", totalBetTransferOut=" + totalBetTransferOut +
                ", totalAgentWinloss=" + totalAgentWinloss +
                ", totalAgentWinAgentValid=" + totalAgentWinAgentValid +
                ", totalCompanyWinloss=" + totalCompanyWinloss +
                ", totalProviderWinloss=" + totalProviderWinloss +
                ", lastUpdate='" + lastUpdate + '\'' +
                ", items=" + items +
                ", request=" + request +
                '}';
    }

}