package org.histo.action.dialog;

import java.util.Date;
import java.util.List;

import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.adaptors.MailHandler;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.model.transitory.settings.Version;
import org.histo.template.mail.ErrorMail;
import org.histo.util.TimeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Getter
@Setter
public class ProgrammVersionDialog extends AbstractTabDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	private VersionTab versionTab;
	private ErrorTab errorTab;
	private AboutTab aboutTab;

	public ProgrammVersionDialog() {
		setVersionTab(new VersionTab());
		setErrorTab(new ErrorTab());
		setAboutTab(new AboutTab());

		tabs = new AbstractTab[] { versionTab, errorTab, aboutTab };
	}

	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public boolean initBean() {
		return super.initBean(Dialog.INFO);
	}

	@Getter
	@Setter
	public class VersionTab extends AbstractTab {

		private List<Version> versionInfo;

		public VersionTab() {
			setTabName("VersionTab");
			setName("dialog.info.version.headline");
			setViewID("versionTab");
			setCenterInclude("include/version.xhtml");
		}

		public boolean initTab() {
			setVersionInfo(Version.factroy(GlobalSettings.VERSIONS_INFO));
			return true;
		}

	}

	@Getter
	@Setter
	public class ErrorTab extends AbstractTab {

		private String errorMessage;

		private Date errorDate;

		public ErrorTab() {
			setTabName("ErrorTab");
			setName("dialog.info.error.headline");
			setViewID("errorTab");
			setCenterInclude("include/error.xhtml");
		}

		public boolean initTab() {
			setErrorDate(new Date(System.currentTimeMillis()));
			setErrorMessage("");
			return true;
		}

		public void sendErrorMessage() {
			// TODO rewrite if email system is updated
			logger.debug("Sending Error DiagnosisRevision Date + "
					+ TimeUtil.formatDate(errorDate, DateFormat.GERMAN_DATE_TIME.getDateFormat()) + " Message: "
					+ errorMessage);

			if (errorMessage != null && !errorMessage.isEmpty() && errorMessage != null) {

				ErrorMail mail = MailHandler.getDefaultTemplate(ErrorMail.class);
				mail.prepareTemplate(userHandlerAction.getCurrentUser(), errorMessage,
						new Date(System.currentTimeMillis()));
				mail.fillTemplate();

				globalSettings.getMailHandler().sendAdminMail(mail);
				
				initTab();
				
				mainHandlerAction.sendGrowlMessagesAsResource("growl.mail.sendErrorMail", "growl.mail.sendErrorMail.text");
			}
		}

	}

	@Getter
	@Setter
	public class AboutTab extends AbstractTab {

		public AboutTab() {
			setTabName("AboutTab");
			setName("dialog.info.about.headline");
			setViewID("aboutTab");
			setCenterInclude("include/about.xhtml");
		}

	}
}
