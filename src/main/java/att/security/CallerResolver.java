package att.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

import co.il.attendance.security.JwtClaimsResolver;

public final class CallerResolver {

    private CallerResolver() {
    }

    public static Integer currentUserId() {
        return toId(JwtClaimsResolver.currentJwt(currentAuthentication()).map(Jwt::getSubject).orElse(null));
    }

    public static Integer currentHomeTenantId() {
        return toId(JwtClaimsResolver.currentTenantId(currentAuthentication()));
    }

    private static Authentication currentAuthentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private static Integer toId(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
