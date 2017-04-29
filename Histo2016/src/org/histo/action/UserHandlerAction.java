package org.histo.action;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.MailPresetName;
import org.histo.config.enums.Role;
import org.histo.dao.GenericDAO;
import org.histo.model.HistoUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class UserHandlerAction implements Serializable {

	private static final long serialVersionUID = -8314968695816748306L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	@Lazy
	private MainHandlerAction mainHandlerAction;

	/********************************************************
	 * login
	 ********************************************************/
	/**
	 * True if unlock button was clicked
	 */
	private boolean unlockRequestSend;

	/********************************************************
	 * login
	 ********************************************************/

	/********************************************************
	 * user settings dialog
	 ********************************************************/
	public void prepareUserSettingsDialog() {
		mainHandlerAction.showDialog(Dialog.USER_SETTINGS);
	}

	public void saveUserSettings() {
		genericDAO.save(getCurrentUser(),
				resourceBundle.get("log.userSettings.update", getCurrentUser().getUsername()));
		hideUserSettingsDialog();
	}

	public void hideUserSettingsDialog() {
		genericDAO.refresh(getCurrentUser());
		mainHandlerAction.hideDialog(Dialog.USER_SETTINGS);
	}

	/********************************************************
	 * user settings dialog
	 ********************************************************/

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
		logger.debug("Role of user " + histoUser.getUsername() + " to " + histoUser.getRole().toString());
		genericDAO.save(histoUser, resourceBundle.get("log.user.role.changed", histoUser.getRole()));
	}

	/**
	 * Sends an unlock Request to admins
	 */
	public void requestUnlock() {
		HistoUser currentUser = getCurrentUser();

		HashMap<String, String> subject = new HashMap<String, String>();
		subject.put("%name%", currentUser.getPhysician().getPerson().getFullName());

		HashMap<String, String> content = new HashMap<String, String>();
		content.put("%name%", currentUser.getPhysician().getPerson().getFullName());
		content.put("%username%", currentUser.getUsername());
		content.put("%i%", currentUser.getPhysician().getClinicRole());

		// sending mail to inform about unlocking request
		mainHandlerAction.getSettings().getMail().sendTempalteMail(mainHandlerAction.getSettings().getAdminMails(),
				MailPresetName.RequestUnlock, subject, content);

		setUnlockRequestSend(true);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public boolean isUnlockRequestSend() {
		return unlockRequestSend;
	}

	public void setUnlockRequestSend(boolean unlockRequestSend) {
		this.unlockRequestSend = unlockRequestSend;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
