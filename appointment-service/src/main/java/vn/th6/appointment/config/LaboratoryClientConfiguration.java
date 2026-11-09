package vn.th6.appointment.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class LaboratoryClientConfiguration {
    @Bean
    RestClient laboratoryRestClient(
            RestClient.Builder builder,
            @Value("${laboratory.base-url}") String baseUrl,
            @Value("${laboratory.connect-timeout:2s}") Duration connectTimeout,
            @Value("${laboratory.read-timeout:3s}") Duration readTimeout) {
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(connectTimeout)
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(readTimeout);
        return builder.baseUrl(baseUrl).requestFactory(requestFactory).build();
    }
}
