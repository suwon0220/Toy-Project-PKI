package toy.pki.ca.domain.certificate;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.X500NameBuilder;
import org.jspecify.annotations.NonNull;

@Data
public class DistinguishedName {

    @Pattern(regexp="^[A-Z]{2}$") private String country;
    private String stateOrProvince;
    @Size(max=64) private String commonName;
    @Size(max=64) private String organization;
    @Size(max=128) private String organizationalUnit;

    public void setCountry(String country) {
        this.country = "".equals(country) ? null : country;
    }

    X500Name toX500Name() {
        X500NameBuilder builder = new X500NameBuilder();
        if (country != null) {
            builder.addRDN(org.bouncycastle.asn1.x500.style.BCStyle.C, country);
        }
        if (stateOrProvince != null) {
            builder.addRDN(org.bouncycastle.asn1.x500.style.BCStyle.ST, stateOrProvince);
        }
        if (commonName != null) {
            builder.addRDN(org.bouncycastle.asn1.x500.style.BCStyle.CN, commonName);
        }
        if (organization != null) {
            builder.addRDN(org.bouncycastle.asn1.x500.style.BCStyle.O, organization);
        }
        if (organizationalUnit != null) {
            builder.addRDN(org.bouncycastle.asn1.x500.style.BCStyle.OU, organizationalUnit);
        }
        return builder.build();
    }

    DistinguishedName overlay(@NonNull DistinguishedName other) {
        DistinguishedName result = new DistinguishedName();
        result.setCommonName(other.getCommonName() != null ? other.getCommonName() : this.getCommonName());
        result.setOrganization(other.getOrganization() != null ? other.getOrganization() : this.getOrganization());
        result.setOrganizationalUnit(other.getOrganizationalUnit() != null ? other.getOrganizationalUnit() : this.getOrganizationalUnit());
        result.setCountry(other.getCountry() != null ? other.getCountry() : this.getCountry());
        result.setStateOrProvince(other.getStateOrProvince() != null ? other.getStateOrProvince() : this.getStateOrProvince());
        return result;
    }
}
