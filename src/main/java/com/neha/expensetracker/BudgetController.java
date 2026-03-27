package com.neha.expensetracker;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/budget")
public class BudgetController {

    private final BudgetRepository budgetRepository;
    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public BudgetController(BudgetRepository budgetRepository,
                            ExpenseRepository expenseRepository,
                            UserRepository userRepository) {
        this.budgetRepository = budgetRepository;
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    private String getCurrentUsername() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    private User getCurrentUser() {
        return userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    // Set or update a budget for a category
    @PostMapping("/set")
    public ResponseEntity<Budget> setBudget(
            @RequestBody Budget budgetRequest) {

        String username = getCurrentUsername();

        // if budget already exists for this category, update it
        Optional<Budget> existing = budgetRepository
                .findByUserUsernameAndCategory(
                        username, budgetRequest.getCategory());

        Budget budget = existing.orElse(new Budget());
        budget.setCategory(budgetRequest.getCategory());
        budget.setLimitAmount(budgetRequest.getLimitAmount());
        budget.setUser(getCurrentUser());

        return ResponseEntity.ok(budgetRepository.save(budget));
    }

    // Get budget status for all categories
    @GetMapping("/status")
    public List<Map<String, Object>> getBudgetStatus() {

        String username = getCurrentUsername();
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        List<Budget> budgets = budgetRepository
                .findByUserUsername(username);

        List<Map<String, Object>> statusList = new ArrayList<>();

        for (Budget budget : budgets) {

            // get total spent in this category this month
            List<Expense> expenses = expenseRepository
                    .findByUsernameAndCategoryAndMonthAndYear(
                            username, budget.getCategory(), month, year);

            double spent = expenses.stream()
                    .mapToDouble(Expense::getAmount)
                    .sum();

            double remaining = budget.getLimitAmount() - spent;
            boolean exceeded = spent > budget.getLimitAmount();

            Map<String, Object> status = new LinkedHashMap<>();
            status.put("category", budget.getCategory());
            status.put("limit", budget.getLimitAmount());
            status.put("spent", spent);
            status.put("remaining", Math.abs(remaining));
            status.put("exceeded", exceeded);
            status.put("status", exceeded ? "⚠️ EXCEEDED" : "✅ ON TRACK");

            statusList.add(status);
        }

        return statusList;
    }

    // Get budget status for one specific category
    @GetMapping("/status/{category}")
    public ResponseEntity<Map<String, Object>> getCategoryStatus(
            @PathVariable Category category) {

        String username = getCurrentUsername();
        int month = LocalDate.now().getMonthValue();
        int year = LocalDate.now().getYear();

        Optional<Budget> budgetOpt = budgetRepository
                .findByUserUsernameAndCategory(username, category);

        if (budgetOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Budget budget = budgetOpt.get();

        List<Expense> expenses = expenseRepository
                .findByUsernameAndCategoryAndMonthAndYear(
                        username, category, month, year);

        double spent = expenses.stream()
                .mapToDouble(Expense::getAmount)
                .sum();

        double remaining = budget.getLimitAmount() - spent;
        boolean exceeded = spent > budget.getLimitAmount();

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("category", category);
        status.put("limit", budget.getLimitAmount());
        status.put("spent", spent);
        status.put("remaining", Math.abs(remaining));
        status.put("exceeded", exceeded);
        status.put("status", exceeded ? "⚠️ EXCEEDED" : "✅ ON TRACK");

        return ResponseEntity.ok(status);
    }
}
