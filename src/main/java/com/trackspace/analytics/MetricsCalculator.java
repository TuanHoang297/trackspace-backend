package com.trackspace.analytics;

import java.util.Set;

/**
 * Pure utility class for all analytics score calculations.
 *
 * All methods are static — no Spring dependency.
 */
public final class MetricsCalculator {

    private MetricsCalculator() {}

    // ── File-weight constants ──────────────────────────────────────────────────

    /** Core logic files: maximum credit */
    public static final double WEIGHT_HIGH = 1.0;
    /** Markup / styling files: medium credit */
    public static final double WEIGHT_MEDIUM = 0.5;
    /** Config / documentation files: minimal credit */
    public static final double WEIGHT_LOW = 0.1;
    /** Generated / binary / library files: ignored */
    public static final double WEIGHT_NONE = 0.0;

    /**
     * Default weight applied when per-file breakdown is not stored
     * (a conservative middle-ground between MEDIUM and HIGH).
     */
    public static final double DEFAULT_FILE_WEIGHT = 0.7;

    // ── Bug-fix multipliers ────────────────────────────────────────────────────

    /** Strong fix keywords (hotfix, fix!) → x3 */
    public static final double MULTIPLIER_HOTFIX = 3.0;
    /** Standard fix keywords (fix, resolve, bug, patch) → x2 */
    public static final double MULTIPLIER_FIX = 2.0;
    /** Regular commit → x1 */
    public static final double MULTIPLIER_NORMAL = 1.0;

    // ── Thresholds ────────────────────────────────────────────────────────────

    /** Days without a commit before a member is flagged as inactive */
    public static final long INACTIVE_DAYS_THRESHOLD = 3;
    /** Score ratio below which a member is flagged as low-contributor */
    public static final double LOW_CONTRIBUTION_RATIO = 0.2;
    /** Churn rate above which a member is flagged as high-churn */
    public static final double HIGH_CHURN_THRESHOLD = 1.5;

    // Extensions that should receive no credit
    private static final Set<String> IGNORED_EXTENSIONS = Set.of(
            "lock", "png", "jpg", "jpeg", "gif", "svg", "ico", "woff", "woff2",
            "ttf", "eot", "mp4", "mp3", "zip", "jar", "class", "pyc"
    );

    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the file-type weight for a given filename.
     *
     * @param filename filename or relative path
     * @return weight constant (0.0 – 1.0)
     */
    public static double getFileWeight(String filename) {
        if (filename == null) return DEFAULT_FILE_WEIGHT;
        String lower = filename.toLowerCase();

        // Always ignore: generated/library folders
        if (lower.contains("node_modules/") || lower.contains("vendor/")
                || lower.contains("target/") || lower.contains(".min.")) {
            return WEIGHT_NONE;
        }

        String ext = extractExtension(lower);

        if (IGNORED_EXTENSIONS.contains(ext)) return WEIGHT_NONE;

        return switch (ext) {
            // HIGH – logic code
            case "java", "kt", "scala", "groovy",
                 "js", "ts", "jsx", "tsx", "mjs",
                 "py", "rb", "go", "rs", "c", "cpp", "h", "cs",
                 "sql", "graphql" -> WEIGHT_HIGH;

            // MEDIUM – UI / markup
            case "html", "htm", "vue", "svelte",
                 "css", "scss", "sass", "less",
                 "xml", "xsd" -> WEIGHT_MEDIUM;

            // LOW – config / docs
            case "json", "yaml", "yml", "toml",
                 "md", "txt", "properties",
                 "env", "ini", "cfg" -> WEIGHT_LOW;

            default -> DEFAULT_FILE_WEIGHT;
        };
    }

    /**
     * Log<sub>10</sub>-based impact score for a number of added lines.
     *
     * <pre>
     *   1 line     → 0
     *  10 lines    → 1.0
     * 100 lines    → 2.0
     * 1 000 lines  → 3.0
     * 10 000 lines → 4.0  (diminishing returns cap on copy-paste)
     * </pre>
     *
     * @param linesAdded raw lines-added count from the commit
     * @return log10 score ≥ 0
     */
    public static double calcLog10Score(int linesAdded) {
        if (linesAdded <= 0) return 0.0;
        return Math.log10(linesAdded);
    }

    /**
     * Determines the bug-fix multiplier from a commit message.
     *
     * <ul>
     *   <li>hotfix / fix! → x3</li>
     *   <li>fix / resolve / bug / patch → x2</li>
     *   <li>anything else → x1</li>
     * </ul>
     */
    public static double getBugMultiplier(String commitMessage) {
        if (commitMessage == null || commitMessage.isBlank()) return MULTIPLIER_NORMAL;
        String lower = commitMessage.toLowerCase();

        if (lower.contains("hotfix") || lower.contains("fix!")) {
            return MULTIPLIER_HOTFIX;
        }
        if (lower.contains("fix") || lower.contains("resolve")
                || lower.contains("bug") || lower.contains("patch")) {
            return MULTIPLIER_FIX;
        }
        return MULTIPLIER_NORMAL;
    }

    /**
     * Returns {@code true} if the commit message is associated with a bug-fix.
     */
    public static boolean isBugFix(String commitMessage) {
        return getBugMultiplier(commitMessage) > MULTIPLIER_NORMAL;
    }

    /**
     * Consistency multiplier based on the number of days with at least one commit.
     *
     * <pre>
     *  0 active days → 0.5 (penalty: did nothing / last-minute dump)
     * 14+ active days → 2.0 (reward: steady daily work)
     * Linear between those extremes.
     * </pre>
     *
     * @param activeDays distinct calendar days with at least one commit
     * @return multiplier in [0.5, 2.0]
     */
    public static double calcConsistencyFactor(int activeDays) {
        if (activeDays <= 0) return 0.5;
        if (activeDays >= 14) return 2.0;
        return 0.5 + (activeDays / 14.0) * 1.5;
    }

    /**
     * Code churn rate: deletedLines / (addedLines + 1).
     *
     * A value above {@link #HIGH_CHURN_THRESHOLD} suggests the user's code
     * had low quality and was heavily reworked by others.
     */
    public static double calcCodeChurnRate(int linesAdded, int linesDeleted) {
        return (double) linesDeleted / (linesAdded + 1.0);
    }

    /**
     * Jira quality factor (penalty for reworks).
     *
     * <pre>
     *  0 reworks → 1.0   (no penalty)
     *  1 rework  → 0.85
     *  2 reworks → 0.70
     *  3+ reworks → 0.50 (floor)
     * </pre>
     */
    public static double calcJiraQualityFactor(int reworkCount) {
        if (reworkCount <= 0) return 1.0;
        if (reworkCount >= 3) return 0.5;
        return 1.0 - (reworkCount * 0.15);
    }

    /**
     * Smart Coder Bonus: reward task efficiency — many tasks closed with compact code.
     *
     * <pre>
     *   ratio = tasksCompleted / max(1, log10(linesAdded + 1))
     *   ratio ≤ 0  → 1.0  (no activity)
     *   ratio 0–5  → linear ramp from 1.0 to 1.5
     *   ratio ≥ 5  → 1.5  (max bonus — very efficient coder)
     * </pre>
     *
     * This multiplier is applied on top of the Jira Execution score.
     * A "Smart Coder" who closes 10 tasks with 200 lines of core code scores
     * higher than someone who closes 3 tasks with 10 000 lines of copy-paste.
     */
    public static double calcSmartCoderBonus(int tasksCompleted, int linesAdded) {
        if (tasksCompleted == 0) return 1.0;
        double logLines = Math.max(1.0, Math.log10(linesAdded + 1));
        double ratio    = tasksCompleted / logLines;
        double capped   = Math.min(ratio, 5.0);
        return 1.0 + (capped / 5.0) * 0.5;
    }

    /**
     * Normalises a raw score to [0, 100] given the maximum observed score
     * in the group.  Returns 0 when maxScore is 0 (no activity in project).
     */
    public static double normalizeScore(double score, double maxScore) {
        if (maxScore <= 0) return 0.0;
        return Math.min((score / maxScore) * 100.0, 100.0);
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static String extractExtension(String filename) {
        int dot = filename.lastIndexOf('.');
        int slash = Math.max(filename.lastIndexOf('/'), filename.lastIndexOf('\\'));
        if (dot > slash && dot < filename.length() - 1) {
            return filename.substring(dot + 1);
        }
        return "";
    }
}
