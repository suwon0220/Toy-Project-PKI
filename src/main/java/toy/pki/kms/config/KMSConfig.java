package toy.pki.kms.config;

import java.net.http.HttpClient;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
public class KMSConfig {

    @Bean
    public RestClient kmsRestClient(
        @Value("${kms.base-url}") String baseUrl,
        @Value("${kms.connect-timeout}") Duration connectTimeout,
        @Value("${kms.read-timeout}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder().connectTimeout(connectTimeout).build();
        JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
        factory.setReadTimeout(readTimeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory)
            .defaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE).build();
    }
}
