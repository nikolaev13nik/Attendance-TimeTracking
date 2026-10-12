package att.security;

public class SecurityConstants {


    public static final String AUTHORITIES = "authorities";
    public static final String TENANT_ID = "tenantId";
    public static final String ADMINISTRATOR_AUTHORITY = "ROLE_" + SecurityRoles.ADMINISTRATOR.name();

    public enum SecurityRoles {
        USER,
        MODERATOR,
        ADMINISTRATOR
    }


}
