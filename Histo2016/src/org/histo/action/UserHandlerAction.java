package org.histo.action;

import java.io.Serializable;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Role;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.model.HistoUser;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.template.mail.RequestUnlockMail;
import org.histo.util.mail.MailHandler;
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
	private SettingsHandler settingsHandler;

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
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void changeRoleForCurrentUser(String role) throws CustomDatabaseInconsistentVersionException {
		changeRoleForUser(getCurrentUser(), Role.getRoleByToken(role));
	}

	/**
	 * Changes the role of the current user.
	 * 
	 * @param role
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void changeRoleForCurrentUser(Role role) throws CustomDatabaseInconsistentVersionException {
		changeRoleForUser(getCurrentUser(), role);
	}

	/**
	 * Changes the role of the given user.
	 * 
	 * @param user
	 * @param role
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void changeRoleForUser(HistoUser user, Role role) throws CustomDatabaseInconsistentVersionException {
		user.setRole(role);
		roleOfuserHasChanged(user);
	}

	/**
	 * Saves a role change for a given user.
	 * 
	 * @param histoUser
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void roleOfuserHasChanged(HistoUser histoUser) throws CustomDatabaseInconsistentVersionException {
		PredefinedRoleSettings roleSetting = settingsHandler.getRoleSettingsForRole(histoUser.getRole());
		histoUser.updateUserSettings(roleSetting);
		logger.debug("Role of user " + histoUser.getUsername() + " to " + histoUser.getRole().toString());
		genericDAO.saveDataRollbackSave(histoUser, "log.user.role.changed", new Object[] { histoUser.getRole() });
	}

	/**
	 * Sends an unlock Request to admins
	 */
	public void requestUnlock() {
		HistoUser currentUser = getCurrentUser();

		RequestUnlockMail mail = MailHandler.getDefaultTemplate(RequestUnlockMail.class);
		mail.prepareTemplate(currentUser);
		mail.fillTemplate();
		
		settingsHandler.getMailHandler().sendAdminMail(mail);

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
