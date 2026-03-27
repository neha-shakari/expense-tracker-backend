package com.neha.expensetracker;

import jakarta.validation.Valid;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/expenses")
public class ExpenseController {

    private final ExpenseRepository expenseRepository;
    private final UserRepository userRepository;

    public ExpenseController(ExpenseRepository expenseRepository,
                             UserRepository userRepository) {
        this.expenseRepository = expenseRepository;
        this.userRepository = userRepository;
    }

    // gets the username from the JWT token
    private String getCurrentUsername() {
        return SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getName();
    }

    // gets the full User object from database
    private User getCurrentUser() {
        return userRepository.findByUsername(getCurrentUsername())
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.UNAUTHORIZED));
    }

    @GetMapping
    public List<Expense> getAllExpenses(
            @RequestParam(required = false) Category category,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year) {

        String username = getCurrentUsername();

        // both category and date filter
        if (category != null && month != null && year != null) {
            return expenseRepository
                    .findByUsernameAndCategoryAndMonthAndYear(
                            username, category, month, year);
        }

        // only date filter
        if (month != null && year != null) {
            return expenseRepository
                    .findByUsernameAndMonthAndYear(
                            username, month, year);
        }

        // only category filter
        if (category != null) {
            return expenseRepository
                    .findByUsernameAndCategoryAndMonthAndYear(
                            username, category, month, year);
        }

        // no filter → return all
        return expenseRepository.findByUserUsername(username);
    }

    @GetMapping("/{id}")
    public Expense getExpenseById(@PathVariable Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Expense not found"));

        if (!expense.getUser().getUsername().equals(getCurrentUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        return expense;
    }

    @PostMapping
    public Expense addExpense(@Valid @RequestBody Expense expense) {
        expense.setUser(getCurrentUser());

        // auto set date to today if not provided
        if (expense.getDate() == null) {
            expense.setDate(LocalDate.now());
        }

        return expenseRepository.save(expense);
    }

    @PutMapping("/{id}")
    public Expense updateExpense(@PathVariable Long id,
                                 @Valid @RequestBody Expense updatedExpense) {

        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Expense not found"));

        if (!expense.getUser().getUsername().equals(getCurrentUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        expense.setTitle(updatedExpense.getTitle());
        expense.setAmount(updatedExpense.getAmount());
        return expenseRepository.save(expense);
    }

    @DeleteMapping("/{id}")
    public void deleteExpense(@PathVariable Long id) {
        Expense expense = expenseRepository.findById(id)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND, "Expense not found"));

        if (!expense.getUser().getUsername().equals(getCurrentUsername())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        expenseRepository.delete(expense);
    }
}
