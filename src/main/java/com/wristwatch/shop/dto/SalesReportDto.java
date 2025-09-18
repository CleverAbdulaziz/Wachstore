package com.wristwatch.shop.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class SalesReportDto {
    
    private String period;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private Long totalOrders;
    private BigDecimal totalRevenue;
    private BigDecimal averageOrderValue;
    private List<TopProductDto> topProducts;
    private List<DailySalesDto> dailyBreakdown;
    
    @Data
    public static class TopProductDto {
        private String productName;
        private Long quantitySold;
        private BigDecimal revenue;
    }
    
    @Data
    public static class DailySalesDto {
        private LocalDateTime date;
        private Long orders;
        private BigDecimal revenue;
    }
}
