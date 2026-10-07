package toy.pki.ca.domain.policy;

import lombok.Data;
import lombok.RequiredArgsConstructor;

@Data
@RequiredArgsConstructor
public final class DnAttributePolicy {
    private final boolean required; // 필수 여부
    private final String fixedValue; // 고정 값 (null이면 고정 값 없음)
//    private final String defaultValue;

    public DnAttributePolicy(boolean required) {
        this.required = required;
        this.fixedValue = null;
    }

    public DnAttributePolicy(String fixedValue) {
        this.required = false;
        this.fixedValue = fixedValue;
    }
}
