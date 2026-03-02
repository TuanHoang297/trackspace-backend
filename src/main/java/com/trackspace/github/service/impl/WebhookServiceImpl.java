package com.trackspace.github.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.trackspace.github.entity.Connection;
import com.trackspace.github.repository.ConnectionRepository;
import com.trackspace.github.service.CommitService;
import com.trackspace.github.service.WebhookService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.HexFormat;
import java.util.List;

/**
 * Implementation of WebhookService
 * Handles GitHub push event verification and async sync trigger
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookServiceImpl implements WebhookService {

    private final ConnectionRepository connectionRepository;
    private final CommitService commitService;
    private final ObjectMapper objectMapper;

    @Value("${github.webhook.secret:}")
    private String webhookSecret;

    @Override
    public boolean verifySignature(byte[] payload, String signature, String secret) {
        if (signature == null || !signature.startsWith("sha256=")) {
            log.warn("[Webhook] Missing or malformed signature header");
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] computed = mac.doFinal(payload);
            String expected = "sha256=" + HexFormat.of().formatHex(computed);
            // Constant-time comparison to prevent timing attacks
            return constantTimeEquals(signature, expected);
        } catch (Exception e) {
            log.error("[Webhook] Signature verification error", e);
            return false;
        }
    }

    @Override
    @Async
    public void handlePushEvent(String payload) {
        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode repoNode = root.path("repository");
            if (repoNode.isMissingNode()) {
                log.warn("[Webhook] Payload missing 'repository' field");
                return;
            }

            // GitHub sends html_url like: https://github.com/owner/repo
            String htmlUrl = repoNode.path("html_url").asText("");
            String cloneUrl = repoNode.path("clone_url").asText(""); // ends with .git

            // Normalize to match what we store (with or without .git)
            String normalizedUrl = cloneUrl.isEmpty() ? htmlUrl : cloneUrl;

            log.info("[Webhook] Push event received for repo: {}", normalizedUrl);

            // Find all connections matching this repo URL
            List<Connection> allConnections = connectionRepository.findAll();
            List<Connection> matching = allConnections.stream()
                    .filter(c -> {
                        String stored = c.getRepositoryUrl();
                        // Compare both with and without .git suffix
                        String storedNorm = stored.replace(".git", "").toLowerCase();
                        String incomingNorm = normalizedUrl.replace(".git", "").toLowerCase();
                        return storedNorm.equals(incomingNorm) ||
                                stored.equalsIgnoreCase(normalizedUrl) ||
                                stored.replace(".git", "").equalsIgnoreCase(htmlUrl);
                    })
                    .toList();

            if (matching.isEmpty()) {
                log.warn("[Webhook] No matching connection found for repo: {}. Skipping sync.", normalizedUrl);
                return;
            }

            for (Connection conn : matching) {
                log.info("[Webhook] Triggering sync for connectionId={} (project={})", conn.getId(),
                        conn.getProjectId());
                try {
                    var result = commitService.syncSingleConnection(conn.getId());
                    log.info("[Webhook] Sync result for connectionId={}: {}", conn.getId(), result.get("message"));
                } catch (Exception e) {
                    log.error("[Webhook] Error syncing connectionId={}: {}", conn.getId(), e.getMessage());
                }
            }

        } catch (Exception e) {
            log.error("[Webhook] Failed to process push event", e);
        }
    }

    private boolean constantTimeEquals(String a, String b) {
        if (a.length() != b.length())
            return false;
        int result = 0;
        for (int i = 0; i < a.length(); i++) {
            result |= a.charAt(i) ^ b.charAt(i);
        }
        return result == 0;
    }
}
