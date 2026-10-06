package toy.pki.kms.application.model;

import java.util.Locale;
import java.util.function.Predicate;

import toy.pki.kms.domain.key.KeyAlgorithm;
import toy.pki.kms.domain.key.ManagedKey;

public record KeySearchCriteria(
    KeyAlgorithm algorithm,
    String keyword) {

    public KeySearchCriteria {
        keyword = keyword == null || keyword.isBlank()
                  ? null
                  : keyword.strip().toLowerCase(Locale.ROOT);
    }

    public Predicate<ManagedKey> toPredicate() {
        return key -> (algorithm == null || key.getKeyAlgorithm() == algorithm)
            && (keyword == null
            || key.getKeyId().value().toLowerCase(Locale.ROOT).contains(keyword)
            || (key.getAlias() != null && key.getAlias().toLowerCase(Locale.ROOT).contains(keyword)));
    }
}
