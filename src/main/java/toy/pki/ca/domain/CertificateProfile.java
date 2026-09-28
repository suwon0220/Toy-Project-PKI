package toy.pki.ca.domain;

import lombok.Data;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

import java.util.List;


@Data
@RequiredArgsConstructor
public class CertificateProfile {
    private final List<ExtensionProfile> extensions;
    @NonNull
    private String name;
    private String description;

    public void addExtension(ExtensionProfile extensionProfile) {
        this.extensions.add(extensionProfile);
    }
}

