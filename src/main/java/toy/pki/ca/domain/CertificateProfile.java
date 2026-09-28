package toy.pki.ca.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Data
@RequiredArgsConstructor
public class CertificateProfile {
    @NonNull private String name;
    private String description;
    private final List<ExtensionProfile> extensions;

    public void addExtension(ExtensionProfile extensionProfile) {
        this.extensions.add(extensionProfile);
    }
}

