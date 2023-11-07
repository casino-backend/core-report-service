package com.core.report.client;

import com.core.report.dto.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@Slf4j
public class MemberClientImpl implements MemberClient {


    private final WebClient webClient;
    @Value("${services.memberService.url}")
    String baseUrl;

    public MemberClientImpl() {
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl) // Replace with the actual API endpoint
                .build();
    }
@Override
    public GetUserResponse getUser(GetUserRequest request) {

        return webClient.post()
                .uri("/game/UserService/GetUser")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GetUserResponse.class).block();
    }

    @Override
    public GetUserResponse getAgentUser(GetUserRequest getAgentRequest) {
        return null;
    }

    @Override
    public GetParentsResponse getUserParents(GetParentsRequest request) {
        return webClient.post()
                .uri("/game/UserService/GetUserParents")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(GetParentsResponse.class).block();
    }
@Override
    public double fetchAgentGameRate(String username, String productId) {
        String url = "/getAgentGameRate"; // Replace with the actual endpoint

        Mono<GetUserResponse> responseMono = webClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(GetUserResponse.class);

        try {
            GetUserResponse response = responseMono.block(); // Blocking until the response is received

            if (response != null) {
                double rate = processResponse(response, productId);
                return rate;
            } else {
                log.info("Failed to fetch upline games: Empty response");
                return 0.0;
            }
        } catch (Exception e) {
            log.info("Failed to fetch upline games: " + e.getMessage());
            return 0.0;
        }
    }

    private double processResponse(GetUserResponse response, String productId) {
        double rate = 0.0;

        for (Game game : response.getGames()) {
            if (game.getProductId().equals(productId) && game.getStatus().equals("A")) {
                rate = game.getRate();
                break;
            }
        }

        if (rate == 0.0) {
            log.info("Failed to get upline game rate");
        }

        return rate;
    }

  /*  @Override
    public User getUser(GetUserRequest userRequest) {
        //NEED TO ADD LOGIC TO FETCH DATA

        return new User();
    }*/



}
