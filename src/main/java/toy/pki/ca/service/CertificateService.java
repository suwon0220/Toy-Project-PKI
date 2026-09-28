package toy.pki.ca.service;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.stereotype.Service;

import java.math.BigInteger;
import java.security.Security;

@Service
public class CertificateService {

    public CertificateService() {
        Security.addProvider(new BouncyCastleProvider());
    }

    // TODO: Implement certificate issuance
    // Issuer Cert data, Subject Cert data, CertificateProfile, KeyPair
    public void issueCertificate() {
    }

    public void revokeCertificate(BigInteger serialNumber) {
    }
}
