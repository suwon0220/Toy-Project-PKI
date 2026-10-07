package toy.pki.ca.domain.certificate;

public record CertificateSubject(
    String domainComponent,
    String commonName,
    String organizationUnit,
    String organization,
    String country) {

}
