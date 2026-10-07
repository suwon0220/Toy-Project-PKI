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
import java.security.spec.MGF1ParameterSpec;
import java.security.spec.PSSParameterSpec;
import org.springframework.stereotype.Component;
import toy.pki.kms.domain.signature.RsaPssSignatureParameters;
import toy.pki.kms.domain.signature.SignatureParameters;

@Component
public class RsaPssSignatureOperator extends AbstractJcaSignatureOperator {

    private static final String SIGNATURE_ALGORITHM = "RSASSA-PSS";

    @Override
    public boolean supports(SignatureParameters parameters) {
        return parameters instanceof RsaPssSignatureParameters;
    }

    @Override
    public byte[] sign(
        PrivateKey privateKey,
        @Nonnull SignatureParameters parameters,
        @Nonnull byte[] data) throws GeneralSecurityException {

        RsaPssSignatureParameters pss = (RsaPssSignatureParameters) parameters;
        if (pss.hashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "Hash algorithm is required");
        }

        if (pss.mgf1HashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "MGF1 hash algorithm is required");
        }

        if (pss.saltLength() < 0) {
            throw new InvalidAlgorithmParameterException(
                "Salt length must be non-negative");
        }

        if (!(privateKey instanceof RSAPrivateKey)) {
            throw new InvalidKeyException("An RSA private key is required");
        }

        String digest = pss.hashAlgorithm().getAlgorithmName();
        String mgf1Digest = pss.mgf1HashAlgorithm().getAlgorithmName();

        PSSParameterSpec spec = new PSSParameterSpec(
            digest,
            "MGF1",
            new MGF1ParameterSpec(mgf1Digest),
            pss.saltLength(),
            PSSParameterSpec.TRAILER_FIELD_BC);

        Signature signature = Signature.getInstance(
            SIGNATURE_ALGORITHM,
            provider);

        signature.setParameter(spec);
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

        RsaPssSignatureParameters pss = (RsaPssSignatureParameters) parameters;
        if (pss.hashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "Hash algorithm is required");
        }

        if (pss.mgf1HashAlgorithm() == null) {
            throw new InvalidAlgorithmParameterException(
                "MGF1 hash algorithm is required");
        }

        if (pss.saltLength() < 0) {
            throw new InvalidAlgorithmParameterException(
                "Salt length must be non-negative");
        }

        if (!(publicKey instanceof RSAPublicKey)) {
            throw new InvalidKeyException("An RSA public key is required");
        }

        String digest = pss.hashAlgorithm().getAlgorithmName();
        String mgf1Digest = pss.mgf1HashAlgorithm().getAlgorithmName();

        PSSParameterSpec spec = new PSSParameterSpec(
            digest,
            "MGF1",
            new MGF1ParameterSpec(mgf1Digest),
            pss.saltLength(),
            PSSParameterSpec.TRAILER_FIELD_BC);

        Signature verifier = Signature.getInstance(
            SIGNATURE_ALGORITHM,
            provider);

        verifier.setParameter(spec);
        verifier.initVerify(publicKey);
        verifier.update(data);
        return verifier.verify(signature);
    }
}
