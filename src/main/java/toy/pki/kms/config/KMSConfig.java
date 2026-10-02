package toy.pki.kms.config;


import java.io.IOException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import toy.pki.kms.application.port.KeyMaterialProvider;
import toy.pki.kms.application.registry.KeyMaterialProviderRegistry;
import toy.pki.kms.domain.key.KeyProviderId;

@Slf4j
@Configuration
public class KMSConfig {

    static private final String KEYID_DIGEST_ALGORITHM = "SHA-256";

    @Bean
    public MessageDigest keyIdMessageDigest() {
        try {
            return MessageDigest.getInstance(KEYID_DIGEST_ALGORITHM);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(
                KEYID_DIGEST_ALGORITHM + " algorithm is not available in the environment.", e
            );
        }
    }

    @Bean
    public KeyMaterialProviderRegistry keyMaterialProviderRegistry(List<KeyMaterialProvider> providers) {
        return new KeyMaterialProviderRegistry(providers);
    }
}
