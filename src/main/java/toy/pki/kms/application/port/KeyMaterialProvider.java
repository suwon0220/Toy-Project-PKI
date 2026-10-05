package toy.pki.kms.application.port;

import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.util.Optional;

import toy.pki.kms.application.model.GeneratedKeyMaterial;
import toy.pki.kms.domain.key.KeyMaterialRef;
import toy.pki.kms.domain.key.KeyProviderId;
import toy.pki.kms.domain.key.generation.KeyGenerationParameters;
import toy.pki.kms.domain.key.signature.SignatureParameters;

public interface KeyMaterialProvider {

    KeyProviderId id();

    GeneratedKeyMaterial generate(
        KeyGenerationParameters parameters) throws NoSuchAlgorithmException, InvalidAlgorithmParameterException;

    Optional<PublicKey> getPublicKey(
        KeyMaterialRef reference);

    byte[] sign(
        KeyMaterialRef reference,
        SignatureParameters parameters,
        byte[] data);

    boolean verify(
        KeyMaterialRef reference,
        SignatureParameters parameters,
        byte[] data,
        byte[] signature);

    void delete(
        KeyMaterialRef reference);
}