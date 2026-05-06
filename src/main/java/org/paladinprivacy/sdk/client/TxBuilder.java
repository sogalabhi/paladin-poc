package org.paladinprivacy.sdk.client;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.paladinprivacy.sdk.exception.PaladinRpcException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;

public class TxBuilder {
    private static final Logger log = LoggerFactory.getLogger(TxBuilder.class);

    private final PaladinClient client;
    private String type;
    private String domain;
    private String function;
    private String from;
    private String to;
    private Object inputs;
    private String idempotencyKey;

    protected TxBuilder(PaladinClient client) {
        this.client = client;
    }

    public TxBuilder privateTx() { this.type = "private"; return this; }
    public TxBuilder domain(String domain) { this.domain = domain; return this; }
    public TxBuilder function(String function) { this.function = function; return this; }
    public TxBuilder from(String from) { this.from = from; return this; }
    public TxBuilder to(String to) { this.to = to; return this; }
    public TxBuilder inputs(Object inputs) { this.inputs = inputs; return this; }
    public TxBuilder idempotencyKey(String key) { this.idempotencyKey = key; return this; }

    public CompletableFuture<String> send() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("type", type);
        payload.put("domain", domain);
        if (function != null) payload.put("function", function);
        payload.put("from", from);
        payload.put("to", to);
        payload.put("data", inputs);
        if (idempotencyKey != null) payload.put("idempotencyKey", idempotencyKey);

        return client.sendRpc("ptx_sendTransaction", List.of(payload))
                .thenApply(JsonNode::asText)
                .exceptionallyCompose(throwable -> {
                    Throwable cause = throwable.getCause();
                    
                    if (cause instanceof PaladinRpcException rpcEx && "PD012220".equals(rpcEx.getCode())) {
                        if (idempotencyKey != null) {
                            log.info("Idempotency clash (PD012220) detected for key '{}'. Automatically recovering transaction ID...", idempotencyKey);
                            return client.sendRpc("ptx_getTransactionByIdempotencyKey", List.of(idempotencyKey))
                                    .thenApply(JsonNode::asText);
                        }
                    }
                    return CompletableFuture.failedFuture(cause != null ? cause : throwable);
                });
    }
}
