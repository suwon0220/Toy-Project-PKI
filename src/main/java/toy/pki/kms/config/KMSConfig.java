package toy.pki.kms.config;


import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

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
}
