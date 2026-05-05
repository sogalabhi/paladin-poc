package org.paladinprivacy.sdk.client;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.paladinprivacy.sdk.exception.PaladinRpcException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PaladinClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final ObjectMapper mapper;

    public PaladinClient(String baseUrl) {
        this.httpClient = HttpClient.newHttpClient();
        this.baseUrl = baseUrl;
        this.mapper = new ObjectMapper();
    }

    public TxBuilder newTransaction() {
        return new TxBuilder(this);
    }

    protected CompletableFuture<JsonNode> sendRpc(String method, Object params) {
        try {
            Map<String, Object> rpcPayload = Map.of(
                    "jsonrpc", "2.0",
                    "id", System.currentTimeMillis(),
                    "method", method,
                    "params", params != null ? params : "[]"
            );

            String jsonBody = mapper.writeValueAsString(rpcPayload);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
                    .thenApply(response -> {
                        try {
                            JsonNode rootNode = mapper.readTree(response.body());
                            if (rootNode.has("error")) {
                                JsonNode errorNode = rootNode.get("error");
                                throw new PaladinRpcException(
                                        errorNode.get("code").asText(),
                                        errorNode.get("message").asText()
                                );
                            }
                            return rootNode.get("result");
                        } catch (PaladinRpcException e) {
                            throw e; 
                        } catch (Exception e) {
                            throw new RuntimeException("Failed to parse JSON-RPC response", e);
                        }
                    });
        } catch (JsonProcessingException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
