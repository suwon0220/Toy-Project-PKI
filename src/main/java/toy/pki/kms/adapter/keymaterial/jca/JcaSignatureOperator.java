package toy.pki.kms.adapter.keymaterial.jca;

import java.security.GeneralSecurityException;
import java.security.PrivateKey;
import java.security.PublicKey;

import toy.pki.kms.domain.signature.SignatureParameters;

public interface JcaSignatureOperator {
    boolean supports(SignatureParameters parameters);
    byte[] sign(PrivateKey privateKey, SignatureParameters parameters, byte[] data)
        throws GeneralSecurityException;
    boolean verify(PublicKey publicKey, SignatureParameters parameters, byte[] data, byte[] signature)
        throws GeneralSecurityException;
}
