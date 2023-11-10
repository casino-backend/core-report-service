package com.core.report.controller;

import com.core.report.dto.GetWinLossByProductRequest;
import com.core.report.dto.GetWinLossRequest;
import com.core.report.service.WinlossService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/winloss")
public class WinlossController {

    private final WinlossService winlossService;

    @Autowired
    public WinlossController(WinlossService winlossService) {
        this.winlossService = winlossService;
    }

    @PostMapping("/report")
    public ResponseEntity<Object> reportWinloss(@RequestBody GetWinLossRequest getWinlossRequest) {
        try {
            return ResponseEntity.ok(winlossService.reportWinloss(getWinlossRequest));
        } catch (Exception e) {
            throw new InternalError("unexpected error occurred", e);
        }
    }

    @PostMapping("/reportByProduct")
    public ResponseEntity<Object> reportWinlossByProduct(@RequestBody GetWinLossByProductRequest getWinlossRequest) {
        try {
            return ResponseEntity.ok(winlossService.reportWinlossByProduct(getWinlossRequest));
        } catch (Exception e) {
            throw new InternalError("unexpected error occurred", e);
        }
    }
}
