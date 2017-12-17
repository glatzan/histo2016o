package org.histo.action;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.MailHandler;
import org.histo.adaptors.printer.ClinicPrinter;
import org.histo.adaptors.printer.LabelPrinter;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.GenericDAO;
import org.histo.dao.UserDAO;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoPermissions;
import org.histo.model.user.HistoSettings;
import org.histo.model.user.HistoUser;
import org.histo.template.mail.RequestUnlockMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class UserHandlerAction implements Serializable {

	private static final long serialVersionUID = -8314968695816748306L;

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserDAO userDAO;

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
	 * Selected ClinicPrinter to print the document
	 */
	private ClinicPrinter selectedPrinter;

	/**
	 * Selected label pirnter
	 */
	private LabelPrinter selectedLabelPrinter;

	/**
	 * Method called on postconstruct. Initializes all important variables.
	 */
	@PostConstruct
	public void init() {
		updateSelectedPrinters();
	}

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
	public boolean currentUserHasPermission(HistoPermissions... permissions) {
		return userHasPermission(getCurrentUser(), permissions);
	}

	/**
	 * Checks if user has the passed role.
	 * 
	 * @param user
	 * @param role
	 * @return
	 */
	public boolean userHasPermission(HistoUser user, HistoPermissions... role) {
		return user.getGroup().getPermissions().stream().anyMatch(p -> {
			for (int i = 0; i < role.length; i++)
				if (p == role[i])
					return true;
			return false;
		});
	}

	/**
	 * Saves a role change for a given user.
	 * 
	 * @param histoUser
	 * @throws CustomDatabaseInconsistentVersionException
	 */
	public void groupOfUserHasChanged(HistoUser histoUser) throws CustomDatabaseInconsistentVersionException {
		// init histo group
		HistoGroup group = userDAO.initializeGroup(histoUser.getGroup(), true);

		if (histoUser.getSettings() == null)
			histoUser.setSettings(new HistoSettings());

		histoUser.getSettings().updateCrucialSettings(group.getSettings());
		logger.debug("Role of user " + histoUser.getUsername() + " to " + histoUser.getGroup().toString());
		genericDAO.save(histoUser, "log.user.role.changed", new Object[] { histoUser.getGroup() });
	}

	/**
	 * Sends an unlock Request to admins
	 */
	public void requestUnlock() {
		HistoUser currentUser = getCurrentUser();

		RequestUnlockMail mail = MailHandler.getDefaultTemplate(RequestUnlockMail.class);
		mail.prepareTemplate(currentUser);
		mail.fillTemplate();

		globalSettings.getMailHandler().sendAdminMail(mail);

		setUnlockRequestSend(true);
	}

	public void updateSelectedPrinters() {

		if (getCurrentUser().getSettings().getPreferedPrinter() == null) {
			// dummy printer is allways there
			setSelectedPrinter(globalSettings.getPrinterList().get(0));
			getCurrentUser().getSettings().setPreferedPrinter(getSelectedPrinter().getName());
		} else {
			ClinicPrinter printer = globalSettings
					.getPrinterByName(getCurrentUser().getSettings().getPreferedPrinter());
			// if printer was found set it
			if (printer != null) {
				logger.debug("Settings printer " + printer.getName() + " as selected printer");
				setSelectedPrinter(printer);
			} else {
				// TODO search for printer in the same room
				setSelectedPrinter(globalSettings.getPrinterList().get(0));
			}
		}

		if (getCurrentUser().getSettings().getPreferedLabelPritner() == null) {
			setSelectedLabelPrinter(globalSettings.getLabelPrinterList().get(0));
			getCurrentUser().getSettings().setPreferedLabelPritner(Long.toString(getSelectedLabelPrinter().getId()));
		} else {
			LabelPrinter labelPrinter = globalSettings
					.getLabelPrinterByID(getCurrentUser().getSettings().getPreferedLabelPritner());

			if (labelPrinter != null) {
				logger.debug("Settings printer " + labelPrinter.getName() + " as selected printer");
				setSelectedLabelPrinter(labelPrinter);
			} else {
				// TODO serach for pritner in the same room
				setSelectedLabelPrinter(globalSettings.getLabelPrinterList().get(0));
			}
		}
	}

}
