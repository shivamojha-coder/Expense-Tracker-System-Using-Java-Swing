package com.expensetracker.service;

import com.expensetracker.model.User;

public final class DemoAuthService implements AuthService {
    private User currentUser;

    @Override
    public User login(String email, char[] password) {
        currentUser = new User(DemoDataService.DEMO_USER_ID, (email != null && !email.isBlank()) ? email : DemoDataService.DEMO_USER_EMAIL);
        return currentUser;
    }

    @Override
    public User register(String email, char[] password) {
        currentUser = new User(DemoDataService.DEMO_USER_ID, (email != null && !email.isBlank()) ? email : DemoDataService.DEMO_USER_EMAIL);
        return currentUser;
    }

    @Override
    public void logout() {
        currentUser = null;
    }

    @Override
    public String getAccessToken() {
        return "demo-session-token";
    }

    @Override
    public User getCurrentUser() {
        return currentUser;
    }
}
