package com.core.report.service;

import com.core.report.dto.GetWinLossByProductRequest;
import com.core.report.dto.GetWinLossRequest;

public interface WinlossService {

    Object reportWinloss(GetWinLossRequest getWinLossRequest);
    Object reportWinlossByProduct(GetWinLossByProductRequest getWinLossByProductRequest);
}
