package org.histo.service;

import java.util.Date;
import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;
import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.util.HistoUtil;
import org.histo.util.notification.FaxExecutor;
import org.histo.util.notification.MailContainer;
import org.histo.util.notification.MailExecutor;
import org.histo.util.notification.NotificationContainer;
import org.histo.util.pdf.PDFGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Service
@Getter
@Setter
public class NotificationService {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	private static Logger logger = Logger.getLogger("org.histo");

	public void startNotificationPhase(Task task) {
		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					task.setNotificationCompletionDate(0);

					genericDAO.savePatientData(task, "log.patient.task.phase.notification.enter");

					if (!task.isListedInFavouriteList(PredefinedFavouriteList.NotificationList)) {
						favouriteListDAO.addTaskToList(task, PredefinedFavouriteList.NotificationList);
					}
				}
			});
		} catch (Exception e) {
			throw new CustomDatabaseInconsistentVersionException(task);
		}
	}

	public void endNotificationPhase(Task task) {
		try {

			transactionTemplate.execute(new TransactionCallbackWithoutResult() {

				public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

					task.setNotificationCompletionDate(System.currentTimeMillis());

					genericDAO.savePatientData(task, "log.patient.task.phase.notification.end");

					favouriteListDAO.removeTaskFromList(task, PredefinedFavouriteList.NotificationList);
				}
			});
		} catch (Exception e) {
			throw new CustomDatabaseInconsistentVersionException(task);
		}
	}

	public void executeMailNotification(Task task, List<NotificationContainer> mails, DiagnosisReportMail mailTemplate,
			TemplateDiagnosisReport defaultReport, boolean individualAddresses) {
		logger.debug("Mail notification is used");
		// pdf container if no individual address is needed

		MailExecutor mailExecutor = new MailExecutor();

		for (NotificationContainer container : mails) {
			try {
				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				logger.debug("Send mail to " + container.getContactAddress());

				if (!mailExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.mail.error.mailNotValid");

				// setting mail
				((MailContainer) container).setMail((DiagnosisReportMail) mailTemplate.clone());

				container.setPdf(
						mailExecutor.getPDF((MailContainer) container, task, defaultReport, individualAddresses));

				if (!mailExecutor.performSend((MailContainer) container))
					throw new IllegalArgumentException("dialog.notification.sendProcess.mail.error.failed");

				mailExecutor.finishSendProecess((MailContainer) container, true,
						resourceBundle.get("dialog.notification.sendProcess.mail.success"));

				logger.debug("Sending completed " + container.getNotification().getCommentary());

			} catch (IllegalArgumentException e) {
				mailExecutor.finishSendProecess((MailContainer) container, true,
						resourceBundle.get(e.getMessage(), container.getContactAddress()));
				logger.debug("Sending failed" + container.getNotification().getCommentary());
			}
		}
	}

	public void executeMailNotification(Task task, List<NotificationContainer> faxes,
			TemplateDiagnosisReport defaultReport, boolean individualAddresses, boolean print) {

		logger.debug("Fax notification is used");

		FaxExecutor faxExecutor = new FaxExecutor();

		for (NotificationContainer container : faxes) {
			try {

				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				if (!faxExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.fax.error.numberNotValid");

				container.setPdf(faxExecutor.getPDF(container, task, defaultReport, individualAddresses));

				if (!faxExecutor.performSend(container))
					throw new IllegalArgumentException("dialog.notification.sendProcess.mail.error.failed");

				// offline mode
				if (globalSettings.getProgramSettings().isOffline()) {
					logger.debug("Offline mode, not sending email!");
					throw new IllegalArgumentException("dialog.notification.sendProcess.fax.error.offline");
				} else if (faxTab.isAutoSendFax()) {
					globalSettings.getFaxHandler().sendFax(container.getContactAddress(), container.getPdf());
					progressStepText("dialog.notification.sendProcess.fax.success");
				} else if (faxTab.isPrint()) {
					progressStepText("dialog.notification.sendProcess.pdf.print");
					userHandlerAction.getSelectedPrinter().print(container.getPdf());
				}

				container.getNotification().setActive(false);
				container.getNotification().setPerformed(true);
				container.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));

				progressStep();
			} catch (IllegalArgumentException e) {
				// no template or no number
				container.getNotification().setPerformed(true);
				container.getNotification().setFailed(true);
				container.getNotification().setActive(true);
				container.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
				container.getNotification().setCommentary(resourceBundle.get(e.getMessage()));
				progressStepText(e.getMessage());
			}
		}
	}
}
