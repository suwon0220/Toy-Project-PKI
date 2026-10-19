package toy.pki.kms.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public record KeyId(String value) {
}
