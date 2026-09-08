package att.client.common;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.ClientHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import att.exceptions.InternalApiException;
import att.exceptions.NotFoundException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Isolated unit test for the handler every internal API client (current and future) shares - not a
 * WireMock/Spring-context test since there's no HTTP stack to exercise here, only the pure
 * status-code-to-exception mapping.
 */
class InternalApiResponseErrorHandlerTest {

    private static final String SERVICE_NAME = "attendance-accounting";

    private final InternalApiResponseErrorHandler handler = new InternalApiResponseErrorHandler(SERVICE_NAME);

    private static ClientHttpResponse fakeResponse(HttpStatus status) {
        return new ClientHttpResponse() {
            @Override
            public HttpStatusCode getStatusCode() {
                return status;
            }

            @Override
            public String getStatusText() {
                return status.getReasonPhrase();
            }

            @Override
            public void close() {
                // no-op
            }

            @Override
            public InputStream getBody() {
                return new ByteArrayInputStream(new byte[0]);
            }

            @Override
            public HttpHeaders getHeaders() {
                return new HttpHeaders();
            }
        };
    }

    @Test
    void hasError_falseForSuccessfulResponse() throws IOException {
        assertFalse(handler.hasError(fakeResponse(HttpStatus.OK)));
    }

    @Test
    void hasError_trueForErrorResponse() throws IOException {
        assertTrue(handler.hasError(fakeResponse(HttpStatus.INTERNAL_SERVER_ERROR)));
    }

    @Test
    void handleError_notFound_throwsNotFoundExceptionNamingService() {
        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> handler.handleError(URI.create("http://accounting/account/user/1"), HttpMethod.GET,
                        fakeResponse(HttpStatus.NOT_FOUND)));

        assertTrue(exception.getMessage().contains(SERVICE_NAME));
    }

    @Test
    void handleError_otherErrorStatus_throwsInternalApiExceptionNamingService() {
        InternalApiException exception = assertThrows(InternalApiException.class,
                () -> handler.handleError(URI.create("http://accounting/account/user/1"), HttpMethod.GET,
                        fakeResponse(HttpStatus.INTERNAL_SERVER_ERROR)));

        assertTrue(exception.getMessage().contains(SERVICE_NAME));
    }
}
