package com.expensetracker.service;

import com.expensetracker.model.User;

public interface AuthService {
    User login(String email, char[] password);

    User register(String email, char[] password);

    void logout();

    User getCurrentUser();
}