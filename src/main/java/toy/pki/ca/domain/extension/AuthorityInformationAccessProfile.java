package toy.pki.ca.domain.extension;

public record AuthorityInformationAccessProfile(
    boolean includeAiaOcsp,
    boolean includeAiaCaIssuers,
    boolean includeCd
) {
}
