package org.histo.config;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.histo.action.WorklistHandlerAction;
import org.histo.model.UserRole;
import org.histo.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.DefaultRedirectStrategy;
import org.springframework.security.web.RedirectStrategy;
import org.springframework.security.web.WebAttributes;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
public class CustomLoginSuccessHandler implements AuthenticationSuccessHandler {

    private RedirectStrategy redirectStrategy = new DefaultRedirectStrategy();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
	handle(request, response, authentication);
	clearAuthenticationAttributes(request);
    }

    protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {
	String targetUrl = determineTargetUrl(authentication);

	if (response.isCommitted()) {
	    System.out.println("Response has already been committed. Unable to redirect to " + targetUrl);
	    return;
	}

	redirectStrategy.sendRedirect(request, response, targetUrl);
    }

    /** Builds the target URL according to the logic defined in the main class Javadoc. */
    protected String determineTargetUrl(Authentication authentication) {
        System.out.println("working");
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();

        if(UserUtil.accessByUserLevel((Collection<UserRole>) authorities,UserRole.ROLE_LEVEL_USER))
            return "/pages/worklist/workList.xhtml";
        else
            return "/pages/index.xhtml";
        
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request) {
	HttpSession session = request.getSession(false);
	if (session == null) {
	    return;
	}
	session.removeAttribute(WebAttributes.AUTHENTICATION_EXCEPTION);
    }

    public void setRedirectStrategy(RedirectStrategy redirectStrategy) {
	this.redirectStrategy = redirectStrategy;
    }

    protected RedirectStrategy getRedirectStrategy() {
	return redirectStrategy;
    }
}
