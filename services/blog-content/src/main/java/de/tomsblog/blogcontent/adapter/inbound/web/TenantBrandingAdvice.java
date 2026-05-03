package de.tomsblog.blogcontent.adapter.inbound.web;

import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantInfoDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.TenantSettingsDto;
import de.tomsblog.blogcontent.adapter.outbound.usermanagement.UserManagementClient;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/**
 * ControllerAdvice providing tenant branding and tenant list as global model attributes.
 *
 * @req SWR-050
 * @req SWR-053
 */
@ControllerAdvice
public class TenantBrandingAdvice {

    private static final Logger LOG = LoggerFactory.getLogger(TenantBrandingAdvice.class);
    private static final String DEFAULT_TENANT_NAME = "Toms Blog";
    private static final String SETTINGS_ATTR = "TenantBrandingAdvice.settings";

    private final UserManagementClient userManagementClient;

    public TenantBrandingAdvice(UserManagementClient userManagementClient) {
        this.userManagementClient = userManagementClient;
    }

    @ModelAttribute("tenantName")
    public String tenantName(HttpServletRequest request, HttpSession session) {
        TenantSettingsDto settings = resolveSettings(request, session);
        if (settings != null
                && settings.displayName() != null
                && !settings.displayName().isBlank()) {
            return settings.displayName();
        }
        return DEFAULT_TENANT_NAME;
    }

    @ModelAttribute("tenantTagline")
    public String tenantTagline(HttpServletRequest request, HttpSession session) {
        TenantSettingsDto settings = resolveSettings(request, session);
        if (settings != null) {
            return settings.tagline();
        }
        return null;
    }

    @ModelAttribute("tenants")
    public List<TenantInfoDto> tenants(Authentication authentication) {
        if (isSuperAdmin(authentication)) {
            try {
                return userManagementClient.listTenants();
            } catch (Exception e) {
                LOG.debug("Failed to load tenant list.", e);
            }
        }
        return List.of();
    }

    @ModelAttribute("activeTenantId")
    public String activeTenantId(HttpServletRequest request, HttpSession session) {
        return resolveActiveTenantId(request, session).toString();
    }

    @ModelAttribute("isSuperAdmin")
    public boolean isSuperAdmin(Authentication authentication) {
        return authentication != null
                && authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_SUPERADMIN"));
    }

    private TenantSettingsDto resolveSettings(HttpServletRequest request, HttpSession session) {
        Object cached = request.getAttribute(SETTINGS_ATTR);
        if (cached instanceof TenantSettingsDto dto) {
            return dto;
        }
        if (cached != null) {
            // Sentinel value indicating a previous failed lookup
            return null;
        }
        UUID tenantId = resolveActiveTenantId(request, session);
        try {
            TenantSettingsDto settings = userManagementClient.getTenantSettings(tenantId);
            request.setAttribute(SETTINGS_ATTR, settings != null ? settings : Boolean.FALSE);
            return settings;
        } catch (Exception e) {
            LOG.debug("Failed to load tenant settings for '{}'. Using defaults.", tenantId, e);
            request.setAttribute(SETTINGS_ATTR, Boolean.FALSE);
            return null;
        }
    }

    private UUID resolveActiveTenantId(HttpServletRequest request, HttpSession session) {
        Object sessionTenant = session.getAttribute("activeTenantId");
        if (sessionTenant != null) {
            try {
                return UUID.fromString(sessionTenant.toString());
            } catch (IllegalArgumentException e) {
                // Ignore invalid session value
            }
        }
        String header = request.getHeader("X-Tenant-Id");
        if (header != null) {
            try {
                return UUID.fromString(header);
            } catch (IllegalArgumentException e) {
                // Ignore invalid header
            }
        }
        return UUID.fromString("00000000-0000-0000-0000-000000000001");
    }
}
