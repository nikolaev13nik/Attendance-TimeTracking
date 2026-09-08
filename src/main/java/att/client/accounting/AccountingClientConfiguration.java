package att.client.accounting;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

import att.client.accounting.api.AccountApi;
import att.client.accounting.support.ApiClient;
import att.client.common.InternalApiResponseErrorHandler;

@Configuration
public class AccountingClientConfiguration {

    @Value("${att.integration.attendance-accounting.base-url}")
    private String baseUrl;

    @Value("${att.integration.attendance-accounting.connect-timeout-ms}")
    private long connectTimeoutMs;

    @Value("${att.integration.attendance-accounting.read-timeout-ms}")
    private long readTimeoutMs;

    @Bean
    AccountApi accountApi() {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                        .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                        .build());
        requestFactory.setReadTimeout(Duration.ofMillis(readTimeoutMs));

        RestClient restClient = ApiClient.buildRestClientBuilder(ApiClient.createDefaultMapper(null))
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .requestInterceptor(forwardCallerJwtInterceptor())
                .defaultStatusHandler(new InternalApiResponseErrorHandler("attendance-accounting"))
                .build();

        return new AccountApi(new ApiClient(restClient));
    }

    private ClientHttpRequestInterceptor forwardCallerJwtInterceptor() {
        return (request, body, execution) -> {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
                request.getHeaders().setBearerAuth(jwt.getTokenValue());
            }
            return execution.execute(request, body);
        };
    }
}
