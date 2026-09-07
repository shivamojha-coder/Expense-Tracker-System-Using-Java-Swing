package com.expensetracker.service;

import com.expensetracker.model.User;

public interface AuthService {
    User login(String email, char[] password);

    User register(String email, char[] password);

    void logout();

    User getCurrentUser();

    /**
     * Returns the short-lived access token for the current session.
     *
     * <p>The default keeps non-Supabase test implementations source-compatible.
     * Data services must still rely on the authenticated user's id and Supabase
     * RLS for authorization.</p>
     */
    default String getAccessToken() {
        return "";
    }
}