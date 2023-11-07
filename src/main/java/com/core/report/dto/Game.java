package com.core.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Game {
    private String category;
    private String productId;
    private String productName;
    private double rate;
    private String provider;
    private String status;
}
