package toy.pki.ca.domain.certificate;

import lombok.Data;

public record CertificateSubject (
    String domainComponent,
    String commonName,
    String organizationUnit,
    String organization,
    String country
){

}
