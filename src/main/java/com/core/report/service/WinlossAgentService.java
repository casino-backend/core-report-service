package com.core.report.service;

import java.util.Date;

public interface WinlossAgentService {

    void sumWinLossAgent(Date oldStartDate, Date oldEndDate, String upline) throws Exception;
}
