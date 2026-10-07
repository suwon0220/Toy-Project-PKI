package toy.pki.kms.domain.exception;

import toy.pki.kms.domain.KeyId;

public class ManagedKeyNotFoundException extends RuntimeException {
    public ManagedKeyNotFoundException(String message) {
        super(message);
    }
    public ManagedKeyNotFoundException(KeyId keyId) {
        super("ManagedKey not found: " + keyId.value());
    }
}
