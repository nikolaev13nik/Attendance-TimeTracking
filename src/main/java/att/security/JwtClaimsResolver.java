package att.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.Optional;

import att.exceptions.BadRequestException;

import static att.exceptions.ErrorConstants.REPORT_REQUESTER_UNRESOLVED_MSG;

/**
 * Single place that reads claims off the caller's JWT. Both the tenant check and any service needing the calling
 * user's id go through here, so the {@code instanceof Jwt} unwrapping lives in one spot.
 */
public final class JwtClaimsResolver {

    private JwtClaimsResolver() {
    }

    /**
     * The {@code tenantId} claim of the caller, or {@code null} when the request is not JWT-authenticated or the
     * claim is absent.
     */
    public static String currentTenantId(Authentication auth) {
        return currentJwt(auth).map(jwt -> jwt.getClaimAsString(SecurityConstants.TENANT_ID)).orElse(null);
    }

    /**
     * The {@code sub} claim of the caller as a user id.
     *
     * @throws BadRequestException when the request carries no JWT principal or {@code sub} is not a user id
     */
    public static Integer currentUserId() {
        String subject = currentJwt(SecurityContextHolder.getContext().getAuthentication())
                .map(Jwt::getSubject)
                .orElseThrow(() -> new BadRequestException(REPORT_REQUESTER_UNRESOLVED_MSG));
        try {
            return Integer.valueOf(subject);
        } catch (NumberFormatException e) {
            throw new BadRequestException(REPORT_REQUESTER_UNRESOLVED_MSG);
        }
    }

    private static Optional<Jwt> currentJwt(Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return Optional.of(jwt);
        }
        return Optional.empty();
    }
}
