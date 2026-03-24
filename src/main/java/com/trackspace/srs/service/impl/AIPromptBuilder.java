package com.trackspace.srs.service.impl;

import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.jira.entity.JiraSprint;
import com.trackspace.project.ProjectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class AIPromptBuilder {

    /**
     * Builds the prompt for auto-generating 4 basic SRS sections:
     * Cover, Introduction, Business Rules, Non-Screen Functions.
     * Other sections are left as placeholders for user to fill via AI Vision.
     */
    public String buildPrompt(ProjectInfo info,
            List<JiraIssue> issues,
            List<JiraSprint> sprints,
            String groupName,
            String createdByName,
            Integer versionNumber,
            String additionalInfo) {

        StringBuilder sb = new StringBuilder();

        // --- Role & Task ---
        sb.append("You are an expert Software Requirements Specification (SRS) writer following the IEEE 830 standard.\n");
        sb.append("Your task is to generate a PARTIAL SRS document based on the project data below.\n");
        sb.append("You will ONLY generate 4 sections. Other sections will be filled later by the user.\n");
        sb.append("The output MUST be in valid JSON format only, with no markdown wrappers.\n\n");

        // --- Target Structure ---
        sb.append("=== REQUIRED JSON STRUCTURE ===\n");
        sb.append("Generate ONLY these sections. For sections marked 'PLACEHOLDER', output the exact placeholder text.\n");
        sb.append("""
            {
                "projectName": "string",
                "locationDate": "string (format: '– Hanoi, <Month Year> –')",
                "introduction": {
                    "overview": "string (2-3 paragraphs about the project scope, goals, target users)",
                    "context": "string (background, motivation, problems the project solves)"
                },
                "businessMainFlows": {
                    "description": "",
                    "flows": []
                },
                "businessRules": [
                    { "id": "BR-01", "description": "string" }
                ],
                "useCases": {
                    "description": "[PLACEHOLDER] Please insert your Use Case diagram and click AI Describe to generate use case descriptions.",
                    "diagramInfo": "",
                    "list": []
                },
                "systemFunctions": {
                    "screenFlow": "[PLACEHOLDER] Please insert your Screen Flow diagram and click AI Describe to generate screen details.",
                    "screenDetails": [],
                    "roles": [],
                    "authorizations": [],
                    "nonScreenFunctions": [
                        { "feature": "string", "name": "string", "description": "string" }
                    ]
                },
                "highLevelDesign": {
                    "conceptualERD": "",
                    "logicalERD": "",
                    "dbSchema": "",
                    "tables": []
                },
            }
        """).append("\n\n");

        // --- Instructions for each section ---
        sb.append("=== SECTION INSTRUCTIONS ===\n");
        sb.append("1. projectName + locationDate: Use project topic and current date.\n");
        sb.append("2. introduction: Write detailed overview and context from PROJECT DATA below.\n");
        sb.append("3. businessRules: Generate from ADDITIONAL INFO and infer from Jira issues (validation rules, constraints, permissions).\n");
        sb.append("4. systemFunctions.nonScreenFunctions: Generate from ADDITIONAL INFO (background services, sync jobs, APIs).\n");
        sb.append("5. ALL other sections: Use EXACT placeholder text shown above. Do NOT generate content for them.\n");
        sb.append("6. Do NOT generate functionalRequirements. Section III will be generated later from Screen Flow.\n\n");

        // --- Project Data (PRIMARY: Jira) ---
        sb.append("=== JIRA PROJECT DATA (PRIMARY SOURCE) ===\n");

        // Sprint overview
        if (sprints != null && !sprints.isEmpty()) {
            sb.append("SPRINTS (").append(sprints.size()).append(" sprints):\n");
            for (JiraSprint sprint : sprints) {
                sb.append("- ").append(nullSafe(sprint.getSprintName()));
                if (sprint.getSprintGoal() != null && !sprint.getSprintGoal().isBlank()) {
                    sb.append(" | Goal: ").append(sprint.getSprintGoal());
                }
                sb.append(" | Status: ").append(sprint.getStatus()).append("\n");
            }
            sb.append("\n");
        }

        // Issues grouped by sprint
        if (!issues.isEmpty()) {
            Map<Integer, List<JiraIssue>> issuesBySprint = issues.stream()
                    .collect(Collectors.groupingBy(
                            issue -> issue.getSprintId() != null ? issue.getSprintId() : 0));

            sb.append("JIRA ISSUES (").append(issues.size()).append(" total):\n");
            for (Map.Entry<Integer, List<JiraIssue>> entry : issuesBySprint.entrySet()) {
                String sprintLabel = entry.getKey() == 0 ? "Backlog"
                        : sprints.stream()
                                .filter(s -> {
                                    try {
                                        return Integer.parseInt(s.getJiraSprintId()) == entry.getKey();
                                    } catch (NumberFormatException e) {
                                        return false;
                                    }
                                })
                                .findFirst()
                                .map(JiraSprint::getSprintName)
                                .orElse("Sprint " + entry.getKey());
                sb.append("[").append(sprintLabel).append("]\n");
                for (JiraIssue issue : entry.getValue()) {
                    sb.append("  - ").append(issue.getIssueKey()).append(": ").append(issue.getSummary());
                    if (issue.getDescription() != null && !issue.getDescription().isBlank()) {
                        String desc = issue.getDescription().length() > 200
                                ? issue.getDescription().substring(0, 200) + "..."
                                : issue.getDescription();
                        sb.append(" | ").append(desc);
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }

        // --- Project Context (SUPPLEMENTARY) ---
        sb.append("=== PROJECT CONTEXT (SUPPLEMENTARY) ===\n");
        if (info != null) {
            sb.append("Topic: ").append(nullSafe(info.getTopic())).append("\n");
            sb.append("Context: ").append(nullSafe(info.getContext())).append("\n");
            sb.append("Problems: ").append(nullSafe(info.getProblems())).append("\n");
            sb.append("Primary Actors: ").append(nullSafe(info.getPrimaryActors())).append("\n");
        } else {
            sb.append("(No project info provided. Derive context from Jira issues.)\n");
        }
        sb.append("Version: v").append(versionNumber).append("\n");
        sb.append("Creation Date: ").append(LocalDate.now()).append("\n");
        sb.append("Group Name: ").append(nullSafe(groupName)).append("\n");
        sb.append("Author: ").append(nullSafe(createdByName)).append("\n\n");

        // --- Additional Info ---
        sb.append("=== ADDITIONAL INFO (user-provided) ===\n");
        if (additionalInfo != null && !additionalInfo.isBlank()) {
            sb.append(additionalInfo).append("\n\n");
        } else {
            sb.append("(No additional info provided. Infer from Jira data.)\n\n");
        }

        // --- Output Constraints ---
        sb.append("=== OUTPUT CONSTRAINTS ===\n");
        sb.append("1. Output MUST be a valid JSON object.\n");
        sb.append("2. Language: EXCLUSIVELY ENGLISH.\n");
        sb.append("3. DO NOT wrap in code blocks. Return ONLY raw JSON.\n");
        sb.append("4. Keep sections concise. Ensure JSON is completely closed and valid.\n");
        sb.append("5. For PLACEHOLDER sections, use the EXACT placeholder text from the structure above.\n");

        return sb.toString();
    }

    /**
     * Builds a prompt for AI Vision: analyzing an image and generating SRS text.
     * @param imageType one of: usecase, screenflow, db_schema, mockup
     * @param projectContext brief project description for context
     * @param roles list of role names (for authorization matrix)
     */
    public String buildVisionPrompt(String imageType, String projectContext, List<String> roles) {
        StringBuilder sb = new StringBuilder();

        sb.append("You are an expert SRS (Software Requirements Specification) writer.\n");
        sb.append("Analyze the provided image and generate structured content for an SRS document.\n");
        sb.append("Project context: ").append(nullSafe(projectContext)).append("\n\n");

        switch (imageType) {
            case "usecase" -> {
                sb.append("The image is a USE CASE DIAGRAM.\n");
                sb.append("Generate a JSON array of use cases:\n");
                sb.append("""
                    [{ "id": "UC-01", "feature": "string", "name": "string", "description": "string" }]
                """);
                sb.append("Rules:\n");
                sb.append("- Each oval/ellipse in the diagram = one use case.\n");
                sb.append("- Group use cases by feature (related functionality).\n");
                sb.append("- Description should explain what the actor does and the expected outcome.\n");
            }

            case "screenflow" -> {
                sb.append("The image is a SCREEN FLOW DIAGRAM.\n");
                sb.append("Generate a JSON object with 4 parts:\n");
                sb.append("""
                    {
                        "screenFlow": "string (describe the navigation flow between screens)",
                        "screenDetails": [{ "feature": "string", "name": "string", "description": "string" }],
                        "authorizations": [{ "screenName": "string", "permissions": {} }],
                        "functionalRequirements": [
                            {
                                "name": "string (feature/module name, group related screens together)",
                                "functions": [
                                    { "name": "string (screen/function name)" }
                                ]
                            }
                        ]
                    }
                """);
                if (roles != null && !roles.isEmpty()) {
                    sb.append("Roles for authorization matrix: ").append(String.join(", ", roles)).append("\n");
                    sb.append("For each screen, set \"x\" if the role has access, or \"\" (empty string) if not.\n");
                }
                sb.append("Rules:\n");
                sb.append("- Each box/rectangle = one screen.\n");
                sb.append("- Arrows show navigation between screens.\n");
                sb.append("- Describe how users navigate from one screen to another.\n");
                sb.append("- For functionalRequirements: group screens into features/modules. Each screen = one function. Only provide names, no descriptions.\n");
            }

            case "db_schema" -> {
                sb.append("The image is a DATABASE SCHEMA DIAGRAM.\n");
                sb.append("Generate a JSON array of table descriptions:\n");
                sb.append("""
                    [{ "name": "string (table name)", "description": "string (purpose, what data it stores)", "pk": "string (primary key fields)", "fk": "string (foreign key fields and references)" }]
                """);
                sb.append("Rules:\n");
                sb.append("- Each box = one database table.\n");
                sb.append("- List all columns you can see.\n");
                sb.append("- Identify PK (usually marked with key icon or 'PK') and FK relationships.\n");
            }

            case "mockup" -> {
                sb.append("The image is a SCREEN MOCKUP / UI PROTOTYPE.\n");
                sb.append("Generate a JSON object describing this function:\n");
                sb.append("""
                    {
                        "trigger": "string (how to navigate to this screen, e.g., 'Admin clicks Manage Users on left menu')",
                        "description": "string (what actors/roles use this, what they can do, purpose)",
                        "details": "string (explain data fields, validation rules, business logic, normal and abnormal flows)"
                    }
                """);
                sb.append("Rules:\n");
                sb.append("- Describe ALL visible UI elements (buttons, tables, forms, filters).\n");
                sb.append("- Explain what happens when user interacts with each element.\n");
                sb.append("- Include validation rules you can infer from the UI.\n");
                sb.append("- IMPORTANT: GENERALIZE the data. DO NOT include specific hardcoded mockup numbers, names, or placeholder data from the image (e.g., do not write '42 accounts' or 'John Doe'). Instead, write 'displays the total number of accounts' or 'displays the user's name'.\n");
                sb.append("- Focus on the structure, functionality, and purpose of the elements, not the exact dummy data.\n");
            }

            default -> {
                sb.append("Describe what you see in this image in detail.\n");
            }
        }

        sb.append("\nOutput MUST be valid JSON only. No markdown wrappers. ENGLISH only.\n");
        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "(not provided)";
    }
}
