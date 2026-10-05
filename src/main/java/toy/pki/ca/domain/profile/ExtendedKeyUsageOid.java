package toy.pki.ca.domain.profile;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;

/**
 * ExtendedKeyUsageOid
 */
public record ExtendedKeyUsageOid(
    @NotBlank String value) {

    private static final Pattern OID_PATTERN = Pattern.compile(
        "[0-2]\\.(0|[1-9][0-9]*)(\\.(0|[1-9][0-9]*))*");

    // RFC 5280 section 4.2.1.12
    private static final Map<String, Set<KeyUsage>> COMPATIBLE_KEY_USAGES = Map.of(
        "1.3.6.1.5.5.7.3.1", Set.of(
            KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_ENCIPHERMENT, KeyUsage.KEY_AGREEMENT),
        "1.3.6.1.5.5.7.3.2", Set.of(
            KeyUsage.DIGITAL_SIGNATURE, KeyUsage.KEY_AGREEMENT),
        "1.3.6.1.5.5.7.3.3", Set.of(KeyUsage.DIGITAL_SIGNATURE),
        "1.3.6.1.5.5.7.3.4", Set.of(
            KeyUsage.DIGITAL_SIGNATURE, KeyUsage.CONTENT_COMMITMENT,
            KeyUsage.KEY_ENCIPHERMENT, KeyUsage.KEY_AGREEMENT),
        "1.3.6.1.5.5.7.3.8", Set.of(
            KeyUsage.DIGITAL_SIGNATURE, KeyUsage.CONTENT_COMMITMENT),
        "1.3.6.1.5.5.7.3.9", Set.of(
            KeyUsage.DIGITAL_SIGNATURE, KeyUsage.CONTENT_COMMITMENT));

    @AssertTrue(message = "OID는 올바른 점 구분 숫자 형식과 첫 두 구성요소의 범위를 만족해야 합니다")
    public boolean isValidOidFormat() {
        if (value == null) {
            return true; // null은 @NotBlank에서 검증합니다.
        }
        if (!OID_PATTERN.matcher(value).matches()) {
            return false;
        }
        if (value.charAt(0) == '2') {
            return true;
        }
        int nextDot = value.indexOf('.', 2);
        String secondArc = nextDot < 0 ? value.substring(2) : value.substring(2, nextDot);
        return secondArc.length() <= 2 && Integer.parseInt(secondArc) <= 39;
    }

    public boolean isCompatibleWith(Set<KeyUsage> keyUsages) {
        if (value == null) {
            return true;
        }
        Set<KeyUsage> compatibleUsages = COMPATIBLE_KEY_USAGES.get(value);
        return compatibleUsages == null || !Collections.disjoint(compatibleUsages, keyUsages);
    }
}
