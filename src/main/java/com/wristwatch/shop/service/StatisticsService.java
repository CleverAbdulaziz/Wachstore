package com.wristwatch.shop.service;

import com.wristwatch.shop.dto.SalesReportDto;
import com.wristwatch.shop.entity.Order;
import com.wristwatch.shop.repository.OrderRepository;
import com.wristwatch.shop.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StatisticsService {
    
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    
    public Map<String, Object> getDailySales() {
        LocalDateTime startOfDay = LocalDateTime.now().withHour(0).withMinute(0).withSecond(0);
        return getSalesStatistics(startOfDay);
    }
    
    public Map<String, Object> getWeeklySales() {
        LocalDateTime startOfWeek = LocalDateTime.now().minusDays(7);
        return getSalesStatistics(startOfWeek);
    }
    
    public Map<String, Object> getMonthlySales() {
        LocalDateTime startOfMonth = LocalDateTime.now().minusDays(30);
        return getSalesStatistics(startOfMonth);
    }
    
    public SalesReportDto getDetailedSalesReport(LocalDateTime startDate, LocalDateTime endDate) {
        SalesReportDto report = new SalesReportDto();
        report.setStartDate(startDate);
        report.setEndDate(endDate);
        report.setPeriod(calculatePeriodDescription(startDate, endDate));
        
        // Get orders in date range
        List<Order> orders = orderRepository.findOrdersBetweenDates(startDate, endDate)
                .stream()
                .filter(order -> order.getStatus() == Order.OrderStatus.PAID)
                .collect(Collectors.toList());
        
        // Calculate basic statistics
        report.setTotalOrders((long) orders.size());
        
        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        report.setTotalRevenue(totalRevenue);
        
        if (orders.size() > 0) {
            report.setAverageOrderValue(totalRevenue.divide(
                    BigDecimal.valueOf(orders.size()), 2, RoundingMode.HALF_UP));
        } else {
            report.setAverageOrderValue(BigDecimal.ZERO);
        }
        
        // Get top products
        List<Object[]> topProductsData = orderItemRepository.findTopSellingProductsSince(startDate);
        List<SalesReportDto.TopProductDto> topProducts = topProductsData.stream()
                .limit(10)
                .map(data -> {
                    SalesReportDto.TopProductDto topProduct = new SalesReportDto.TopProductDto();
                    topProduct.setProductName((String) data[0]);
                    topProduct.setQuantitySold((Long) data[1]);
                    return topProduct;
                })
                .collect(Collectors.toList());
        report.setTopProducts(topProducts);
        
        // Generate daily breakdown
        report.setDailyBreakdown(generateDailyBreakdown(orders, startDate, endDate));
        
        return report;
    }
    
    public List<Object[]> getTopSellingProducts(int days) {
        LocalDateTime startDate = LocalDateTime.now().minusDays(days);
        return orderItemRepository.findTopSellingProductsSince(startDate);
    }
    
    public Map<String, Object> getOrderStatusDistribution() {
        Map<String, Object> distribution = new HashMap<>();
        
        for (Order.OrderStatus status : Order.OrderStatus.values()) {
            Long count = (long) orderRepository.findByStatusOrderByCreatedAtDesc(status).size();
            distribution.put(status.name(), count);
        }
        
        return distribution;
    }
    
    public Map<String, Object> getRevenueGrowth() {
        Map<String, Object> growth = new HashMap<>();
        
        LocalDateTime thisMonth = LocalDateTime.now().minusDays(30);
        LocalDateTime lastMonth = LocalDateTime.now().minusDays(60);
        LocalDateTime twoMonthsAgo = LocalDateTime.now().minusDays(90);
        
        Double thisMonthRevenue = orderRepository.getTotalRevenueSince(thisMonth);
        Double lastMonthRevenue = orderRepository.getTotalRevenueSince(lastMonth) - 
                                 (thisMonthRevenue != null ? thisMonthRevenue : 0);
        
        growth.put("thisMonth", thisMonthRevenue != null ? thisMonthRevenue : 0.0);
        growth.put("lastMonth", lastMonthRevenue != null ? lastMonthRevenue : 0.0);
        
        if (lastMonthRevenue != null && lastMonthRevenue > 0) {
            double growthRate = ((thisMonthRevenue != null ? thisMonthRevenue : 0) - lastMonthRevenue) / lastMonthRevenue * 100;
            growth.put("growthRate", growthRate);
        } else {
            growth.put("growthRate", 0.0);
        }
        
        return growth;
    }
    
    private Map<String, Object> getSalesStatistics(LocalDateTime startDate) {
        Map<String, Object> stats = new HashMap<>();
        
        Long orderCount = orderRepository.countPaidOrdersSince(startDate);
        Double totalRevenue = orderRepository.getTotalRevenueSince(startDate);
        
        stats.put("orderCount", orderCount != null ? orderCount : 0L);
        stats.put("totalRevenue", totalRevenue != null ? totalRevenue : 0.0);
        stats.put("period", startDate);
        
        return stats;
    }
    
    private String calculatePeriodDescription(LocalDateTime startDate, LocalDateTime endDate) {
        long days = ChronoUnit.DAYS.between(startDate, endDate);
        
        if (days <= 1) {
            return "Daily";
        } else if (days <= 7) {
            return "Weekly";
        } else if (days <= 31) {
            return "Monthly";
        } else {
            return "Custom Period";
        }
    }
    
    private List<SalesReportDto.DailySalesDto> generateDailyBreakdown(
            List<Order> orders, LocalDateTime startDate, LocalDateTime endDate) {
        
        Map<String, SalesReportDto.DailySalesDto> dailyMap = new HashMap<>();
        
        // Initialize all days with zero values
        LocalDateTime current = startDate.truncatedTo(ChronoUnit.DAYS);
        while (!current.isAfter(endDate)) {
            String dateKey = current.toLocalDate().toString();
            SalesReportDto.DailySalesDto dailySales = new SalesReportDto.DailySalesDto();
            dailySales.setDate(current);
            dailySales.setOrders(0L);
            dailySales.setRevenue(BigDecimal.ZERO);
            dailyMap.put(dateKey, dailySales);
            current = current.plusDays(1);
        }
        
        // Fill in actual data
        for (Order order : orders) {
            String dateKey = order.getCreatedAt().toLocalDate().toString();
            SalesReportDto.DailySalesDto dailySales = dailyMap.get(dateKey);
            if (dailySales != null) {
                dailySales.setOrders(dailySales.getOrders() + 1);
                dailySales.setRevenue(dailySales.getRevenue().add(order.getTotalAmount()));
            }
        }
        
        return new ArrayList<>(dailyMap.values());
    }
}
