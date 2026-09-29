package toy.pki.ca.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import toy.pki.ca.domain.extension.KeyUsage;

import java.util.EnumSet;
import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class KeyUsageForm {
    private boolean enabled;
    private boolean critical;
    private Set<KeyUsage> usages;
}
