package toy.pki.kms.domain;

import jakarta.validation.constraints.NotEmpty;

public record KeyMaterialRef(@NotEmpty String value) {

//    public KeyMaterialRef {
//        if(value == null || value.isBlank()) {
//            throw new IllegalArgumentException("KeyMaterialRef must not be blank");
//        }
//    }
}
