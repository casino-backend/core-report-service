package com.core.report.controller;

import com.core.report.dto.GetWinLossRequest;
import com.core.report.dto.GetWinLossByProductRequest;
import com.core.report.service.WinlossService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertEquals;

//public class WinlossControllerTest {
//
//    @Mock
//    private WinlossService winlossService;
//
//    private WinlossController winlossController;
//
//    @Before
//    public void setUp() {
//        MockitoAnnotations.initMocks(this);
//        winlossController = new WinlossController(winlossService);
//    }
//
//    @Test
//    public void testReportWinloss() {
//        // Prepare a sample request
//        GetWinLossRequest request = new GetWinLossRequest(/* add request parameters */);
//
//        // Mock the behavior of the winlossService
//      //  when(winlossService.reportWinloss(request)).thenReturn(/* expected response */);
//
//        // Call the controller method
//        ResponseEntity<Object> responseEntity = winlossController.reportWinloss(request);
//
//        // Verify that the service method was called
//        verify(winlossService).reportWinloss(request);
//
//        // Verify the response
//      //  assertEquals(/* expected response */, responseEntity.getBody());
//    }
//
//    @Test
//    public void testReportWinlossByProduct() {
//        // Prepare a sample request
//        GetWinLossByProductRequest request = new GetWinLossByProductRequest(/* add request parameters */);
//
//        // Mock the behavior of the winlossService
//       // when(winlossService.reportWinlossByProduct(request)).thenReturn(/* expected response */);
//
//        // Call the controller method
//        ResponseEntity<Object> responseEntity = winlossController.reportWinlossByProduct(request);
//
//        // Verify that the service method was called
//        verify(winlossService).reportWinlossByProduct(request);
//
//        // Verify the response
//       // assertEquals(/* expected response */, responseEntity.getBody());
//    }
//}
