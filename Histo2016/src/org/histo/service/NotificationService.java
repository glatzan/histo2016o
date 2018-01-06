package org.histo.service;

import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.handler.GlobalSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.PdfDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.util.notification.FaxExecutor;
import org.histo.util.notification.MailContainer;
import org.histo.util.notification.MailExecutor;
import org.histo.util.notification.NotificationContainer;
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

	public boolean executeNotification(NotificationFeedback feedback, Task task, boolean useMail,
			List<NotificationContainer> mailContainer, TemplateDiagnosisReport defaultMailReport,
			DiagnosisReportMail defaultMailTemplate, boolean mailIndividualAddresses, boolean useFax,
			List<NotificationContainer> faxContainer, TemplateDiagnosisReport defaultFaxReport,
			boolean faxIndividualAddresses, boolean faxSend, boolean faxPrint, boolean useLetter,
			List<NotificationContainer> letterContainer, TemplateDiagnosisReport defaultLetterReport,
			boolean letterIndividualAddresses, boolean letterPrint, boolean usePhone,
			List<NotificationContainer> phoneContainer) {

		boolean emailSendSuccessful = useMail
				? executeMailNotification(feedback, task, mailContainer, defaultMailReport, defaultMailTemplate,
						mailIndividualAddresses)
				: true;

		boolean faxSendSuccessful = useFax
				? executeFaxNotification(feedback, task, faxContainer, defaultFaxReport, faxIndividualAddresses,
						faxSend, faxPrint)
				: true;

		boolean letterSendSuccessful = useLetter
				? executeLetterNotification(feedback, task, letterContainer, defaultLetterReport,
						letterIndividualAddresses, letterIndividualAddresses)
				: true;

		executePhoneNotification(feedback, task, phoneContainer);

		PDFContainer sendReport = generateSendReport(task, useMail, mailContainer, useFax, faxContainer, useLetter,
				letterContainer, usePhone, phoneContainer, new Date());

		genericDAO.savePatientData(task, "log.patient.task.notification.send");

		pdfDAO.attachPDF(task.getPatient(), task, sendReport);

		return emailSendSuccessful && faxSendSuccessful && letterSendSuccessful;
	}

	public boolean executeMailNotification(NotificationFeedback feedback, Task task,
			List<NotificationContainer> mailContainer, TemplateDiagnosisReport defaultReport,
			DiagnosisReportMail mailTemplate, boolean individualAddresses) {
		logger.debug("Mail notification is used");
		// pdf container if no individual address is needed successful

		boolean success = true;

		MailExecutor mailExecutor = new MailExecutor(feedback);

		for (NotificationContainer container : mailContainer) {
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
			feedback.progressStep();
		}

		return success;
	}

	public boolean executeFaxNotification(NotificationFeedback feedback, Task task, List<NotificationContainer> faxes,
			TemplateDiagnosisReport defaultReport, boolean individualAddresses, boolean send, boolean print) {

		logger.debug("Fax notification is used");

		FaxExecutor faxExecutor = new FaxExecutor(feedback);

		boolean success = true;

		for (NotificationContainer container : faxes) {
			try {

				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				if (!faxExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("dialog.notification.sendProcess.fax.error.numberNotValid");

				container.setPdf(faxExecutor.getPDF(container, task, defaultReport, individualAddresses));

				if (!faxExecutor.performNotification(container, send, print))
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
			List<NotificationContainer> letters, TemplateDiagnosisReport defaultReport, boolean individualAddresses,
			boolean print) {

		logger.debug("Fax notification is used");

		NotificationExecutor<NotificationContainer> notificationExecutor = new NotificationExecutor<NotificationContainer>(
				feedback);

		boolean success = true;

		for (NotificationContainer container : letters) {
			try {

				// copy contact address before sending -> save before error
				container.getNotification().setContactAddress(container.getContactAddress());

				if (!notificationExecutor.isAddressApproved(container.getContactAddress()))
					throw new IllegalArgumentException("");

				container.setPdf(notificationExecutor.getPDF(container, task, defaultReport, individualAddresses));

				if (!notificationExecutor.performNotification(container, false, print))
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

	public void executePhoneNotification(NotificationFeedback feedback, Task task, List<NotificationContainer> phones) {
		NotificationExecutor<NotificationContainer> notificationExecutor = new NotificationExecutor<NotificationContainer>(
				feedback);
		for (NotificationContainer container : phones) {
			notificationExecutor.finishSendProecess(container, true,
					resourceBundle.get("dialog.notification.sendProcess.pdf.print"));
		}
		feedback.progressStep();
	}

	public PDFContainer generateSendReport(Task task, boolean mailUsed, List<NotificationContainer> mailContainer,
			boolean faxUsed, List<NotificationContainer> faxContainer, boolean letterUserd,
			List<NotificationContainer> letterContainer, boolean phoneUsed, List<NotificationContainer> phoneContainer,
			Date notificationDate) {
		DocumentTemplate sendReport = DocumentTemplate
				.getTemplateByID(globalSettings.getDefaultDocuments().getNotificationSendReport());

		sendReport.initializeTempalte(task, new Boolean(mailUsed), new Boolean(faxUsed), new Boolean(letterUserd),
				new Boolean(phoneUsed), mailContainer, faxContainer, letterContainer, phoneContainer, notificationDate);
		PDFContainer container = (new PDFGenerator()).getPDF(sendReport);

		return container;
	}

	public void saveContactsOfTask(Task task, PDFContainer sendReport) {

		// // removing from diagnosis list
		// favouriteListDAO.removeTaskFromList(getTask(),
		// PredefinedFavouriteList.NotificationList,
		// PredefinedFavouriteList.StayInNotificationList);
	}
}
