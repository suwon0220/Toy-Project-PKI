package toy.pki.kms.service;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.kms.domain.parameter.SignatureParameter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final Provider cryptographicProvider;

    public byte[] sign(PrivateKey privateKey, SignatureParameter parameter, byte[] data)
        throws GeneralSecurityException {
        Signature signer = Signature.getInstance(parameter.getAlgorithm(), cryptographicProvider);

        signer.initSign(privateKey);
        signer.update(data);

        byte[] signatureBytes = signer.sign();

        log.debug(
            "Signature generated. algorithm={}, length={}",
            parameter.getAlgorithm(),
            signatureBytes.length);
        return signatureBytes;
    }

    public boolean verify(PublicKey publicKey, SignatureParameter parameter, byte[] data, byte[] signature)
        throws GeneralSecurityException {
        Signature verifier = Signature.getInstance(parameter.getAlgorithm(), cryptographicProvider);

        verifier.initVerify(publicKey);
        verifier.update(data);

        return verifier.verify(signature);
    }
}
