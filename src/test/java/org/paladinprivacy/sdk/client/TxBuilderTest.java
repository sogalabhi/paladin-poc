package org.paladinprivacy.sdk.client;

import java.io.IOException;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

class TxBuilderTest {

    private MockWebServer mockNode;
    private PaladinClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockNode = new MockWebServer();
        mockNode.start();
        client = new PaladinClient(mockNode.url("/").toString());
    }

    @AfterEach
    void tearDown() throws IOException {
        mockNode.shutdown();
    }

    @Test
    void testIdempotencyClashRecovery() {
        mockNode.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"jsonrpc\":\"2.0\",\"id\":1,\"error\":{\"code\":\"PD012220\",\"message\":\"Idempotency key already in use\"}}"));

        mockNode.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("{\"jsonrpc\":\"2.0\",\"id\":2,\"result\":\"recovered-tx-uuid-8899\"}"));

        String transactionId = client.newTransaction()
                .privateTx()
                .domain("noto")
                .from("alice@node1")
                .to("bob@node2")
                .inputs(Map.of("amount", 500))
                .idempotencyKey("payment-ref-123")
                .send()
                .join();

        assertEquals("recovered-tx-uuid-8899", transactionId, "SDK should transparently recover the transaction ID on PD012220");
    }
}