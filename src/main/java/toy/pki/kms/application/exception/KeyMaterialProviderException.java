package toy.pki.kms.application.exception;

public class KeyMaterialProviderException extends RuntimeException {
    public KeyMaterialProviderException(String message, Throwable cause) {
        super(message, cause);
    }
}
