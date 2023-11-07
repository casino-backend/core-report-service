package com.core.report.utils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Transformer {

    public static Map<String, Object> winlossSchemaTransformer(Date startDate, Date endDate, String productId, Map<String, Object> key) {
        Map<String, Object> newData = new HashMap<>();

        // Extracting playdate and formatting it
        Object playdateObject = key.get("playdate");
        if (playdateObject == null || !(playdateObject instanceof String)) {
            throw new IllegalArgumentException("playdate field is missing or not a string");
        }

        String playdate = (String) playdateObject;
        String dateOnly = playdate.split(" ")[0];

        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");
        Date playdateTime;
        try {
            playdateTime = dateFormat.parse(dateOnly);
        } catch (ParseException e) {
            throw new IllegalArgumentException("Failed to parse playdate");
        }
        SimpleDateFormat datestampFormat = new SimpleDateFormat("yyyyMMdd");
        String datestamp = datestampFormat.format(playdateTime);

        SimpleDateFormat layout = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        newData.put("startDate", layout.format(startDate));
        newData.put("endDate", layout.format(endDate));
        newData.put("productId", productId);
        newData.put("productName", key.get("productName"));
        newData.put("gameCategory", key.get("gameCategory"));
        newData.put("gameProvider", key.get("gameProvider"));
        newData.put("betCount", key.get("betCount"));
        newData.put("betAmount", key.get("betAmount"));
        newData.put("betWinloss", key.get("betWinloss"));
        newData.put("betTransferIn", key.get("betTransferIn"));
        newData.put("betTransferOut", key.get("betTransferOut"));
        newData.put("memberWinloss", key.get("memberWinloss"));
        newData.put("createdAt", key.get("createdAt"));
        newData.put("playdate", playdate);
        newData.put("datestamp", datestamp);
        newData.put("lastUpdate", layout.format(new Date()));

        // Default value for ref_sale
        Object refSaleObject = key.get("refSale");
        String refSale = (refSaleObject instanceof String) ? (String) refSaleObject : "";

        newData.put("refSale", refSale);
        return newData;
    }
}
