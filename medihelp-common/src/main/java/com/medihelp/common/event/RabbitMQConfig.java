package com.medihelp.common.event;

public final class RabbitMQConfig {

    private RabbitMQConfig() {}

    // Exchange
    public static final String EXCHANGE = "medihelp.events";

    // Routing Keys
    public static final String RK_USER_REGISTERED = "user.registered";
    public static final String RK_MEDICATION_REMINDER = "medication.reminder.due";
    public static final String RK_VITALS_ANOMALY = "vitals.anomaly.detected";
    public static final String RK_APPOINTMENT_REMINDER = "appointment.reminder.due";
    public static final String RK_HEALTH_SCORE_UPDATED = "health.score.updated";
    public static final String RK_MEDICATION_ADHERENCE = "medication.adherence.updated";

    // Queues
    public static final String Q_PROFILE_USER_REGISTERED = "user-profile.user-registered";
    public static final String Q_NOTIFICATION_WELCOME = "notification.welcome-email";
    public static final String Q_NOTIFICATION_MEDICATION = "notification.medication-reminder";
    public static final String Q_NOTIFICATION_ANOMALY = "notification.anomaly-alert";
    public static final String Q_NOTIFICATION_APPOINTMENT = "notification.appointment-reminder";
    public static final String Q_NOTIFICATION_BADGE = "notification.badge-earned";
    public static final String Q_HEALTH_RECALCULATE = "health-tracking.recalculate-score";

    // Emergency SOS
    public static final String RK_EMERGENCY_SOS = "emergency.sos.triggered";
    public static final String Q_NOTIFICATION_SOS = "notification.emergency-sos";
}
