package org.histo.action;

import java.io.Serializable;

import org.apache.log4j.Logger;
import org.histo.config.enums.Role;
import org.histo.dao.GenericDAO;
import org.histo.dao.HelperDAO;
import org.histo.dao.UserDAO;
import org.histo.model.HistoUser;
import org.histo.model.UserRole;
import org.histo.util.ResourceBundle;
import org.histo.util.UserUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserHandlerAction implements Serializable {

	private static final long serialVersionUID = -8314968695816748306L;

	@Autowired
	private HelperHandlerAction helperHandlerAction;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;
	
	public boolean isUserAvailable() {
		if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof HistoUser)
			return true;
		return false;
	}

	/**
	 * Returns the current user.
	 * 
	 * @return
	 */
	public HistoUser getCurrentUser() {
		HistoUser user = (HistoUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
		return user;
	}

	public boolean currentUserHasRole(Role role) {
		return getCurrentUser().getRole().equals(role);
	}

	public boolean currentUserHasRoleOrHigher(Role role) {
		return getCurrentUser().getRole().getRoleValue() >= role.getRoleValue();
	}
	
	public boolean userHasRole(HistoUser user){
		
	}
	
	public void changeRoleForCurrentUser(String role) {
		changeRoleForUser(getCurrentUser(),Role.getRoleByToken(role));
	}

	public void changeRoleForCurrentUser(Role role) {
		changeRoleForUser(getCurrentUser(),role);
	}
	
	public void changeRoleForUser(HistoUser user, Role role){
		user.setRole(role);
		roleOfuserHasChanged(user);
	}
	
	public void roleOfuserHasChanged(HistoUser histoUser){
		genericDAO.save(histoUser, resourceBundle.get("log.user.role.changed",histoUser.getRole()));
	}
	

	
	public boolean hasRole(String role) {
		// sonderfall wenn nicht eingeloggt
		if (SecurityContextHolder.getContext().getAuthentication().getPrincipal() instanceof String)
			return false;
		else {
			HistoUser user = (HistoUser) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
			return hasRole(user, role);
		}
	}

	public boolean hasRole(HistoUser histoUser, String role) {
		if (histoUser.getRole().equals(role))
			return true;
		return false;
	}

}
