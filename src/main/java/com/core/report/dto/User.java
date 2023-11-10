package com.core.report.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class User implements UserDetails {

    private String username;
    private String type;
    private List<Game> gamesList;

    @Override
    public String getType() {
        return type;
    }

}
