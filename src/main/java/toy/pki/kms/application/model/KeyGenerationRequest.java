package toy.pki.kms.application.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import toy.pki.kms.domain.ManagedKey;
import toy.pki.kms.web.KeyAlgorithmPreset;

public record KeyGenerationRequest(
    @NotNull(message = "키 생성 프리셋을 선택하세요.") KeyAlgorithmPreset keyAlgorithmPreset,

    @Size(max = ManagedKey.MAX_ALIAS_LENGTH, message = "별칭은 최대 64자까지 입력할 수 있습니다.")
    String alias) {
}
