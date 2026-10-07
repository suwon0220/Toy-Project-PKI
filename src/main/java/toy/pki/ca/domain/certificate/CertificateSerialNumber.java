package toy.pki.ca.domain.certificate;

import java.io.Serializable;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Locale;

public record CertificateSerialNumber(BigInteger value) implements Serializable {
    private static final SecureRandom RANDOM = new SecureRandom();

    public CertificateSerialNumber {
        if (value == null || value.signum() <= 0 || value.bitLength() > 159) {
            throw new IllegalArgumentException("Certificate serial must be a positive integer of at most 159 bits");
        }
    }

    public static CertificateSerialNumber generate() {
        return new CertificateSerialNumber(new BigInteger(159, RANDOM).setBit(158));
    }

    public String hex() { return value.toString(16).toUpperCase(Locale.ROOT); }
}
