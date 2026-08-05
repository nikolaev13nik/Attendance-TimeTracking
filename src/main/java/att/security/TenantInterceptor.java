package att.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class TenantInterceptor implements HandlerInterceptor {
    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);
    private final String TENANT_ID_PATH = "tenantId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        String pathTenantId = getPathTenantId(request);
        if (pathTenantId == null) {
            return true; // No tenantId in path, skip validation
        }

        String jwtTenantId = getJwtTenantId();
        if (jwtTenantId == null) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Lack of tenant ID in security context");
            return false;
        }

        if (!pathTenantId.equals(jwtTenantId)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Tenant ID mismatch");
            return false;
        }
        log.info("Tenant verification passed for tenantId: {} on URI: {}", pathTenantId, request.getRequestURI());
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

    private String getJwtTenantId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString(TENANT_ID_PATH);
        }
        return null;
    }
}