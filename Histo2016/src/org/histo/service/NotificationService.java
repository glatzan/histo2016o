package org.histo.service;

import java.util.Date;

import org.apache.log4j.Logger;
import org.histo.action.UserHandlerAction;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.PdfDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.DiagnosisReport;
import org.histo.template.documents.SendReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.util.notification.FaxExecutor;
import org.histo.util.notification.MailContainer;
import org.histo.util.notification.MailContainerList;
import org.histo.util.notification.MailExecutor;
import org.histo.util.notification.NotificationContainer;
import org.histo.util.notification.NotificationContainerList;
import org.histo.util.notification.NotificationExecutor;
import org.histo.util.notification.NotificationFeedback;
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PdfDAO pdfDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

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

	public boolean executeNotification(NotificationFeedback feedback, Task task, MailContainerList mailContainerList,
			NotificationContainerList faxContainerList, NotificationContainerList letterContainerList,
			NotificationContainerList phoneContainerList, NotificationContainerList printContainerList,
			boolean temporaryNotification) {

		boolean emailSendSuccessful = executeMailNotification(feedback, task, mailContainerList, temporaryNotification);
		boolean faxSendSuccessful = true;
		boolean letterSendSuccessful = true;

		if (!temporaryNotification) {
			faxSendSuccessful = executeFaxNotification(feedback, task, faxContainerList);
			letterSendSuccessful = executeLetterNotification(feedback, task, letterContainerList);
		}

		if (printContainerList.isUse() && printContainerList.getDefaultReport() != null) {
			// addition templates
			((DiagnosisReport) printContainerList.getDefaultReport()).initData(task, "");
			PDFContainer report = (new PDFGenerator())
					.getPDF(((DiagnosisReport) printContainerList.getDefaultReport()));

			userHandlerAction.getSelectedPrinter().print(report, printContainerList.getPrintCount(),
					printContainerList.getDefaultReport().getAttributes());

		}

		feedback.progressStep();

		PDFContainer sendReport = generateSendReport(feedback, task, mailContainerList, faxContainerList,
				letterContainerList, phoneContainerList, new Date(), temporaryNotification);

		genericDAO.savePatientData(task, "log.patient.task.notification.send");

		pdfDAO.attachPDF(task.getPatient(), task, sendReport);

		return emailSendSuccessful && faxSendSuccessful && letterSendSuccessful;
	}

	public boolean executeMailNotification(NotificationFeedback feedback, Task task,
			MailContainerList mailContainerList, boolean temporaryNotification) {
		// pdf container if no individual address is needed successful

		boolean success = true;

		MailExecutor mailExecutor = new MailExecutor(feedback);

		for (NotificationContainer container : mailContainerList.getContainerToNotify()) {
			try {
				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				logger.debug("Send mail to " + container.getContactAddress());

				if (!mailExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.mail.error.mailNotValid");

				// setting mail
				((MailContainer) container).setMail((DiagnosisReportMail) mailContainerList.getSelectedMail().clone());

				container.setPdf(
						mailExecutor.getPDF((MailContainer) container, task, mailContainerList.getDefaultReport(),
								mailContainerList.getSelectedRevisions(), mailContainerList.isIndividualAddresses()));

				if (!mailExecutor.performNotification((MailContainer) container, true, false))
					throw new IllegalArgumentException("dialog.notification.sendProcess.mail.error.failed");

				mailExecutor.finishSendProecess((MailContainer) container, true,
						resourceBundle.get("dialog.notification.sendProcess.mail.success"));

				logger.debug("Sending completed " + container.getNotification().getCommentary());

			} catch (IllegalArgumentException e) {
				success = false;
				mailExecutor.finishSendProecess((MailContainer) container, false,
						resourceBundle.get(e.getMessage(), container.getContactAddress()));
				logger.debug("Sending failed" + container.getNotification().getCommentary());
			}

			// renew if temporary notification
			if (temporaryNotification)
				contactDAO.renewNotification(task, container.getContact(), container.getNotification());

			feedback.progressStep();
		}

		return success;
	}

	public boolean executeFaxNotification(NotificationFeedback feedback, Task task,
			NotificationContainerList faxContainerList) {

		FaxExecutor faxExecutor = new FaxExecutor(feedback);

		boolean success = true;

		for (NotificationContainer container : faxContainerList.getContainerToNotify()) {
			try {

				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				if (!faxExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.fax.error.numberNotValid");

				container.setPdf(faxExecutor.getPDF(container, task, faxContainerList.getDefaultReport(),
						faxContainerList.getSelectedRevisions(), faxContainerList.isIndividualAddresses()));

				if (!faxExecutor.performNotification(container, faxContainerList.isSend(), faxContainerList.isPrint()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.fax.error.failed");

				faxExecutor.finishSendProecess(container, true,
						resourceBundle.get("dialog.notification.sendProcess.fax.success"));

			} catch (IllegalArgumentException e) {
				success = false;
				faxExecutor.finishSendProecess(container, false,
						resourceBundle.get(e.getMessage(), container.getContactAddress()));
				logger.debug("Sending failed" + container.getNotification().getCommentary());
			}
			feedback.progressStep();
		}

		return success;
	}

	public boolean executeLetterNotification(NotificationFeedback feedback, Task task,
			NotificationContainerList letterContainerList) {

		NotificationExecutor<NotificationContainer> notificationExecutor = new NotificationExecutor<NotificationContainer>(
				feedback);

		boolean success = true;

		for (NotificationContainer container : letterContainerList.getContainerToNotify()) {
			try {

				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				if (!notificationExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("");

				container.setPdf(notificationExecutor.getPDF(container, task, letterContainerList.getDefaultReport(),
						letterContainerList.getSelectedRevisions(), letterContainerList.isIndividualAddresses()));

				if (!notificationExecutor.performNotification(container, false, letterContainerList.isPrint()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.pdf.error.failed");

				notificationExecutor.finishSendProecess(container, true,
						resourceBundle.get("dialog.notification.sendProcess.pdf.print"));

			} catch (IllegalArgumentException e) {
				success = false;
				notificationExecutor.finishSendProecess(container, false,
						resourceBundle.get(e.getMessage(), container.getContactAddress()));
				logger.debug("Sending failed" + container.getNotification().getCommentary());
			}
			feedback.progressStep();
		}

		return success;
	}

	public void executePhoneNotification(NotificationFeedback feedback, Task task,
			NotificationContainerList phoneContainerList) {
		NotificationExecutor<NotificationContainer> notificationExecutor = new NotificationExecutor<NotificationContainer>(
				feedback);
		for (NotificationContainer container : phoneContainerList.getContainerToNotify()) {
			notificationExecutor.finishSendProecess(container, true,
					resourceBundle.get("dialog.notification.sendProcess.pdf.print"));
		}
		feedback.progressStep();
	}

	public PDFContainer generateSendReport(NotificationFeedback feedback, Task task,
			MailContainerList mailContainerList, NotificationContainerList faxContainerList,
			NotificationContainerList letterContainerList, NotificationContainerList phoneContaienrList,
			Date notificationDate, boolean temporarayNotification) {

		feedback.setFeedback("log.notification.pdf.sendReport.generation");

		SendReport sendReport = DocumentTemplate
				.getTemplateByID(globalSettings.getDefaultDocuments().getNotificationSendReport());

		sendReport.initializeTempalte(task, mailContainerList, faxContainerList, letterContainerList,
				phoneContaienrList, notificationDate, temporarayNotification);

		PDFContainer container = (new PDFGenerator()).getPDF(sendReport);

		return container;
	}
}
