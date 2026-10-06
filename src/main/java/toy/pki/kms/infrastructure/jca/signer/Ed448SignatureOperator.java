package toy.pki.kms.infrastructure.jca.signer;

import jakarta.annotation.Nonnull;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.key.signature.Ed448SignatureParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@Component
public class Ed448SignatureOperator extends AbstractJcaSignatureOperator {
    private static final String SIGNATURE_ALGORITHM = "Ed448";

    @Override
    public boolean supports(SignatureParameters parameters) {
        return parameters instanceof Ed448SignatureParameters;
    }

    @Override
    public byte[] sign(
        PrivateKey privateKey,
        @Nonnull SignatureParameters parameters,
        @Nonnull byte[] data) throws GeneralSecurityException {
        if (!(parameters instanceof Ed448SignatureParameters)) {
            throw new InvalidAlgorithmParameterException(
                "Ed448 parameters are required");
        }
        Signature signature = Signature.getInstance(
            SIGNATURE_ALGORITHM,
            provider);

        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    @Override
    public boolean verify(
        @Nonnull PublicKey publicKey,
        @Nonnull SignatureParameters parameters,
        @Nonnull byte[] data,
        @Nonnull byte[] signature) throws GeneralSecurityException {
        if (!(parameters instanceof Ed448SignatureParameters)) {
            throw new InvalidAlgorithmParameterException(
                "Ed448 parameters are required");
        }

        Signature verifier = Signature.getInstance(
            SIGNATURE_ALGORITHM,
            provider);

        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }
}
