package com.core.report.service;

import org.bson.Document;

import java.util.Date;

public interface WinlossMemberService {

     void sumWinLossMember(Date startDate, Date endDate, Document filter, boolean byHour) throws Exception;
     void processDailyWinLoss(String sumDate);

    }
