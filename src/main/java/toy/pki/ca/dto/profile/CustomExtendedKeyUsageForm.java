package toy.pki.ca.dto.profile;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomExtendedKeyUsageForm {
    private String oid;
    private String displayName;
}
