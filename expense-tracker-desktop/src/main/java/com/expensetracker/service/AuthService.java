package com.expensetracker.service;

import com.expensetracker.model.User;

public interface AuthService {
    User login(String email, char[] password);

    User register(String email, char[] password);

    void logout();

    /** Sends a password-reset email; services without an email backend keep this default. */
    default void sendPasswordReset(String email) {
        throw new ServiceException("Password reset is not available in this mode.");
    }

    /** The signed-in user's display name, or an empty string when none is set. */
    default String getDisplayName() {
        return "";
    }

    default void updateDisplayName(String displayName) {
        throw new ServiceException("Changing the display name is not available in this mode.");
    }

    /** Sets a new password for the signed-in user. */
    default void changePassword(char[] newPassword) {
        throw new ServiceException("Changing the password is not available in this mode.");
    }

    /**
     * Deletes the signed-in user's login. With {@code confirm == false} nothing is deleted;
     * the call only verifies that deletion is possible so callers can fail before removing other data.
     */
    default void deleteAccount(boolean confirm) {
        throw new ServiceException("Deleting the account is not available in this mode.");
    }

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