package com.neha.expensetracker;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/analytics")
public class InsightsController {

    private final ExpenseRepository expenseRepository;
    private final BudgetRepository budgetRepository;

    public InsightsController(ExpenseRepository expenseRepository,
                              BudgetRepository budgetRepository) {
        this.expenseRepository = expenseRepository;
        this.budgetRepository = budgetRepository;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    @GetMapping("/insights")
    public List<Map<String, String>> getInsights() {

        String username = getCurrentUsername();
        List<Map<String, String>> insights = new ArrayList<>();

        int currentMonth = LocalDate.now().getMonthValue();
        int currentYear = LocalDate.now().getYear();
        int lastMonth = currentMonth == 1 ? 12 : currentMonth - 1;
        int lastMonthYear = currentMonth == 1 ? currentYear - 1 : currentYear;

        List<Expense> thisMonthExpenses = expenseRepository
                .findByUsernameAndMonthAndYear(username, currentMonth, currentYear);

        List<Expense> lastMonthExpenses = expenseRepository
                .findByUsernameAndMonthAndYear(username, lastMonth, lastMonthYear);

        double totalThisMonth = thisMonthExpenses.stream()
                .mapToDouble(Expense::getAmount).sum();

        double totalLastMonth = lastMonthExpenses.stream()
                .mapToDouble(Expense::getAmount).sum();

        // ─── Insight 1: Spending increased or decreased ───
        if (totalLastMonth > 0) {
            double change = ((totalThisMonth - totalLastMonth)
                    / totalLastMonth) * 100;

            if (change > 20) {
                insights.add(Map.of(
                        "type", "warning",
                        "icon", "⚠️",
                        "title", "Spending spike!",
                        "message", String.format(
                                "You spent %.1f%% more than last month. " +
                                        "₹%.0f this month vs ₹%.0f last month.",
                                change, totalThisMonth, totalLastMonth)
                ));
            } else if (change < -10) {
                insights.add(Map.of(
                        "type", "success",
                        "icon", "🎉",
                        "title", "Great job saving!",
                        "message", String.format(
                                "You spent %.1f%% less than last month. " +
                                        "Keep it up!",
                                Math.abs(change))
                ));
            } else {
                insights.add(Map.of(
                        "type", "info",
                        "icon", "📊",
                        "title", "Spending stable",
                        "message", "Your spending is consistent with last month."
                ));
            }
        }

        // ─── Insight 2: Top spending category ───
        if (!thisMonthExpenses.isEmpty()) {
            Map<Category, Double> byCategory = thisMonthExpenses.stream()
                    .collect(Collectors.groupingBy(
                            Expense::getCategory,
                            Collectors.summingDouble(Expense::getAmount)
                    ));

            Category topCategory = Collections.max(
                    byCategory.entrySet(),
                    Map.Entry.comparingByValue()
            ).getKey();

            double topAmount = byCategory.get(topCategory);
            double topPercent = (topAmount / totalThisMonth) * 100;

            insights.add(Map.of(
                    "type", "info",
                    "icon", "🏆",
                    "title", "Top spending category",
                    "message", String.format(
                            "%s is your biggest expense this month " +
                                    "at ₹%.0f (%.1f%% of total spending).",
                            topCategory, topAmount, topPercent)
            ));

            // warn if one category is more than 50% of spending
            if (topPercent > 50) {
                insights.add(Map.of(
                        "type", "warning",
                        "icon", "🔍",
                        "title", "Spending concentrated!",
                        "message", String.format(
                                "Over half your spending goes to %s. " +
                                        "Consider balancing your expenses.",
                                topCategory)
                ));
            }
        }

        // ─── Insight 3: Budget exceeded ───
        List<Budget> budgets = budgetRepository
                .findByUserUsername(username);

        for (Budget budget : budgets) {
            List<Expense> categoryExpenses = expenseRepository
                    .findByUsernameAndCategoryAndMonthAndYear(
                            username, budget.getCategory(),
                            currentMonth, currentYear);

            double spent = categoryExpenses.stream()
                    .mapToDouble(Expense::getAmount).sum();

            double percentage = (spent / budget.getLimitAmount()) * 100;

            if (spent > budget.getLimitAmount()) {
                insights.add(Map.of(
                        "type", "danger",
                        "icon", "🚨",
                        "title", budget.getCategory() + " budget exceeded!",
                        "message", String.format(
                                "You've spent ₹%.0f on %s but your " +
                                        "limit is ₹%.0f. You're ₹%.0f over budget!",
                                spent, budget.getCategory(),
                                budget.getLimitAmount(),
                                spent - budget.getLimitAmount())
                ));
            } else if (percentage > 75) {
                insights.add(Map.of(
                        "type", "warning",
                        "icon", "⚠️",
                        "title", budget.getCategory() + " budget almost full!",
                        "message", String.format(
                                "You've used %.1f%% of your %s budget. " +
                                        "Only ₹%.0f remaining.",
                                percentage, budget.getCategory(),
                                budget.getLimitAmount() - spent)
                ));
            }
        }

        // ─── Insight 4: Weekend spending spike ───
        if (!thisMonthExpenses.isEmpty()) {
            double weekendSpending = thisMonthExpenses.stream()
                    .filter(e -> {
                        if (e.getDate() == null) return false;
                        var day = e.getDate().getDayOfWeek();
                        return day == java.time.DayOfWeek.SATURDAY
                                || day == java.time.DayOfWeek.SUNDAY;
                    })
                    .mapToDouble(Expense::getAmount)
                    .sum();

            double weekdaySpending = totalThisMonth - weekendSpending;

            // count weekends and weekdays in month
            long weekendDays = thisMonthExpenses.stream()
                    .filter(e -> e.getDate() != null)
                    .map(Expense::getDate)
                    .distinct()
                    .filter(d -> d.getDayOfWeek() == java.time.DayOfWeek.SATURDAY
                            || d.getDayOfWeek() == java.time.DayOfWeek.SUNDAY)
                    .count();

            long weekdays = thisMonthExpenses.stream()
                    .filter(e -> e.getDate() != null)
                    .map(Expense::getDate)
                    .distinct()
                    .filter(d -> d.getDayOfWeek() != java.time.DayOfWeek.SATURDAY
                            && d.getDayOfWeek() != java.time.DayOfWeek.SUNDAY)
                    .count();

            if (weekendDays > 0 && weekdays > 0) {
                double avgWeekend = weekendSpending / weekendDays;
                double avgWeekday = weekdaySpending / weekdays;

                if (avgWeekend > avgWeekday * 1.5) {
                    insights.add(Map.of(
                            "type", "warning",
                            "icon", "📅",
                            "title", "Weekend spending spike!",
                            "message", String.format(
                                    "You spend %.1fx more on weekends " +
                                            "(avg ₹%.0f/day) vs weekdays " +
                                            "(avg ₹%.0f/day).",
                                    avgWeekend / avgWeekday,
                                    avgWeekend, avgWeekday)
                    ));
                }
            }
        }

        // ─── Insight 5: Subscription detection ───
        if (!thisMonthExpenses.isEmpty()) {
            Map<String, Long> titleCount = thisMonthExpenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getTitle().toLowerCase(),
                            Collectors.counting()
                    ));

            // check last month too
            Map<String, Long> lastMonthTitleCount = lastMonthExpenses.stream()
                    .collect(Collectors.groupingBy(
                            e -> e.getTitle().toLowerCase(),
                            Collectors.counting()
                    ));

            List<String> possibleSubscriptions = titleCount.keySet()
                    .stream()
                    .filter(title -> lastMonthTitleCount.containsKey(title))
                    .collect(Collectors.toList());

            if (!possibleSubscriptions.isEmpty()) {
                String subList = possibleSubscriptions.stream()
                        .map(s -> s.substring(0, 1).toUpperCase()
                                + s.substring(1))
                        .collect(Collectors.joining(", "));

                insights.add(Map.of(
                        "type", "info",
                        "icon", "🔄",
                        "title", "Recurring expenses detected!",
                        "message", String.format(
                                "These appear every month: %s. " +
                                        "Make sure they're all still needed!",
                                subList)
                ));
            }
        }

        // ─── Insight 6: No expenses yet ───
        if (thisMonthExpenses.isEmpty()) {
            insights.add(Map.of(
                    "type", "info",
                    "icon", "👋",
                    "title", "No expenses this month yet!",
                    "message", "Start adding expenses to get " +
                            "personalized insights."
            ));
        }

        return insights;
    }
}
