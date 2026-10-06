package toy.pki.kms.domain.key;

import jakarta.validation.constraints.NotEmpty;

public record KeyProviderId(@NotEmpty String value) {
//    public KeyProviderId {
//        if (value == null || value.isBlank()) {
//            throw new IllegalArgumentException("KeyProviderId cannot be blank");
//        }
//    }
}
