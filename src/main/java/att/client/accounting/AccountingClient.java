package att.client.accounting;

import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;

import java.util.function.Supplier;

import att.client.accounting.api.AccountApi;
import att.client.accounting.dto.UserProfileDto;
import att.exceptions.AccountingServiceException;
import att.exceptions.NotFoundException;
import lombok.RequiredArgsConstructor;

/**
 * Thin wrapper around the generated {@link AccountApi} client - the only class that should be
 * injected by strategy services that need to call attendance-accounting. Not wired into any
 * flow yet; {@code getUserInfo} calls a placeholder endpoint (see the local copy of
 * attendance-accounting-account.yaml) pending the real upstream design.
 *
 * <p>Each public method is a thin, domain-named entry point for one endpoint actually used by
 * this app (see CLAUDE.md - user registration/role management stays owned by Accounting, so only
 * read-style lookups belong here). The HTTP-exception-to-domain-exception translation itself
 * lives once, in {@link #callOrNotFound(Supplier, Supplier)} - adding a new lookup-by-id style
 * endpoint is a one-line method, not a new try/catch block. A future endpoint with no 404
 * semantics (e.g. a list/collection call) should add a sibling {@code call(Supplier<T>)} helper
 * that drops the {@code HttpClientErrorException.NotFound} catch - not written here since nothing
 * calls it yet.
 */
@Component
@RequiredArgsConstructor
public class AccountingClient {

    private final AccountApi accountApi;

    public UserProfileDto getUserInfo(Integer idUser) {
        return callOrNotFound(() -> accountApi.getUserInfo(idUser),
                () -> new NotFoundException("User " + idUser + " not found in accounting service"));
    }

    /**
     * Runs an {@link AccountApi} lookup-by-id call, translating a 404 into
     * {@link NotFoundException} (caller controls the message) and any other RestClient failure
     * into {@link AccountingServiceException}.
     */
    private <T> T callOrNotFound(Supplier<T> apiCall, Supplier<NotFoundException> notFoundException) {
        try {
            return apiCall.get();
        } catch (HttpClientErrorException.NotFound e) {
            throw notFoundException.get();
        } catch (RestClientException e) {
            throw new AccountingServiceException("attendance-accounting call failed: " + e.getMessage(), e);
        }
    }
}
