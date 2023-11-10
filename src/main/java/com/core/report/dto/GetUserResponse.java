package com.core.report.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GetUserResponse {
    private long id;
    private String username;
    private String type;
    private String parentTopUsername;
    private String refSale;
    private String upline;
    private String uCompany;
    private List<Game> games;
}
