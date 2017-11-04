 package org.histo.config;

import java.io.IOException;
import java.util.Collection;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.histo.config.enums.View;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoGroup.AuthRole;
import org.histo.model.user.HistoUser;
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
	public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
			Authentication authentication) throws IOException {
		handle(request, response, authentication);
		clearAuthenticationAttributes(request);
	}

	protected void handle(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
			throws IOException {
		String targetUrl = determineTargetUrl(authentication);

		if (response.isCommitted()) {
			System.out.println("Response has already been committed. Unable to redirect to " + targetUrl);
			return;
		}

		redirectStrategy.sendRedirect(request, response, targetUrl);
	}

	/**
	 * Returns the base URL according to the users role
	 */
	protected String determineTargetUrl(Authentication authentication) {
		if ((authentication.getPrincipal() instanceof HistoUser)) {

			HistoUser user = (HistoUser) authentication.getPrincipal();

			// always one group
			HistoGroup group = user.getAuthorities().get(0);

			if (group.getAuthRole() == AuthRole.ROLE_NONEAUTH)
				// no role, should never happen
				return View.LOGIN.getPath();
			else 
				return user.getSettings().getDefaultView().getRootPath();
			
//			if (userRole == Role.GUEST)
//				// guest need to be unlocked first
//				return View.GUEST.getPath();
//			else if(userRole == Role.USER)
//				return View.USERLIST.getPath();
//			else if (userRole == Role.SCIENTIST)
//				// no names are displayed
//				return View.SCIENTIST.getPath();
//			else if (userRole.getRoleValue() >= Role.MTA.getRoleValue()) {
//				// normal work environment
//				return View.WORKLIST.getPath();
//			} else
		}

		return View.LOGIN.getPath();
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
