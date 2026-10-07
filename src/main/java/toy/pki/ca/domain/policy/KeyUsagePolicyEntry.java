package toy.pki.ca.domain.policy;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import toy.pki.ca.domain.profile.KeyUsage;

@Data
@RequiredArgsConstructor
public final class KeyUsagePolicyEntry {
    private final boolean required; // 필수 여부
    private final boolean critical;
    KeyUsage keyUsage;
}
