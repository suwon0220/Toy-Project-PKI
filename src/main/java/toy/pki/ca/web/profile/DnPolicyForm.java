package toy.pki.ca.web.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import toy.pki.ca.domain.policy.DnAttributePolicy;
import toy.pki.ca.domain.policy.DnPolicy;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DnPolicyForm {

    private AttributeForm domainComponent = new AttributeForm();
    private AttributeForm commonName = new AttributeForm();
    private AttributeForm organizationUnit = new AttributeForm();
    private AttributeForm organization = new AttributeForm();
    private AttributeForm country = new AttributeForm();

    public static DnPolicyForm from(DnPolicy policy) {
        if (policy == null) {
            return new DnPolicyForm();
        }
        return new DnPolicyForm(
            AttributeForm.from(policy.domainComponent()),
            AttributeForm.from(policy.commonName()),
            AttributeForm.from(policy.organizationUnit()),
            AttributeForm.from(policy.organization()),
            AttributeForm.from(policy.country()));
    }

    public DnPolicy toPolicy() {
        return new DnPolicy(domainComponent.toPolicy(), commonName.toPolicy(),
            organizationUnit.toPolicy(), organization.toPolicy(), country.toPolicy());
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AttributeForm {

        private boolean required;
        private String fixedValue;

        private static AttributeForm from(DnAttributePolicy policy) {
            return policy == null ? new AttributeForm()
                : new AttributeForm(policy.isRequired(), policy.getFixedValue());
        }

        private DnAttributePolicy toPolicy() {
            return new DnAttributePolicy(required, fixedValue);
        }
    }
}
