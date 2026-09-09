package att.client.common;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.web.client.ResponseErrorHandler;

import java.io.IOException;
import java.net.URI;

import att.exceptions.InternalApiException;
import att.exceptions.NotFoundException;


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
