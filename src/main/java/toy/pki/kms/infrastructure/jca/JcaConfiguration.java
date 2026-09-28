package toy.pki.kms.infrastructure.jca;

import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.security.Provider;
import java.security.Security;

@Configuration
public class JcaConfiguration {

    @Bean
    public Provider cryptographicProvider() {
        Provider existingProvider
                = Security.getProvider(BouncyCastleProvider.PROVIDER_NAME);
        if (existingProvider != null) {
            return existingProvider;
        }

        Provider provider = new BouncyCastleProvider();
        Security.addProvider(provider);
        return provider;
    }

}
