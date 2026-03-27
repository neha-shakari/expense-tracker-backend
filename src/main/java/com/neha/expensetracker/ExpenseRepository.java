package com.neha.expensetracker;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {
    List<Expense> findByUserUsername(String username);
    List<Expense> findByUserUsernameAndCategory(String username, Category category);
}
