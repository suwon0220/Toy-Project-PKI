package toy.pki.kms.adapter.http;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;
import toy.pki.kms.domain.HashAlgorithm;
import toy.pki.kms.domain.KeyId;
import toy.pki.kms.domain.ManagedKey;
import toy.pki.kms.domain.SignatureAlgorithm;
import toy.pki.kms.web.KeyAlgorithmPreset;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestKmsClient {

    private static final String API = "/kms/api/v1/keys";
    private final RestClient client;

    public List<ManagedKey> findAll() {
        log.debug("Requesting KMS key list");
        KeyData[] keys = client.get()
            .uri(API)
            .retrieve()
            .body(KeyData[].class);

        required(keys);
        log.debug("KMS key list retrieved: count={}", keys.length);
        return Arrays.stream(keys).map(KeyData::toManagedKey).toList();
    }

    public ManagedKey generate(KeyAlgorithmPreset preset, String alias) {
        log.debug("Requesting KMS key generation: algorithm={}", preset);
        KeyData key = client.post()
            .uri(API + "/generate")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new GenerateRequest(alias, preset.name()))
            .retrieve()
            .body(KeyData.class);

        ManagedKey managedKey = required(key).toManagedKey();
        log.info("KMS key generated: keyId={}, algorithm={}", managedKey.getKeyId().value(), preset);
        return managedKey;
    }

    public byte[] sign(KeyId keyId, SignatureAlgorithm algorithm, HashAlgorithm digest, byte[] data) {
        digest = digest(algorithm, digest);
        log.debug("Requesting KMS signature: keyId={}, algorithm={}, digest={}, dataLength={}",
            keyId.value(), algorithm, digest, data.length);
        SignResponse response = client.post()
            .uri(API + "/sign")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new SignRequest(keyId, algorithm, digest, data))
            .retrieve()
            .body(SignResponse.class);

        required(response);
        byte[] signature = required(response.signature());
        log.debug("KMS signing completed: keyId={}, signatureLength={}", keyId.value(), signature.length);
        return signature;
    }

    public boolean verify(KeyId keyId, SignatureAlgorithm algorithm, HashAlgorithm digest, byte[] data, byte[] signature) {
        digest = digest(algorithm, digest);
        log.debug("Requesting KMS verification: keyId={}, algorithm={}, digest={}",
            keyId.value(), algorithm, digest);
        VerifyResponse response = client.post()
            .uri(API + "/verify")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new VerifyRequest(keyId, algorithm, digest, data, signature))
            .retrieve()
            .body(VerifyResponse.class);

        required(response);
        boolean valid = required(response.verify_result());
        log.debug("KMS verification completed: keyId={}, valid={}", keyId.value(), valid);
        return valid;
    }

    public byte[] getPublicKeyOf(KeyId keyId) {
        log.debug("Requesting KMS public key: keyId={}", keyId.value());
        // Toy-Project-KMS requires JSON in the body of this GET request.
        PublicKeyResponse response = client.method(HttpMethod.GET)
            .uri(API + "/pub")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new PublicKeyRequest(keyId))
            .retrieve()
            .body(PublicKeyResponse.class);

        required(response);
        if (!keyId.equals(response.keyId())) {
            throw new RestClientException("KMS returned a different key ID");
        }
        byte[] publicKey = required(response.publicKey());
        log.debug("KMS public key retrieved: keyId={}", keyId.value());
        return publicKey;
    }

    public KeyId delete(KeyId keyId) {
        log.debug("Requesting KMS key deletion: keyId={}", keyId.value());
        KeyId[] deleted = client.post()
            .uri(API + "/delete")
            .contentType(MediaType.APPLICATION_JSON)
            .body(new KeyId[] { keyId })
            .retrieve()
            .body(KeyId[].class);

        required(deleted);
        if (!Arrays.asList(deleted).contains(keyId)) {
            throw new RestClientException("KMS did not confirm key deletion");
        }
        log.info("KMS key deleted: keyId={}", keyId.value());
        return keyId;
    }

    private HashAlgorithm digest(SignatureAlgorithm algorithm, HashAlgorithm hashAlgorithm) {
        return switch (algorithm) {
            case Ed25519, Ed448 -> null;
            case RSA_PKCS1_V1_5, ECDSA -> {
                if (hashAlgorithm == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A digest algorithm is required");
                }
                yield hashAlgorithm;
            }
            case RSA_PSS, DSA -> throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST, "KMS does not support this signature algorithm");
        };
    }

    private <T> T required(T response) {
        if (response == null) {
            throw new RestClientException("KMS returned an incomplete response");
        }
        return response;
    }

    public record KeyData(
        KeyId id,
        String alias,
        KeyAlgorithmPreset keyAlgorithm,
        Instant createdAt) {
        ManagedKey toManagedKey() {
            if (id == null || id.value() == null || id.value().isBlank() || keyAlgorithm == null || createdAt == null) {
                throw new RestClientException("KMS returned incomplete key metadata");
            }
            if (alias != null && alias.strip().length() > ManagedKey.MAX_ALIAS_LENGTH) {
                throw new RestClientException("KMS returned an invalid key alias");
            }
            ManagedKey key = new ManagedKey(id, keyAlgorithm, createdAt);
            key.setAlias(alias);
            return key;
        }
    }

    public record GenerateRequest(
        String alias,
        String keyAlgorithm) {
    }

    public record SignRequest(
        KeyId keyId,
        SignatureAlgorithm signatureAlgorithm,
        HashAlgorithm digestAlgorithm,
        byte[] data) {
    }

    public record SignResponse(
        byte[] signature) {
    }

    public record VerifyRequest(
        KeyId keyId,
        SignatureAlgorithm signatureAlgorithm,
        HashAlgorithm digestAlgorithm,
        byte[] data,
        byte[] signature) {
    }

    public record VerifyResponse(
        Boolean verify_result) {
    }

    public record PublicKeyRequest(
        KeyId id) {
    }

    public record PublicKeyResponse(
        KeyId keyId,
        byte[] publicKey) {
    }

}
