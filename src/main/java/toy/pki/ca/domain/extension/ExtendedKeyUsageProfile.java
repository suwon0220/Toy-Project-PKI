package toy.pki.ca.domain.extension;

import java.util.Set;

public record ExtendedKeyUsageProfile(boolean critical, Set<ExtendedKeyUsageRecord> extendedKeyUsages) {
}
