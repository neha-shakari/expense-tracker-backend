package com.neha.expensetracker;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
public class AnalyticsController {

    private final ExpenseRepository expenseRepository;

    public AnalyticsController(ExpenseRepository expenseRepository) {
        this.expenseRepository = expenseRepository;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    // Total spent this month
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        String username = getCurrentUsername();
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        List<Expense> thisMonth = expenseRepository
                .findByUsernameAndMonthAndYear(username, month, year);

        List<Expense> lastMonth = expenseRepository
                .findByUsernameAndMonthAndYear(
                        username,
                        month == 1 ? 12 : month - 1,
                        month == 1 ? year - 1 : year
                );

        double totalThisMonth = thisMonth.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        double totalLastMonth = lastMonth.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        String comparison;
        if (totalLastMonth == 0) {
            comparison = "No data for last month";
        } else {
            double change = ((totalThisMonth - totalLastMonth)
                    / totalLastMonth) * 100;
            comparison = String.format("%.1f%%", change);
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("month", month);
        summary.put("year", year);
        summary.put("totalThisMonth", totalThisMonth);
        summary.put("totalLastMonth", totalLastMonth);
        summary.put("changeVsLastMonth", comparison);
        summary.put("totalExpenses", thisMonth.size());

        return summary;
    }

    // Spending broken down by category
    @GetMapping("/by-category")
    public Map<String, Double> getByCategory() {

        String username = getCurrentUsername();
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        List<Expense> expenses = expenseRepository
                .findByUsernameAndMonthAndYear(username, month, year);

        return expenses.stream()
                .collect(Collectors.groupingBy(
                        e -> e.getCategory().name(),
                        Collectors.summingDouble(Expense::getAmount)
                ));
    }

    // Highest spending category this month
    @GetMapping("/highest-category")
    public Map<String, Object> getHighestCategory() {

        Map<String, Double> byCategory = getByCategory();

        Map<String, Object> result = new LinkedHashMap<>();

        if (byCategory.isEmpty()) {
            result.put("message", "No expenses this month");
            return result;
        }

        String highest = Collections.max(
                byCategory.entrySet(),
                Map.Entry.comparingByValue()
        ).getKey();

        result.put("highestCategory", highest);
        result.put("amount", byCategory.get(highest));
        return result;
    }
}
