package com.trackspace.common;

/**
 * Application Constants
 * 
 * Contains all constant values used across the application
 */
public class Constants {

    // User Roles
    public static final String ROLE_ADMIN = "ADMIN";
    public static final String ROLE_LECTURER = "LECTURER";
    public static final String ROLE_STUDENT = "STUDENT";

    // JWT
    public static final String JWT_HEADER = "Authorization";
    public static final String JWT_PREFIX = "Bearer ";

    // Email Domain
    public static final String FPT_EMAIL_DOMAIN = "@fpt.edu.vn";

    // Date Formats
    public static final String DATE_FORMAT = "yyyy-MM-dd";
    public static final String DATETIME_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // API Paths
    public static final String API_PREFIX = "/api";
    public static final String AUTH_PATH = API_PREFIX + "/auth";
    public static final String USER_PATH = API_PREFIX + "/users";
    public static final String CLASS_PATH = API_PREFIX + "/classes";
    public static final String GROUP_PATH = API_PREFIX + "/groups";
    public static final String PROJECT_PATH = API_PREFIX + "/projects";
    public static final String JIRA_PATH = API_PREFIX + "/jira";
    public static final String GITHUB_PATH = API_PREFIX + "/github";
    public static final String SRS_PATH = API_PREFIX + "/srs";
    public static final String CONTRIBUTION_PATH = API_PREFIX + "/contributions";
    public static final String NOTIFICATION_PATH = API_PREFIX + "/notifications";

    // Sync Intervals (milliseconds)
    public static final long JIRA_SYNC_INTERVAL = 1800000; // 30 minutes
    public static final long GITHUB_SYNC_INTERVAL = 1800000; // 30 minutes

    // Contribution Thresholds
    public static final int INACTIVE_DAYS_THRESHOLD = 3;
    public static final double LOW_CONTRIBUTION_THRESHOLD = 0.2; // 20%

    // File Upload
    public static final long MAX_FILE_SIZE = 10485760; // 10MB
    public static final String[] ALLOWED_EXCEL_EXTENSIONS = { ".xlsx", ".xls" };

    // Notification Types
    public static final String NOTIF_ACCOUNT_CREATED = "ACCOUNT_CREATED";
    public static final String NOTIF_TEAM_LEADER_ASSIGNED = "TEAM_LEADER_ASSIGNED";
    public static final String NOTIF_TASK_ASSIGNED = "TASK_ASSIGNED";
    public static final String NOTIF_TASK_STATUS_CHANGED = "TASK_STATUS_CHANGED";
    public static final String NOTIF_SRS_SUBMITTED = "SRS_SUBMITTED";
    public static final String NOTIF_SRS_FEEDBACK = "SRS_FEEDBACK_RECEIVED";
    public static final String NOTIF_ISSUE_DETECTED = "ISSUE_DETECTED";

    // Connection Status
    public static final String STATUS_CONNECTED = "CONNECTED";
    public static final String STATUS_DISCONNECTED = "DISCONNECTED";
    public static final String STATUS_ERROR = "ERROR";

    private Constants() {
        // Private constructor to prevent instantiation
    }
}
