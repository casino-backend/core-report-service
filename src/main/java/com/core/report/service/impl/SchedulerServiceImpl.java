package com.core.report.service.impl;

import com.core.report.constants.Constants;
import com.core.report.service.SchedulerService;
import com.core.report.service.WinlossAgentService;
import com.core.report.service.WinlossMemberService;
import org.bson.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class SchedulerServiceImpl implements SchedulerService {

    @Autowired
    private WinlossMemberService winLossMemberService;

    @Autowired
    private WinlossAgentService winLossAgentService;

    static List<String> getTimeHourLists(int hours) {
        List<String> data = new ArrayList<>();
        LocalTime startTime = LocalTime.MIDNIGHT;

        while (true) {
            LocalTime nextHour = startTime.plusHours(hours);
            if (nextHour.equals(LocalTime.MIDNIGHT)) {
                break;
            }
            String timeRange = String.format("%s-%s",
                    startTime.format(DateTimeFormatter.ofPattern("HH:mm:ss")),
                    nextHour.minusMinutes(1).format(DateTimeFormatter.ofPattern("HH:mm:ss")));
            data.add(timeRange);
            startTime = nextHour;
        }

        data.add("00:00:00-00:59:59");
        return data;
    }

    @Scheduled(cron = "0 */1 * * * *") // Runs every minute
    public void sumWinLossByMinute() {
        System.out.println("Running Scheduler........Summing WinLoss By Minute");

        LocalDateTime hStart = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(3);
        LocalDateTime hEnd = LocalDateTime.now(ZoneOffset.UTC);

        Map<String, Object> filter = new HashMap<>();
        filter.put("playdate", Map.of("$gte", hStart, "$lte", hEnd));
        filter.put("productId", Map.of("$ne", "withdrawal"));

        LocalDateTime startDate = LocalDateTime.of(hEnd.getYear(), hEnd.getMonth(), hEnd.getDayOfMonth(), hEnd.getHour(), 0, 0);
        LocalDateTime endDate = LocalDateTime.of(hEnd.getYear(), hEnd.getMonth(), hEnd.getDayOfMonth(), hEnd.getHour(), 59, 59, 999999999);

        ZonedDateTime startDateZonedDateTime = startDate.atZone(ZoneId.systemDefault());
        ZonedDateTime endDateZonedDateTime = endDate.atZone(ZoneId.systemDefault());

        try {
            winLossMemberService.sumWinLossMember(Date.from(startDateZonedDateTime.toInstant()), Date.from(endDateZonedDateTime.toInstant()), new Document(filter), true);
            winLossMemberService.processDailyWinLoss("");
        } catch (Exception e) {
            e.printStackTrace();
            // Handle the exception appropriately
        }
    }

    @Scheduled(cron = "0 0 0 * * *") // Runs every day at midnight
/*
    public void sumWinLossByDay(String date) {
        System.out.println("Running Scheduler........Summing WinLoss By Day");

        List<String> timeList = getTimeHourLists(1);
        LocalDate parsedDate = LocalDate.parse(date, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        System.out.printf("sum_date => %s\n", parsedDate.format(DateTimeFormatter.ISO_LOCAL_DATE));

        for (String timeRange : timeList) {
            String[] timeParts = timeRange.split("-");

            LocalTime startHour = LocalTime.parse(timeParts[0], DateTimeFormatter.ofPattern("HH:mm"));
            LocalTime endHour = LocalTime.parse(timeParts[1], DateTimeFormatter.ofPattern("HH:mm"));

            LocalDateTime startDateTime = LocalDateTime.of(parsedDate, startHour);
            LocalDateTime endDateTime = LocalDateTime.of(parsedDate, endHour);

            System.out.printf("From: %s, To: %s\n", startDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), endDateTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")));
            Document filter = new Document("$and", List.of(
                    new Document("playdate", new Document("$gte", startDateTime).append("$lte", endDateTime)),
                    new Document("productId", new Document("$ne", "withdrawal"))
            ));

            Instant instantStartDate = startDateTime.atZone(ZoneId.systemDefault()).toInstant();
            Instant instantEndDate = endDateTime.atZone(ZoneId.systemDefault()).toInstant();
            try {
                winLossMemberService.sumWinLossMember(Date.from(instantStartDate), Date.from(instantEndDate), filter, true);
                // Additional handling to mimic the Go filter logic
            } catch (Exception e) {
                e.printStackTrace();
                // Handle exception appropriately
            }
        }

        // Process daily winloss - translating the Go logic to Java
        try {
            winLossMemberService.processDailyWinLoss(parsedDate.toString());
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception appropriately
        }

        LocalDateTime startOfDay = LocalDateTime.of(parsedDate, LocalTime.MIN);
        LocalDateTime endOfDay = LocalDateTime.of(parsedDate, LocalTime.MAX);
        Instant instantStartDate = startOfDay.atZone(ZoneId.systemDefault()).toInstant();
        Instant instantEndDate = endOfDay.atZone(ZoneId.systemDefault()).toInstant();
        try {
            winLossAgentService.sumWinLossAgent(Date.from(instantStartDate), Date.from(instantEndDate), "");
        } catch (Exception e) {
            e.printStackTrace();
            // Handle exception appropriately
        }
    }
*/

    @Scheduled(cron = "0 0 * * * *") // Runs every hour
    public void sumWinLossByHour() {
        System.out.println("Running Scheduler........Summing WinLoss By Hour");

        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime startDate = now.withMinute(0).withSecond(0).withNano(0);
        LocalDateTime endDate = now.withMinute(59).withSecond(59).withNano(999999999);
        String formattedStartDate = startDate.format(Constants.DATE_TIME_FORMAT);
        String formattedEndDate = endDate.format(Constants.DATE_TIME_FORMAT);

        if (endDate.getHour() == 0 && endDate.getMinute() == 59) {
            throw new RuntimeException("Date range exceeds one day");
        }

        Document filter = new Document("$and", Arrays.asList(
                new Document("playdate", new Document("$gte", formattedStartDate).append("$lte", formattedEndDate)),
                new Document("productId", new Document("$ne", "withdrawal"))
        ));

        ZonedDateTime startDateZonedDateTime = startDate.atZone(ZoneId.systemDefault());
        ZonedDateTime endDateZonedDateTime = endDate.atZone(ZoneId.systemDefault());

        try {
            winLossMemberService.sumWinLossMember(Date.from(startDateZonedDateTime.toInstant()), Date.from(endDateZonedDateTime.toInstant()), filter, true);
            winLossMemberService.processDailyWinLoss("");
            winLossAgentService.sumWinLossAgent(null, null, "");
        } catch (Exception e) {
            // Log the exception and handle it appropriately
            e.printStackTrace();
        }
    }
}
