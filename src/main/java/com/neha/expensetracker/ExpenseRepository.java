package com.neha.expensetracker;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface ExpenseRepository extends JpaRepository<Expense, Long> {

    List<Expense> findByUserUsername(String username);

    List<Expense> findByUserUsernameAndCategory(
            String username, Category category);

    @Query("SELECT e FROM Expense e WHERE e.user.username = :username " +
            "AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    List<Expense> findByUsernameAndMonthAndYear(
            @Param("username") String username,
            @Param("month") int month,
            @Param("year") int year);

    @Query("SELECT e FROM Expense e WHERE e.user.username = :username " +
            "AND e.category = :category " +
            "AND MONTH(e.date) = :month AND YEAR(e.date) = :year")
    List<Expense> findByUsernameAndCategoryAndMonthAndYear(
            @Param("username") String username,
            @Param("category") Category category,
            @Param("month") int month,
            @Param("year") int year);
}
