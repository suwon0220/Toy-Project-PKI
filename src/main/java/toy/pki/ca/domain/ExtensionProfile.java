package toy.pki.ca.domain;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ExtensionProfile {
    private String name;
    private boolean critical;
    private boolean enabled;
}
