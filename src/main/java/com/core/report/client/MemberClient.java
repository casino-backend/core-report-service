package com.core.report.client;

import com.core.report.dto.*;

public interface MemberClient {
    GetUserResponse getUser(GetUserRequest userRequest);

    GetUserResponse getAgentUser(GetUserRequest getAgentRequest);
    GetParentsResponse getUserParents(GetParentsRequest request);

     double fetchAgentGameRate(String username, String productId);


    }
