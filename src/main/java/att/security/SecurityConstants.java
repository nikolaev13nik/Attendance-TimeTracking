package att.security;

public class SecurityConstants {


    public static final String AUTHORITIES = "authorities";
    public static final String TENANT_ID = "tenantId";
    public static final String ADMINISTRATOR_AUTHORITY = "ROLE_" + SecurityRoles.ADMINISTRATOR.name();
    public static final String PLATFORM_ADMIN_AUTHORITY = "ROLE_" + SecurityRoles.PLATFORM_ADMIN.name();
    public static final Integer PLATFORM_TENANT_ID = 0;

    public enum SecurityRoles {
        USER,
        ADMINISTRATOR,
        PLATFORM_ADMIN
    }


}
