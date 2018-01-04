package org.histo.config;

import java.net.URL;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.View;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoGroup.AuthRole;
import org.histo.model.user.HistoUser;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
public class LoginHandler {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private CustomAuthenticationProvider authenticationManager;

	@Autowired
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Setter(AccessLevel.NONE)
	@Getter(AccessLevel.NONE)
	@Lazy
	private WorklistViewHandlerAction worklistViewHandlerAction;

	private String username;

	private String password;

	public String login() {
		try {

			Authentication authentication = authenticationManager
					.authenticate(new UsernamePasswordAuthenticationToken(this.username, this.password));

			SecurityContextHolder.getContext().setAuthentication(authentication);

			// deleting password
			this.password = null;

			return determineTargetUrl(authentication) + "?faces-redirect=true";
		} catch (BadCredentialsException ex) {

			mainHandlerAction.addQueueGrowlMessage(resourceBundle.get("login.error"), ex.getMessage());

			logger.debug("Login failed");

			// deleting password
			this.password = null;

			// disable the button if something went wrong
			RequestContext.getCurrentInstance().execute("blockForLogin(false)");

			return View.LOGIN.getPath();
		}
	}

	/**
	 * Checking if user should be redirected to a given page, if not the user will
	 * be redirected to the default pages (determined by the group settings)
	 * 
	 * @param authentication
	 * @return
	 */
	protected String determineTargetUrl(Authentication authentication) {
		if ((authentication.getPrincipal() instanceof HistoUser)) {

			// checking if the user should be redirected to an url
			HttpServletRequest request = ((HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext()
					.getRequest());

			SavedRequest savedRequest = new HttpSessionRequestCache().getRequest(request,
					(HttpServletResponse) FacesContext.getCurrentInstance().getExternalContext().getResponse());

			// getting the users default page
			HistoUser user = (HistoUser) authentication.getPrincipal();

			// always one group
			HistoGroup group = user.getAuthorities().get(0);

			if (savedRequest != null) {
				try {

					for (View view : user.getSettings().getAvailableViews()) {

					}
					// TODO check if path is allowed!, reanable redirect

					// worklistViewHandlerAction.initBean();
					// URL url = new URL(savedRequest.getRedirectUrl());
					// return url.getFile().substring(request.getContextPath().length());
				} catch (Exception e) {
					logger.error(e.getMessage() + " Using default URL");
				}
			}

			if (user.getSettings().getStartView() != null) {
				// alway init worklist!
				
				if(user.getSettings().getStartView() != View.GUEST)
					worklistViewHandlerAction.initBean();
				
				return user.getSettings().getStartView().getRootPath();
			} else {
				logger.error("No Start View Found, going back to normal view");
				return View.LOGIN.getPath();
			}
		}

		return View.LOGIN.getPath();
	}
}
