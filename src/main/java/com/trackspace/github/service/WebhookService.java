package com.trackspace.github.service;

/**
 * Service interface for handling GitHub Webhook events
 */
public interface WebhookService {

    /**
     * Verify the HMAC-SHA256 signature from GitHub
     *
     * @param payload   Raw request body bytes
     * @param signature Value of X-Hub-Signature-256 header
     * @param secret    Webhook secret configured in application.properties
     * @return true if signature is valid
     */
    boolean verifySignature(byte[] payload, String signature, String secret);

    /**
     * Handle a push event from GitHub webhook.
     * Finds the matching Connection by repository URL and triggers async sync.
     *
     * @param payload Raw JSON payload string from GitHub
     */
    void handlePushEvent(String payload);
}
