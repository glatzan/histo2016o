package org.histo.action.dialog;

import java.util.Date;

import org.histo.action.UserHandlerAction;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.settings.Version;
import org.histo.template.mail.ErrorMail;
import org.histo.template.mail.RequestUnlockMail;
import org.histo.util.TimeUtil;
import org.histo.util.mail.MailHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class ProgrammVersionDialog extends AbstractDialog {

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private SettingsHandler settingsHandler;

	private Version[] versionInfo;

	private String errorMessage;

	private Date errorDate;

	public void initAndPrepareBean() {
		if (initBean())
			prepareDialog();
	}

	public boolean initBean() {
		super.initBean(null, Dialog.INFO);
		logger.trace("Preparing Info Dialog");
		setVersionInfo(Version.factroy(SettingsHandler.VERSIONS_INFO));
		setErrorDate(new Date(System.currentTimeMillis()));
		setErrorMessage("");
		return true;
	}

	public void sendErrorMessage(Date dateOfError, String errorMessage) {
		// TODO rewrite if email system is updated
		logger.debug("Sending Error DiagnosisRevision Date + "
				+ TimeUtil.formatDate(dateOfError, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " Message: "
				+ errorMessage);

		if (errorMessage != null && !errorMessage.isEmpty() && errorMessage != null) {

			ErrorMail mail = MailHandler.getDefaultTemplate(ErrorMail.class);
			mail.prepareTemplate(userHandlerAction.getCurrentUser(), errorMessage,
					new Date(System.currentTimeMillis()));
			mail.fillTemplate();

			settingsHandler.getMailHandler().sendAdminMail(mail);
		}
	}

	// ************************ Getter/Setter ************************
	public Version[] getVersionInfo() {
		return versionInfo;
	}

	public void setVersionInfo(Version[] versionInfo) {
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

}
