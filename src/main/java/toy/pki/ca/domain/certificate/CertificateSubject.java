package toy.pki.ca.domain.certificate;

import lombok.Data;

public record CertificateSubject (
    String domainComponent,
    String commonName,
    String organization,
    String organizationalUnit,
    String locality,
    String state,
    String country
){

}
