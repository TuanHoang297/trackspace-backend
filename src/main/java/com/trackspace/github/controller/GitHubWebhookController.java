package com.trackspace.github.controller;

import com.trackspace.common.ApiResponse;
import com.trackspace.github.service.WebhookService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * GitHub Webhook Controller
 *
 * Receives real-time push events from GitHub.
 * To register: GitHub repo → Settings → Webhooks → Add
 * Payload URL:
 * https://trackspace-db-server-fngwa9fvfqc4d6bk.japaneast-01.azurewebsites.net/api/v1/github/webhook
 * Content type: application/json
 * Secret: value of github.webhook.secret in application.properties
 * Events: Just the push event
 */
@RestController
@RequestMapping("/api/v1/github")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "GitHub Integration")
public class GitHubWebhookController {

    private final WebhookService webhookService;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    /**
     * POST /api/v1/github/webhook
     * Entry point for GitHub Webhook push events.
     * Must respond with 200 within 10 seconds — actual sync is done async.
     */
    @Operation(summary = "GitHub Webhook receiver", description = "Receives push events from GitHub and triggers real-time commit sync asynchronously.")
    @PostMapping("/webhook")
    public ResponseEntity<ApiResponse<String>> handleWebhook(
            @RequestHeader(value = "X-GitHub-Event", defaultValue = "") String event,
            @RequestHeader(value = "X-Hub-Signature-256", defaultValue = "") String signature,
            @RequestBody byte[] payload) {

        log.info("[Webhook] Received event: '{}'", event);

        // If a secret is configured, verify the signature
        if (!webhookSecret.isEmpty()) {
            boolean valid = webhookService.verifySignature(payload, signature, webhookSecret);
            if (!valid) {
                log.warn("[Webhook] Invalid signature — rejecting request");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(ApiResponse.error("Invalid webhook signature"));
            }
        }

        // Only handle push events (ignore ping, pull_request, etc.)
        if (!"push".equalsIgnoreCase(event)) {
            log.debug("[Webhook] Ignoring non-push event: {}", event);
            return ResponseEntity.ok(ApiResponse.success("Event ignored: " + event));
        }

        // Trigger async sync — respond immediately so GitHub doesn't timeout
        String payloadStr = new String(payload, java.nio.charset.StandardCharsets.UTF_8);
        webhookService.handlePushEvent(payloadStr);

        log.info("[Webhook] Push event accepted, async sync triggered");
        return ResponseEntity.ok(ApiResponse.success("Webhook received, sync triggered"));
    }
}
