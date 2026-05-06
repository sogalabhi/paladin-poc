package org.paladinprivacy.sdk.domain;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.paladinprivacy.sdk.client.PaladinClient;

/**
 * A Domain Helper that wraps the raw TxBuilder into business-friendly methods.
 * This demonstrates how the SDK abstracts Paladin internals for application developers.
 */
public class ZetoHelper {
    private final PaladinClient client;

    public ZetoHelper(PaladinClient client) {
        this.client = client;
    }
    

    /**
     * Executes a Zeto token transfer.
     */
    public CompletableFuture<String> transfer(String from, String to, int amount, String paymentReference) {
        // Look how clean this is because of the TxBuilder we just wrote!
        return client.newTransaction()
                .privateTx()
                .domain("zeto")
                .function("transfer")
                .from(from)
                .to(to)
                .inputs(Map.of("to", to, "amount", amount))
                .idempotencyKey(paymentReference)
                .send();
    }
}