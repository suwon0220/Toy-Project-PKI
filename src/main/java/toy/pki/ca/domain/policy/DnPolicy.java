package toy.pki.ca.domain.policy;

import jakarta.persistence.Embeddable;

@Embeddable
public record DnPolicy(
    DnAttributePolicy domainComponent,
    DnAttributePolicy commonName,
    DnAttributePolicy organizationUnit,
    DnAttributePolicy organization,
    DnAttributePolicy country
) {

}
