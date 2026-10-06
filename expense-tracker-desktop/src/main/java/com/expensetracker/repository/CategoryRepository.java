package com.expensetracker.repository;

import com.expensetracker.model.Category;

import java.util.List;

public interface CategoryRepository {
    List<Category> findAll();
}
