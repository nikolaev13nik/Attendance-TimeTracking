package att.client.accounting;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import com.github.tomakehurst.wiremock.verification.LoggedRequest;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Span;
import io.micrometer.tracing.Tracer;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import att.client.accounting.api.AccountApi;
import att.controller.BaseApiControllerTest;

import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AccountingClientTracingTest extends BaseApiControllerTest {

    private static final Integer ID_USER = 42;
    private static final Integer TENANT_ID = 100;
    private static final String USER_PATH = "/account/tenant/" + TENANT_ID + "/userId/" + ID_USER;

    @RegisterExtension
    static WireMockExtension wireMock = WireMockExtension.newInstance().options(
            com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig().dynamicPort()).build();

    @Autowired
    private ObservationRegistry observationRegistry;

    @Autowired
    private Tracer tracer;

    private AccountApi accountApiPointedAtWireMock() {
        AccountingClientConfiguration configuration = new AccountingClientConfiguration();
        ReflectionTestUtils.setField(configuration, "baseUrl", wireMock.baseUrl());
        ReflectionTestUtils.setField(configuration, "connectTimeoutMs", 2000L);
        ReflectionTestUtils.setField(configuration, "readTimeoutMs", 5000L);
        return configuration.accountApi(observationRegistry);
    }

    @Test
    @DisplayName("A call to Accounting carries the caller's trace id in a W3C traceparent header")
    void accountingCallContinuesTheTraceTest() {
        wireMock.stubFor(get(urlEqualTo(USER_PATH)).willReturn(okJson(
                "{\"idUser\":42,\"firstName\":\"Jane\",\"lastName\":\"Doe\",\"tenantId\":100,\"roles\":[\"USER\"]}")));
        AccountApi accountApi = accountApiPointedAtWireMock();
        Span span = tracer.nextSpan().name("statistic-report").start();

        try (Tracer.SpanInScope ignored = tracer.withSpan(span)) {
            accountApi.getUser(TENANT_ID, ID_USER);
        } finally {
            span.end();
        }

        List<LoggedRequest> requests = wireMock.findAll(getRequestedFor(urlEqualTo(USER_PATH)));
        assertEquals(1, requests.size());
        String traceparent = requests.get(0).getHeader("traceparent");
        assertTrue(traceparent != null && traceparent.startsWith("00-" + span.context().traceId() + "-"),
                "Reason: Accounting must log under the same trace id as the request that called it, header: "
                        + traceparent);
    }
}
