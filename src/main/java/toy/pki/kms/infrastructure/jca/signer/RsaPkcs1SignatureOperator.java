package toy.pki.kms.infrastructure.jca.signer;

import jakarta.annotation.Nonnull;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.key.signature.RsaPkcs1SignatureParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

@Component
public class RsaPkcs1SignatureOperator extends AbstractJcaSignatureOperator {

    @Override
    public boolean supports(SignatureParameters parameters) {
        return parameters instanceof RsaPkcs1SignatureParameters;
    }

    @Override
    public byte[] sign(
        PrivateKey privateKey,
        @Nonnull SignatureParameters parameters,
        @Nonnull byte[] data) throws GeneralSecurityException {

        RsaPkcs1SignatureParameters rsa = (RsaPkcs1SignatureParameters) parameters;
        if (rsa.hashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "Hash algorithm is required");
        }

        if (!(privateKey instanceof RSAPrivateKey)) {
            throw new InvalidKeyException("An RSA private key is required");
        }

        String digest = getSignatureAlgorithm(rsa);
        Signature signature = Signature.getInstance(
            digest,
            provider);

        signature.initSign(privateKey);
        signature.update(data);
        return signature.sign();
    }

    @Override
    public boolean verify(
        @Nonnull PublicKey publicKey,
        SignatureParameters parameters,
        @Nonnull byte[] data,
        @Nonnull byte[] signature) throws GeneralSecurityException {

        RsaPkcs1SignatureParameters rsa = (RsaPkcs1SignatureParameters) parameters;
        if (rsa.hashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "Hash algorithm is required");
        }

        if (!(publicKey instanceof RSAPublicKey)) {
            throw new InvalidKeyException("An RSA public key is required");
        }

        String digest = getSignatureAlgorithm(rsa);

        Signature verifier = Signature.getInstance(
            digest,
            provider);

        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }

    private String getSignatureAlgorithm(
        RsaPkcs1SignatureParameters parameters)
        throws InvalidAlgorithmParameterException {
        if (parameters.hashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "Hash algorithm is required");
        }

        return switch (parameters.hashAlgorithm()) {
            case SHA256 -> "SHA256withRSA";
            case SHA384 -> "SHA384withRSA";
            case SHA512 -> "SHA512withRSA";
        };
    }
}
