package toy.pki.kms.domain.sig;

import java.security.GeneralSecurityException;
import java.security.Provider;
import java.security.PublicKey;
import java.security.Signature;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import toy.pki.kms.domain.key.KeyID;
import toy.pki.kms.domain.key.ManagedKey;
import toy.pki.kms.domain.key.ManagedKeyRepository;
import toy.pki.kms.domain.parameter.SignatureParameter;

@Slf4j
@Service
@RequiredArgsConstructor
public class SignatureService {

    private final Provider cryptographicProvider;
    private final ManagedKeyRepository managedKeyRepository;

    public byte[] sign(
        KeyID keyId,
        SignatureParameter parameter,
        byte[] data
    ) throws GeneralSecurityException {
        ManagedKey key = managedKeyRepository.findByKeyID(keyId);
        if (key == null) {
            throw new IllegalArgumentException("Key not found: " + keyId);
        }

        Signature signer = Signature.getInstance(
            parameter.getAlgorithm(),
            cryptographicProvider
        );

        signer.initSign(key.getKeyPair().getPrivate());
        signer.update(data);
        return signer.sign();
    }

    public boolean verify(PublicKey publicKey, SignatureParameter parameter, byte[] data, byte[] signature)
        throws GeneralSecurityException {
        Signature verifier = Signature.getInstance(parameter.getAlgorithm(), cryptographicProvider);

        verifier.initVerify(publicKey);
        verifier.update(data);

        return verifier.verify(signature);
    }
}
