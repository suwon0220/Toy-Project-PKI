package toy.pki.ca.domain.certificate;

import java.security.cert.X509Certificate;

public interface CertificateBuilder {

    X509Certificate buildCertificate(CertificateData certificateData, ExtensionProfile extensionProfile);
}
