package att.client.accounting;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Set;

import att.client.accounting.api.AccountApi;
import att.client.accounting.dto.UserProfileDto;
import att.exceptions.InternalApiException;
import att.exceptions.NotFoundException;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


class AccountingClientConfigurationTest {

    private static final Integer ID_USER = 42;
    private static final String TOKEN_VALUE = "caller-jwt-token";
    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().options(
            com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort()).build();

    private AccountApi buildAccountApiPointedAtWireMock() {
        AccountingClientConfiguration configuration = new AccountingClientConfiguration();
        ReflectionTestUtils.setField(configuration, "baseUrl", wireMock.baseUrl());
        ReflectionTestUtils.setField(configuration, "connectTimeoutMs", 2000L);
        ReflectionTestUtils.setField(configuration, "readTimeoutMs", 5000L);

        return configuration.accountApi();
    }

    private void authenticateAs(String tokenValue) {
        Jwt jwt = Jwt.withTokenValue(tokenValue)
                .header("alg", "none")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600))
                .claim("sub", ID_USER.toString())
                .build();
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getUserInfo_parsesResponse_andForwardsCallerJwt() {
        authenticateAs(TOKEN_VALUE);
        wireMock.stubFor(get(urlEqualTo("/account/user/" + ID_USER)).willReturn(okJson(
                "{\"idUser\":42,\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"roles\":[\"USER\"]}")));

        UserProfileDto result = buildAccountApiPointedAtWireMock().getUserInfo(ID_USER);

        assertEquals(ID_USER, result.getIdUser());
        assertEquals("Jane", result.getFirstName());
        assertEquals("Doe", result.getLastName());
        assertEquals(Set.of("USER"), result.getRoles());
        wireMock.verify(getRequestedFor(urlEqualTo("/account/user/" + ID_USER))
                .withHeader("Authorization", equalTo("Bearer " + TOKEN_VALUE)));
    }

    @Test
    void getUserInfo_notFoundUpstream_throwsNotFoundException() {
        authenticateAs(TOKEN_VALUE);
        wireMock.stubFor(get(urlEqualTo("/account/user/" + ID_USER)).willReturn(aResponse().withStatus(404)));

        assertThrows(NotFoundException.class, () -> buildAccountApiPointedAtWireMock().getUserInfo(ID_USER));
    }

    @Test
    void getUserInfo_upstreamFailure_throwsInternalApiException() {
        authenticateAs(TOKEN_VALUE);
        wireMock.stubFor(get(urlEqualTo("/account/user/" + ID_USER)).willReturn(aResponse().withStatus(500)));

        assertThrows(InternalApiException.class, () -> buildAccountApiPointedAtWireMock().getUserInfo(ID_USER));
    }

    @Test
    void getUserInfo_noAuthenticationInContext_sendsRequestWithoutAuthorizationHeader() {
        wireMock.stubFor(get(urlEqualTo("/account/user/" + ID_USER)).willReturn(okJson(
                "{\"idUser\":42,\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"roles\":[]}")));

        buildAccountApiPointedAtWireMock().getUserInfo(ID_USER);

        wireMock.verify(getRequestedFor(urlEqualTo("/account/user/" + ID_USER))
                .withoutHeader("Authorization"));
    }
}
