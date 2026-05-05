package org.paladinprivacy.sdk.exception;

public class PaladinRpcException extends RuntimeException {
    private final String code;

    public PaladinRpcException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
