package com.wristwatch.shop.controller;

import com.wristwatch.shop.dto.SalesReportDto;
import com.wristwatch.shop.service.StatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/statistics")
@RequiredArgsConstructor
public class StatisticsController {
    
    private final StatisticsService statisticsService;
    
    @GetMapping("/sales/daily")
    public ResponseEntity<Map<String, Object>> getDailySales() {
        Map<String, Object> stats = statisticsService.getDailySales();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/sales/weekly")
    public ResponseEntity<Map<String, Object>> getWeeklySales() {
        Map<String, Object> stats = statisticsService.getWeeklySales();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/sales/monthly")
    public ResponseEntity<Map<String, Object>> getMonthlySales() {
        Map<String, Object> stats = statisticsService.getMonthlySales();
        return ResponseEntity.ok(stats);
    }
    
    @GetMapping("/sales/report")
    public ResponseEntity<SalesReportDto> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        
        SalesReportDto report = statisticsService.getDetailedSalesReport(startDate, endDate);
        return ResponseEntity.ok(report);
    }
    
    @GetMapping("/products/top")
    public ResponseEntity<List<Object[]>> getTopProducts(@RequestParam(defaultValue = "30") int days) {
        List<Object[]> topProducts = statisticsService.getTopSellingProducts(days);
        return ResponseEntity.ok(topProducts);
    }
    
    @GetMapping("/orders/status-distribution")
    public ResponseEntity<Map<String, Object>> getOrderStatusDistribution() {
        Map<String, Object> distribution = statisticsService.getOrderStatusDistribution();
        return ResponseEntity.ok(distribution);
    }
    
    @GetMapping("/revenue/growth")
    public ResponseEntity<Map<String, Object>> getRevenueGrowth() {
        Map<String, Object> growth = statisticsService.getRevenueGrowth();
        return ResponseEntity.ok(growth);
    }
}
