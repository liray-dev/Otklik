package com.normilinet.otklik.configuration;

import com.normilinet.otklik.domain.model.User;
import com.normilinet.otklik.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Set;

@Component
public class ProfileCompletenessInterceptor implements HandlerInterceptor {

    private final ProfileService profileService;

    public ProfileCompletenessInterceptor(ProfileService profileService) {
        this.profileService = profileService;
    }

    private static final Set<String> ALLOWED_PREFIXES = Set.of(
            "/profile",
            "/logout",
            "/login",
            "/register",
            "/css/",
            "/js/",
            "/images/",
            "/files/",
            "/notifications",
            "/error",
            "/favicon"
    );

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal())) {
            return true;
        }
        String path = request.getRequestURI();
        for (String p : ALLOWED_PREFIXES) {
            if (path.startsWith(p)) return true;
        }
        User u = profileService.getByUsername(auth.getName());
        if (!profileService.isProfileComplete(u)) {
            response.sendRedirect("/profile?incomplete");
            return false;
        }
        return true;
    }
}
