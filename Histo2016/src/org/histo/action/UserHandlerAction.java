package org.histo.action;

import java.io.Serializable;

import org.histo.config.enums.Role;
import org.histo.dao.GenericDAO;
import org.histo.model.HistoUser;
import org.histo.util.ResourceBundle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserHandlerAction implements Serializable {

	private static final long serialVersionUID = -8314968695816748306L;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	/**
	 * Checks if the session is associated with a user.
	 * 
	 * @return
	 */
	public boolean isCurrentUserAvailable() {
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

	/**
	 * Checks if currentUser has the passed role.
	 * 
	 * @param role
	 * @return
	 */
	public boolean currentUserHasRole(Role role) {
		return userHasRole(getCurrentUser(), role);
	}

	/**
	 * Checks if user has the passed role.
	 * 
	 * @param user
	 * @param role
	 * @return
	 */
	public boolean userHasRole(HistoUser user, Role role) {
		return user.getRole().equals(role);
	}

	/**
	 * Checks if the current user has the passed role, or has a role with more
	 * rights.
	 * 
	 * @param role
	 * @return
	 */
	public boolean currentUserHasRoleOrHigher(Role role) {
		return userHasRoleOrHigher(getCurrentUser(), role);
	}

	/**
	 * Check if the user has the passed role, or has a role with more rights.
	 * 
	 * @param user
	 * @param role
	 * @return
	 */
	public boolean userHasRoleOrHigher(HistoUser user, Role role) {
		return user.getRole().getRoleValue() >= role.getRoleValue();
	}

	/**
	 * Changes the role for the current user.
	 * 
	 * @param role
	 */
	public void changeRoleForCurrentUser(String role) {
		changeRoleForUser(getCurrentUser(), Role.getRoleByToken(role));
	}

	/**
	 * Changes the role of the current user.
	 * 
	 * @param role
	 */
	public void changeRoleForCurrentUser(Role role) {
		changeRoleForUser(getCurrentUser(), role);
	}

	/**
	 * Changes the role of the given user.
	 * 
	 * @param user
	 * @param role
	 */
	public void changeRoleForUser(HistoUser user, Role role) {
		user.setRole(role);
		roleOfuserHasChanged(user);
	}

	/**
	 * Saves a role change for a given user.
	 * 
	 * @param histoUser
	 */
	public void roleOfuserHasChanged(HistoUser histoUser) {
		System.out.println("role changes");
		genericDAO.save(histoUser, resourceBundle.get("log.user.role.changed", histoUser.getRole()));
	}
}
