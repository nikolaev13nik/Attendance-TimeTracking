package att.client.common;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

import att.exceptions.InternalApiException;
import att.exceptions.NotFoundException;

/**
 * Global error handler for internal API clients. Registered once, per client, on that client's
 * {@code RestClient.Builder} via {@code defaultStatusHandler(...)} - it then runs for every non-2xx
 * response made through that client, so the generated API (e.g. {@link att.client.accounting.api.AccountApi})
 * can be injected and called directly, with no per-method try/catch. Adding the next endpoint on an
 * existing client, or wiring up a brand-new internal client, never needs new error-handling code - a
 * new client just constructs this same class with its own service name.
 */
public class InternalApiResponseErrorHandler implements ResponseErrorHandler {

    private final String serviceName;

    public InternalApiResponseErrorHandler(String serviceName) {
        this.serviceName = serviceName;
    }

    @Override
    public boolean hasError(ClientHttpResponse response) throws IOException {
        return response.getStatusCode().isError();
    }

    @Override
    public void handleError(URI url, HttpMethod method, ClientHttpResponse response) throws IOException {
        if (response.getStatusCode() == HttpStatus.NOT_FOUND) {
            throw new NotFoundException("Not found in " + serviceName + " service");
        }
        throw new InternalApiException(
                serviceName + " call failed: " + response.getStatusCode() + " " + response.getStatusText(), null);
    }
}
