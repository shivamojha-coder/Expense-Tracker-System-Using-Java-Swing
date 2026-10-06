package com.expensetracker.settings;

import com.expensetracker.model.PaymentMethod;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

/** Loads and saves {@link UserSettings} for one user using the operating system's preference store. */
public final class SettingsStore {
    private static final String ROOT = "expense-tracker-desktop/users/";

    private final Preferences node;
    private final List<Consumer<UserSettings>> listeners = new CopyOnWriteArrayList<>();
    private UserSettings current;

    public SettingsStore(UUID userId) {
        this.node = Preferences.userRoot().node(ROOT + userId);
        this.current = read();
    }

    public synchronized UserSettings get() {
        return current;
    }

    public synchronized void save(UserSettings settings) {
        current = settings;
        node.put("currency", settings.currency().name());
        node.put("dateStyle", settings.dateStyle().name());
        putOrRemove("defaultPayment", settings.defaultPayment() == null ? null : settings.defaultPayment().name());
        putOrRemove("defaultCategory", settings.defaultCategory());
        node.put("ocrLanguage", settings.ocrLanguage());
        node.putBoolean("autoScan", settings.autoScan());
        node.putInt("alertThreshold", settings.alertThreshold());
        flush();
        listeners.forEach(listener -> listener.accept(settings));
    }

    public synchronized void reset() {
        save(UserSettings.defaults());
    }

    /** Removes everything stored for this user (used when the account is deleted). */
    public synchronized void clear() {
        try {
            node.removeNode();
            node.flush();
        } catch (BackingStoreException ignored) {
            // Nothing else to do: the preferences simply stay on disk.
        }
    }

    public void addListener(Consumer<UserSettings> listener) {
        listeners.add(listener);
    }

    private UserSettings read() {
        UserSettings d = UserSettings.defaults();
        return new UserSettings(
                enumValue(CurrencyOption.class, node.get("currency", null), d.currency()),
                enumValue(DateStyle.class, node.get("dateStyle", null), d.dateStyle()),
                enumValue(PaymentMethod.class, node.get("defaultPayment", null), null),
                node.get("defaultCategory", null),
                node.get("ocrLanguage", d.ocrLanguage()),
                node.getBoolean("autoScan", d.autoScan()),
                Math.max(50, Math.min(100, node.getInt("alertThreshold", d.alertThreshold())))
        );
    }

    private void putOrRemove(String key, String value) {
        if (value == null || value.isBlank()) {
            node.remove(key);
        } else {
            node.put(key, value);
        }
    }

    private void flush() {
        try {
            node.flush();
        } catch (BackingStoreException ignored) {
            // The in-memory value is still correct for this session.
        }
    }

    private static <E extends Enum<E>> E enumValue(Class<E> type, String name, E fallback) {
        if (name == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(type, name);
        } catch (IllegalArgumentException exception) {
            return fallback;
        }
    }
}
