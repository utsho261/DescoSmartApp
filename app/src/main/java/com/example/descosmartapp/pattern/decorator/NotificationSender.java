package com.example.descosmartapp.pattern.decorator;

/**
 * DECORATOR PATTERN — Notifications can be enhanced with different behaviors
 */
public interface NotificationSender {
    void send(String title, String message, String tag);
}