package att.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static att.security.SecurityConstants.TENANT_ID;

@Component
public class TenantInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger(TenantInterceptor.class);

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response,
                             Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod)) {
            return true;
        }
        String pathTenantId = getPathTenantId(request);
        if (pathTenantId == null) {
            log.error("Request was rejected: no tenant ID in path {}", request.getRequestURI());
            response.sendError(HttpServletResponse.SC_FORBIDDEN, "Request was rejected: no tenant ID in path");
            return false;
        }

        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (JwtClaimsResolver.isPlatformAdmin(auth)) {
            log.info("Platform admin {} acting on tenant {}: {} {}", auth.getName(), pathTenantId,
                    request.getMethod(), request.getRequestURI());
            return true;
        }

        String jwtTenantId = JwtClaimsResolver.currentTenantId(auth);
        if (jwtTenantId == null) {
            log.error("Request was rejected: lack of tenant ID in security context");
            response.sendError(HttpServletResponse.SC_FORBIDDEN,
                    "Request was rejected: lack of tenant ID in security context");
            return false;
        }

        if (!pathTenantId.equals(jwtTenantId)) {
            log.error("Request was rejected: JWT's tenant does not match to path");
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
            Object tenantId = pathVars.get(TENANT_ID);
            return tenantId != null ? tenantId.toString() : null;
        }
        return null;
    }
}
