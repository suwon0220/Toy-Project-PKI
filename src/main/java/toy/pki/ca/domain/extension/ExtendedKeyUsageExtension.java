package toy.pki.ca.domain.extension;

import java.util.Set;

public record ExtendedKeyUsageExtension(boolean critical, Set<ExtendedKeyUsageRecord> extendedKeyUsages) {
}
