package com.neha.expensetracker;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BudgetRepository extends JpaRepository<Budget, Long> {
    List<Budget> findByUserUsername(String username);
    Optional<Budget> findByUserUsernameAndCategory(
            String username, Category category);
}