-- ============================================
-- TrackSpace Database Schema V3 (SIMPLIFIED SRS)
-- Version: 3.0
-- Date: 31/01/2026
-- Database: MySQL 8.0+
-- Changes: Simplified SRS - No review/approval workflow
-- ============================================

CREATE DATABASE IF NOT EXISTS trackspace;
USE trackspace;


-- ============================================
-- 1. USER MANAGEMENT
-- ============================================

-- Users table (Admin, Lecturer, TeamLeader, TeamMember)
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    email VARCHAR(255) UNIQUE NOT NULL COMMENT 'User email - must be unique',
    password_hash VARCHAR(255) NOT NULL COMMENT 'Bcrypt hashed password',
    full_name VARCHAR(255) NOT NULL,
    student_code VARCHAR(50) COMMENT 'MSSV (for students only) - must be unique',
    role ENUM('ADMIN', 'LECTURER', 'TEAMLEADER', 'TEAMMEMBER') NOT NULL,
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Soft delete flag',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_email (email),
    INDEX idx_role (role),
    INDEX idx_student_code (student_code),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Users table for Admin, Lecturer, TeamLeader, and TeamMember';

-- ============================================
-- 2. CLASS & GROUP MANAGEMENT
-- ============================================

-- Classes table
CREATE TABLE classes (
    class_id INT PRIMARY KEY AUTO_INCREMENT,
    class_name VARCHAR(255) NOT NULL,
    class_code VARCHAR(50) UNIQUE NOT NULL,
    semester VARCHAR(50) NOT NULL,
    lecturer_id INT NOT NULL,
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Soft delete flag',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (lecturer_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_lecturer (lecturer_id),
    INDEX idx_semester (semester),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Classes managed by lecturers';

-- Class-Student relationship (many-to-many)
CREATE TABLE class_students (
    class_id INT NOT NULL,
    student_id INT NOT NULL,
    enrolled_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (class_id, student_id),
    FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Many-to-many relationship between classes and students';

-- Groups table
CREATE TABLE `groups` (
    group_id INT PRIMARY KEY AUTO_INCREMENT,
    group_name VARCHAR(255) NOT NULL,
    description TEXT,
    class_id INT NOT NULL,
    team_leader_id INT COMMENT 'Student assigned as team leader',
    display_order INT DEFAULT 0 COMMENT 'For custom ordering in UI',
    is_active BOOLEAN DEFAULT TRUE COMMENT 'Soft delete flag',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE,
    FOREIGN KEY (team_leader_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_class (class_id),
    INDEX idx_leader (team_leader_id),
    INDEX idx_active (is_active)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Student groups within classes';

-- Group-Member relationship (many-to-many)
CREATE TABLE group_members (
    group_id INT NOT NULL,
    student_id INT NOT NULL,
    joined_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (group_id, student_id),
    FOREIGN KEY (group_id) REFERENCES `groups`(group_id) ON DELETE CASCADE,
    FOREIGN KEY (student_id) REFERENCES users(user_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Many-to-many relationship between groups and members';

-- ============================================
-- 3. PROJECT MANAGEMENT
-- ============================================

-- Projects table (one project per group)
CREATE TABLE projects (
    project_id INT PRIMARY KEY AUTO_INCREMENT,
    group_id INT UNIQUE NOT NULL,
    project_name VARCHAR(255) NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE COMMENT 'Soft delete flag',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (group_id) REFERENCES `groups`(group_id) ON DELETE CASCADE,
    INDEX idx_group (group_id),
    INDEX idx_deleted (is_deleted)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Projects - one per group';

-- Project Info table (for SRS generation)
CREATE TABLE project_info (
    info_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT UNIQUE NOT NULL,
    topic TEXT COMMENT 'Project topic/title',
    context TEXT COMMENT 'Background context',
    problems TEXT COMMENT 'Problems to solve',
    primary_actors TEXT COMMENT 'Main actors/stakeholders',
    functional_requirements TEXT COMMENT 'Key functional requirements',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Detailed project information for SRS generation';

-- ============================================
-- 4. JIRA INTEGRATION
-- ============================================

-- Jira Connections table
CREATE TABLE jira_connections (
    connection_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT UNIQUE NOT NULL,
    site_url VARCHAR(255) NOT NULL COMMENT 'Jira site URL',
    email VARCHAR(255) NOT NULL COMMENT 'Jira account email',
    api_token_encrypted TEXT NOT NULL COMMENT 'Encrypted API token',
    project_key VARCHAR(50) NOT NULL COMMENT 'Jira project key',
    connection_status ENUM('CONNECTED', 'DISCONNECTED', 'ERROR') DEFAULT 'DISCONNECTED',
    last_sync_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_status (connection_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Jira connection credentials per project';

-- Jira Sprints table
CREATE TABLE jira_sprints (
    sprint_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT NOT NULL,
    jira_sprint_id VARCHAR(100) UNIQUE NOT NULL COMMENT 'External Jira sprint ID',
    sprint_name VARCHAR(255) NOT NULL,
    sprint_goal TEXT,
    start_date DATE,
    end_date DATE,
    status ENUM('FUTURE', 'ACTIVE', 'CLOSED') DEFAULT 'FUTURE',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_jira_sprint (jira_sprint_id),
    INDEX idx_status (status),
    INDEX idx_project_status (project_id, status) COMMENT 'Composite index for common queries'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Sprints synced from Jira';

-- Jira Issues table
CREATE TABLE jira_issues (
    issue_id INT PRIMARY KEY AUTO_INCREMENT COMMENT 'Internal record ID',
    project_id INT NOT NULL,
    sprint_id INT,
    jira_issue_id VARCHAR(100) UNIQUE NOT NULL COMMENT 'External Jira issue ID',
    issue_key VARCHAR(50) NOT NULL COMMENT 'Jira issue key (e.g. PROJ-123)',
    issue_type ENUM('EPIC', 'STORY', 'TASK', 'BUG') NOT NULL,
    summary VARCHAR(500) NOT NULL,
    description TEXT,
    status VARCHAR(50) NOT NULL,
    priority VARCHAR(50),
    assignee_id INT,
    due_date DATE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (sprint_id) REFERENCES jira_sprints(sprint_id) ON DELETE SET NULL,
    FOREIGN KEY (assignee_id) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_project (project_id),
    INDEX idx_sprint (sprint_id),
    INDEX idx_jira_issue (jira_issue_id),
    INDEX idx_assignee (assignee_id),
    INDEX idx_status (status),
    INDEX idx_project_status (project_id, status) COMMENT 'Composite index for filtering'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Issues/tasks synced from Jira';

-- ============================================
-- 5. GITHUB INTEGRATION
-- ============================================

-- GitHub Connections table
CREATE TABLE github_connections (
    connection_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT UNIQUE NOT NULL,
    repository_url VARCHAR(500) NOT NULL,
    branch_name VARCHAR(100) NOT NULL,
    access_token_encrypted TEXT NOT NULL COMMENT 'Encrypted GitHub PAT',
    connection_status ENUM('CONNECTED', 'DISCONNECTED', 'ERROR') DEFAULT 'DISCONNECTED',
    last_sync_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_status (connection_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='GitHub repository connection per project';

-- GitHub Commits table
CREATE TABLE github_commits (
    commit_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT NOT NULL,
    commit_sha VARCHAR(100) UNIQUE NOT NULL,
    commit_message TEXT NOT NULL,
    author_name VARCHAR(255) NOT NULL,
    author_email VARCHAR(255),
    author_id INT COMMENT 'Linked user if email matched',
    commit_date TIMESTAMP NOT NULL,
    files_changed INT UNSIGNED DEFAULT 0,
    lines_added INT UNSIGNED DEFAULT 0,
    lines_deleted INT UNSIGNED DEFAULT 0,
    branch_name VARCHAR(100),
    linked_issue_id INT COMMENT 'Linked Jira issue if commit message contains task ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (author_id) REFERENCES users(user_id) ON DELETE SET NULL,
    FOREIGN KEY (linked_issue_id) REFERENCES jira_issues(issue_id) ON DELETE SET NULL,
    INDEX idx_project (project_id),
    INDEX idx_commit_sha (commit_sha),
    INDEX idx_author (author_id),
    INDEX idx_commit_date (commit_date),
    INDEX idx_branch (branch_name),
    INDEX idx_project_author (project_id, author_id) COMMENT 'Composite for user stats'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Commits synced from GitHub';

-- ============================================
-- 6. CONTRIBUTION TRACKING
-- ============================================

-- Contribution Metrics table
CREATE TABLE contribution_metrics (
    metric_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT NOT NULL,
    user_id INT NOT NULL,
    tasks_assigned INT UNSIGNED DEFAULT 0,
    tasks_completed INT UNSIGNED DEFAULT 0,
    tasks_in_progress INT UNSIGNED DEFAULT 0,
    task_completion_rate DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Percentage (0-100)',
    total_commits INT UNSIGNED DEFAULT 0,
    lines_added INT UNSIGNED DEFAULT 0,
    lines_deleted INT UNSIGNED DEFAULT 0,
    contribution_score DECIMAL(5,2) DEFAULT 0.00 COMMENT 'Overall score (0-100)',
    last_activity_date TIMESTAMP NULL,
    is_inactive BOOLEAN DEFAULT FALSE COMMENT 'Inactive >3 days',
    has_low_contribution BOOLEAN DEFAULT FALSE COMMENT 'Below 20% threshold',
    has_overdue_tasks BOOLEAN DEFAULT FALSE,
    calculated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    UNIQUE KEY unique_project_user (project_id, user_id),
    INDEX idx_project (project_id),
    INDEX idx_user (user_id),
    INDEX idx_score (contribution_score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Calculated contribution metrics per user per project';

-- ============================================
-- 7. SRS GENERATION (SIMPLIFIED - NO REVIEW)
-- ============================================

-- SRS Documents table (SIMPLIFIED)
CREATE TABLE srs_documents (
    srs_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT NOT NULL,
    version_number INT UNSIGNED NOT NULL DEFAULT 1,
    title VARCHAR(500) NOT NULL COMMENT 'SRS document title',
    content LONGTEXT NOT NULL COMMENT 'SRS document content (HTML/Markdown)',
    generated_by_ai BOOLEAN DEFAULT FALSE COMMENT 'Whether generated by AI',
    created_by INT NOT NULL COMMENT 'Student who created this SRS',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    FOREIGN KEY (created_by) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_version (project_id, version_number),
    INDEX idx_created_by (created_by)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='SRS documents - Lecturer can only VIEW and DOWNLOAD';

-- ============================================
-- 8. NOTIFICATION SYSTEM
-- ============================================

-- Notifications table
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL,
    notification_type ENUM(
        'ACCOUNT_CREATED',
        'TEAM_LEADER_ASSIGNED',
        'TASK_ASSIGNED',
        'TASK_STATUS_CHANGED',
        'ISSUE_DETECTED'
    ) NOT NULL,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    related_entity_type VARCHAR(50) COMMENT 'PROJECT, TASK, SRS, etc.',
    related_entity_id INT,
    is_read BOOLEAN DEFAULT FALSE,
    email_sent BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    INDEX idx_user (user_id),
    INDEX idx_is_read (is_read),
    INDEX idx_type (notification_type),
    INDEX idx_created (created_at),
    INDEX idx_user_unread (user_id, is_read) COMMENT 'Composite for unread queries'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='In-app and email notifications';

-- ============================================
-- 9. SYNC LOGS (for audit and troubleshooting)
-- ============================================

-- Sync Logs table
CREATE TABLE sync_logs (
    log_id INT PRIMARY KEY AUTO_INCREMENT,
    project_id INT NOT NULL,
    sync_type ENUM('JIRA', 'GITHUB') NOT NULL,
    sync_status ENUM('SUCCESS', 'FAILED', 'PARTIAL') NOT NULL,
    records_synced INT UNSIGNED DEFAULT 0,
    error_message TEXT,
    sync_started_at TIMESTAMP NOT NULL,
    sync_completed_at TIMESTAMP NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (project_id) REFERENCES projects(project_id) ON DELETE CASCADE,
    INDEX idx_project (project_id),
    INDEX idx_type (sync_type),
    INDEX idx_status (sync_status),
    INDEX idx_started (sync_started_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
COMMENT='Audit logs for Jira/GitHub synchronization';

