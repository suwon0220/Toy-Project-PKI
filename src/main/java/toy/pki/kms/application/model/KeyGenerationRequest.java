package toy.pki.kms.application.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import toy.pki.kms.domain.ManagedKey;

public record KeyGenerationRequest(
    @NotBlank(message = "키 Provider를 선택하세요.") String providerId,
    @NotNull(message = "키 생성 프리셋을 선택하세요.") String keyAlgorithmPreset,

    @Size(max = ManagedKey.MAX_ALIAS_LENGTH, message = "별칭은 최대 64자까지 입력할 수 있습니다.")
    String alias) {
}
