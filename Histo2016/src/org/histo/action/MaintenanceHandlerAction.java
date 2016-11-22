package org.histo.action;

import java.util.Date;

import org.apache.log4j.Logger;
import org.histo.config.HistoSettings;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.model.transitory.json.ProgramVersion;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MaintenanceHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private UserHandlerAction userHandlerAction;

	private ProgramVersion[] versionInfo;

	private String errorMessage;

	private Date errorDate;

	public void prepareInfoDialog() {
		logger.trace("Preparing Info Dialog");
		setVersionInfo(ProgramVersion.factroy(HistoSettings.VERSION_JSON));
		setErrorDate(new Date(System.currentTimeMillis()));
		setErrorMessage("");

		mainHandlerAction.showDialog(Dialog.INFO);
	}

	/**
	 * Sends error mail to admins
	 * 
	 * @param dateOfError
	 * @param errorMessage
	 */
	public void sendErrorMessage(Date dateOfError, String errorMessage) {
		logger.debug("Sending Error Report Date + "
				+ TimeUtil.formatDate(dateOfError, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " Message: "
				+ errorMessage);

		if (errorMessage != null && !errorMessage.isEmpty() && errorMessage != null) {
			errorMessage = errorMessage + "\r\n\r\nAbsender: "
					+ userHandlerAction.getCurrentUser().getPhysician().getPerson().getFullName();

			mainHandlerAction.getSettings().getMail().sendMail(mainHandlerAction.getSettings().getErrorMails(),
					"Fehlermeldung vom "
							+ TimeUtil.formatDate(dateOfError, DateFormat.GERMAN_DATE_TIME.getDateFormat()),
					errorMessage);
		}
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public ProgramVersion[] getVersionInfo() {
		return versionInfo;
	}

	public void setVersionInfo(ProgramVersion[] versionInfo) {
		this.versionInfo = versionInfo;
	}

	public String getErrorMessage() {
		return errorMessage;
	}

	public void setErrorMessage(String errorMessage) {
		this.errorMessage = errorMessage;
	}

	public Date getErrorDate() {
		return errorDate;
	}

	public void setErrorDate(Date errorDate) {
		this.errorDate = errorDate;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}
