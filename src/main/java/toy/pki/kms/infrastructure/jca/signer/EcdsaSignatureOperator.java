package toy.pki.kms.infrastructure.jca.signer;

import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.ECPublicKey;

import org.springframework.stereotype.Component;

import jakarta.annotation.Nonnull;
import toy.pki.kms.domain.key.signature.EcdsaSignatureParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@Component
public class EcdsaSignatureOperator extends AbstractJcaSignatureOperator {
    @Override
    public boolean supports(SignatureParameters parameters) {
        return parameters instanceof EcdsaSignatureParameters;
    }

    @Override
    public byte[] sign(
        @Nonnull PrivateKey privateKey,
        @Nonnull SignatureParameters parameters,
        @Nonnull byte[] data) throws GeneralSecurityException {

        EcdsaSignatureParameters ecdsa = (EcdsaSignatureParameters) parameters;
        String algorithm = getSignatureAlgorithm(ecdsa);

        if (!(privateKey instanceof ECPrivateKey)) {
            throw new InvalidKeyException("An EC private key is required");
        }

        Signature signature = Signature.getInstance(
            algorithm,
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
        EcdsaSignatureParameters ecdsa = (EcdsaSignatureParameters) parameters;
        String algorithm = getSignatureAlgorithm(ecdsa);

        if (!(publicKey instanceof ECPublicKey)) {
            throw new InvalidKeyException("An EC public key is required");
        }

        Signature verifier = Signature.getInstance(
            algorithm,
            provider);

        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }

    private String getSignatureAlgorithm(EcdsaSignatureParameters parameters)
        throws InvalidAlgorithmParameterException {
        if (parameters.hashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "Hash algorithm is required");
        }

        return switch (parameters.hashAlgorithm()) {
            case SHA256 -> "SHA256withECDSA";
            case SHA384 -> "SHA384withECDSA";
            case SHA512 -> "SHA512withECDSA";
        };
    }
}