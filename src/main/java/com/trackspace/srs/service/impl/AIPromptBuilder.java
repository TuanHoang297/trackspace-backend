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

    private static final String PDF_TEMPLATE_PATH = "srs/srs_sample.pdf";
    private static final String MD_TEMPLATE_PATH = "srs/srs_sample.md";

    public String buildPrompt(ProjectInfo info,
            List<JiraIssue> issues,
            String groupName,
            String createdByName,
            Integer versionNumber) {

        StringBuilder sb = new StringBuilder();

        // --- Role & Task ---
        sb.append("You are an expert Software Requirements Specification (SRS) writer following the IEEE 830 standard.\n");
        sb.append(
                "Your task is to generate a comprehensive SRS document based on the following project data.\n");
        sb.append("The document must be written in ENGLISH and formatted in clean Markdown.\n\n");

        // --- Target Structure Instructions ---
        sb.append("=== REQUIRED DOCUMENT STRUCTURE ===\n");
        sb.append("The SRS must follow this exact structure:\n");
        sb.append(loadMarkdownTemplate()).append("\n\n");

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
        sb.append("1. Output MUST be valid Markdown text.\n");
        sb.append("2. Use Markdown tables to format structured data.\n");
        sb.append(
                "3. Ensure all tables mentioned in the structure are included even if they contain sample data based on your reasoning.\n");
        sb.append("4. Language: EXCLUSIVELY ENGLISH.\n");
        sb.append("5. DO NOT wrap the text in a code block (no ```markdown). Return only the raw markdown.\n");
        sb.append(
                "6. For any diagrams (Context, Swimlane, Screen Flow, ERD, Use Case), include a placeholder formatted as: [Insert Image: <Description of Diagram Content>].\n");

        return sb.toString();
    }

    public String loadPdfTemplateAsBase64() {
        try {
            ClassPathResource resource = new ClassPathResource(PDF_TEMPLATE_PATH);
            byte[] bytes = resource.getInputStream().readAllBytes();
            return Base64.getEncoder().encodeToString(bytes);
        } catch (IOException e) {
            log.warn("SRS sample PDF not found at '{}'. Generating without template reference.", PDF_TEMPLATE_PATH);
            return null;
        }
    }

    public String loadMarkdownTemplate() {
        try {
            ClassPathResource resource = new ClassPathResource(MD_TEMPLATE_PATH);
            return new String(resource.getInputStream().readAllBytes());
        } catch (IOException e) {
            log.warn("SRS template not found. Using default.");
            return "";
        }
    }

    private String nullSafe(String value) {
        return value != null && !value.isBlank() ? value : "(not provided)";
    }
}
