package com.trackspace.srs.service.impl;

import com.trackspace.jira.entity.JiraIssue;
import com.trackspace.project.ProjectInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.LocalDate;
import java.util.Base64;
import java.util.List;

@Component
@Slf4j
public class AIPromptBuilder {

    public String buildPrompt(ProjectInfo info,
            List<JiraIssue> issues,
            String groupName,
            String createdByName,
            Integer versionNumber) {

        StringBuilder sb = new StringBuilder();

        // --- Role & Task ---
        sb.append("You are an expert Software Requirements Specification (SRS) writer following the IEEE 830 standard.\n");
        sb.append(
                "Your task is to generate a comprehensive SRS document data based on the following project data.\n");
        sb.append("The output MUST be in valid JSON format only, with no markdown wrappers.\n\n");

        // --- Target Structure Instructions ---
        sb.append("=== REQUIRED JSON STRUCTURE ===\n");
        sb.append("The JSON MUST strictly follow this typescript interface structure:\n");
        sb.append("""
            {
                "projectName": "string",
                "locationDate": "string",
                "introduction": {
                    "overview": "string",
                    "context": "string"
                },
                "businessMainFlows": {
                    "description": "string",
                    "flows": [ { "title": "string", "diagramPlaceholder": "string" } ]
                },
                "businessRules": [
                    { "id": "string", "description": "string" }
                ],
                "useCases": {
                    "description": "string",
                    "diagramInfo": "string",
                    "list": [ { "id": "string", "feature": "string", "name": "string", "description": "string" } ]
                },
                "systemFunctions": {
                    "screenFlow": "string",
                    "screenDetails": [ { "feature": "string", "name": "string", "description": "string" } ],
                    "roles": [ { "id": "string", "name": "string" } ],
                    "authorizations": [ { "screenName": "string", "permissions": { "RoleID": true/false } } ]
                },
                "highLevelDesign": {
                    "conceptualERD": "string",
                    "logicalERD": "string",
                    "dbSchema": "string",
                    "tables": [ { "name": "string", "description": "string", "pk": "string", "fk": "string" } ]
                },
                "functionalRequirements": [
                    { "name": "string", "functions": [ { "name": "string", "trigger": "string", "description": "string", "layoutInfo": "string", "details": "string" } ] }
                ]
            }
        """).append("\n\n");

        // --- Project Data ---
        sb.append("=== PROJECT DATA ===\n");
        sb.append("Project Topic: ").append(nullSafe(info.getTopic())).append("\n");
        sb.append("Version: v").append(versionNumber).append("\n");
        sb.append("Creation Date: ").append(LocalDate.now()).append("\n");
        sb.append("Group Name: ").append(nullSafe(groupName)).append("\n");
        sb.append("Author: ").append(nullSafe(createdByName)).append("\n\n");

        sb.append("PROJECT_CONTEXT (for Introduction):\n");
        sb.append(nullSafe(info.getContext())).append("\n\n");

        sb.append("PROJECT_PROBLEMS (for Overview):\n");
        sb.append(nullSafe(info.getProblems())).append("\n\n");

        sb.append("PRIMARY_ACTORS (for Authorization Table - format: Name | Description):\n");
        sb.append(nullSafe(info.getPrimaryActors())).append("\n\n");

        sb.append("FUNCTIONAL_REQUIREMENTS_OVERVIEW:\n");
        sb.append(nullSafe(info.getFunctionalRequirements())).append("\n\n");

        // --- Jira Issues ---
        sb.append("=== JIRA ISSUES (").append(issues.size()).append(" items) ===\n");
        sb.append(
                "Use these issues to populate Section III (Functional Requirements) and the System Function tables:\n");
        for (JiraIssue issue : issues) {
            sb.append("[").append(issue.getIssueType().name()).append("] ")
                    .append(issue.getIssueKey()).append(": ")
                    .append(issue.getSummary());

            if (issue.getDescription() != null && !issue.getDescription().isBlank()) {
                String desc = issue.getDescription().length() > 300
                        ? issue.getDescription().substring(0, 300) + "..."
                        : issue.getDescription();
                sb.append(" | Description: ").append(desc);
            }
            sb.append(" | Status: ").append(issue.getStatus()).append("\n");
        }

        // --- Output Formatting ---
        sb.append("\n=== OUTPUT CONSTRAINTS ===\n");
        sb.append("1. Output MUST be a valid JSON object.\n");
        sb.append("2. Language: EXCLUSIVELY ENGLISH.\n");
        sb.append("3. DO NOT wrap the text in a code block (e.g. ```json). Return ONLY the raw JSON.\n");
        sb.append("4. CRITICAL: Keep your contents concise. You are limited to 8192 tokens. You MUST ensure the JSON is completely closed and valid at the end. If the document is very long, summarize the 'functionalRequirements' and 'systemFunctions' sections to guarantee a perfectly valid and closed JSON structure. DO NOT TRUNCATE THE JSON.\n");

        return sb.toString();
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "(not provided)";
    }
}
