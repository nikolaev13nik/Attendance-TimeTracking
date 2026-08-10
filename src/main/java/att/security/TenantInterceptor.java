package att.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;
import java.util.Objects;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private static final String TENANT_ID_PATH = "tenantId";
    private static final String ADMIN_ROLE = "ROLE_ADMINISTRATOR";
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String pathTenantId = getPathTenantId(request);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (pathTenantId == null || isAdmin(Objects.requireNonNull(auth))) {
            return true;
        }

        String jwtTenantId = getJwtTenantId(pathTenantId, auth);
        if (jwtTenantId == null) {
            log.error("Request was rejected: lack of tenant ID in security context");
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Request was rejected: lack of tenant ID in security context");
            return false;
        }

        if (!pathTenantId.equals(jwtTenantId)) {
            log.error("Request was rejected by Nikita1");
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Request was rejected: JWT's tenant does not match " +
                    "to path");
            return false;
        }

        log.debug("Tenant verification passed for tenantId: {} on URI: {}", pathTenantId, request.getRequestURI());
        return true;
    }

    private String getPathTenantId(HttpServletRequest request) {
        Object uriVars = request.getAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE);
        if (uriVars instanceof Map<?, ?> pathVars) {
            Object tenantId = pathVars.get(TENANT_ID_PATH);
            return tenantId != null ? tenantId.toString() : null;
        }
        return null;
    }

    private String getJwtTenantId(String pathTenantId, Authentication auth) {
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString(TENANT_ID_PATH);
        }
        return null;
    }

    private boolean isAdmin(Authentication auth) {
        return auth.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).filter(Objects::nonNull)
                .anyMatch(role -> role.equals(ADMIN_ROLE));
    }
}