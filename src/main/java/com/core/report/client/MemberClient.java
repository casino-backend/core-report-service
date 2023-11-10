package com.core.report.client;

import com.core.report.dto.GetParentsRequest;
import com.core.report.dto.GetParentsResponse;
import com.core.report.dto.GetUserRequest;
import com.core.report.dto.GetUserResponse;

public interface MemberClient {
    GetUserResponse getUser(GetUserRequest userRequest);

    GetUserResponse getAgentUser(GetUserRequest getAgentRequest);

    GetParentsResponse getUserParents(GetParentsRequest request);

    double fetchAgentGameRate(String username, String productId);
}
