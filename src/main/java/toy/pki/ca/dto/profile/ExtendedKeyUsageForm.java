package toy.pki.ca.dto.profile;

import lombok.Data;
import toy.pki.ca.domain.extension.StandardExtendedKeyUsage;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

@Data
public class ExtendedKeyUsageForm {
    private boolean enabled;
    private boolean critical;

    private Set<StandardExtendedKeyUsage> standardExtendedKeyUsages = EnumSet.noneOf(StandardExtendedKeyUsage.class);
    private List<CustomExtendedKeyUsageForm> customExtendedKeyUsages = new ArrayList<>();
}
