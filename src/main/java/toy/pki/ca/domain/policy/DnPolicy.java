package toy.pki.ca.domain.policy;

public record DnPolicy(
    DnAttributePolicy domainComponent,
    DnAttributePolicy commonName,
    DnAttributePolicy organizationUnit,
    DnAttributePolicy organization,
    DnAttributePolicy country
){

}
