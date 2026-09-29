package toy.pki.ca.domain.extension;

public record ExtendedKeyUsageRecord (
        String oid,
        String displayName
) {
}
