package org.histo.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.MailType;
import org.histo.config.enums.NotificationOption;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.mail.MailTemplate;
import org.histo.model.transitory.json.printing.PrintTemplate;
import org.histo.ui.medicalFindings.EmailNotificationSettings;
import org.histo.ui.medicalFindings.FaxNotificationSettings;
import org.histo.ui.medicalFindings.MedicalFindingsChooser;
import org.histo.ui.medicalFindings.PhoneNotificationSettings;
import org.histo.util.HistoUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class MedicalFindingsHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private PrintHandlerAction printHandlerAction;

	@Autowired
	private MediaHandlerAction mediaHandlerAction;
	/**
	 * class for creating pdfs
	 */
	@Autowired
	private PDFGeneratorHandler pDFGeneratorHandler;
	/********************************************************
	 * General
	 ********************************************************/
	/**
	 * Task to perfome notification
	 */
	private Task temporaryTask;

	/**
	 * Tabindex of settings dialog
	 */
	private int activeTabIndex = 0;

	/********************************************************
	 * General
	 ********************************************************/

	/********************************************************
	 * Settings
	 ********************************************************/
	/**
	 * Email settings
	 */
	private EmailNotificationSettings emailNotificationSettings;

	/**
	 * Fax settings
	 */
	private FaxNotificationSettings faxNotificationSettings;

	/**
	 * Phone settings
	 */
	private PhoneNotificationSettings phoneNotificationSettings;
	/********************************************************
	 * Settings
	 ********************************************************/

	/********************************************************
	 * Notification
	 ********************************************************/
	/**
	 * True if preview should be displayed
	 */
	private boolean showPreview;

	/**
	 * True if the notification is running at the moment
	 */
	private AtomicBoolean notificationRunning = new AtomicBoolean(false);

	/**
	 * True if the notification is perfomed.
	 */
	private AtomicBoolean notificationPerformed = new AtomicBoolean(false);

	/********************************************************
	 * Notification
	 ********************************************************/
	public void prepareMedicalFindingsDialog(Task task) {
		logger.trace("Called prepareMedicalFindingsDialog(Task task)");

		setTemporaryTask(task);

		patientDao.initializeDataList(task);

		setActiveTabIndex(0);
		
		setEmailNotificationSettings(new EmailNotificationSettings(task));
		setFaxNotificationSettings(new FaxNotificationSettings(task));
		setPhoneNotificationSettings(new PhoneNotificationSettings(task));

		MailTemplate taskReport = MailTemplate.factroy(MailType.MedicalFindingsReport);

		emailNotificationSettings.setEmailSubject(taskReport.getSubject());
		emailNotificationSettings.setEmailText(taskReport.getContent());

		setShowPreview(false);

		mainHandlerAction.showDialog(Dialog.MEDICAL_FINDINGS);
	}

	public void hideMedicalFindingsDialog() {
		mainHandlerAction.hideDialog(Dialog.MEDICAL_FINDINGS);
	}

	public void nextStep() {
		logger.trace("Next step");
		if (getActiveTabIndex() < 2)
			setActiveTabIndex(getActiveTabIndex() + 1);
	}

	public void previousStep() {
		logger.trace("Next step");
		if (getActiveTabIndex() > 0)
			setActiveTabIndex(getActiveTabIndex() - 1);
	}
	
	
	public void reSendMedicialFindings(){
		logger.trace("Resend medical findings");
		notificationPerformed.set(false);
		setActiveTabIndex(0);
	}

	public void finalizeTask() {
		logger.trace("Finalize Task");
		
		getTemporaryTask().setFinalized(true);
		hideMedicalFindingsDialog();
	}

	@Async("taskExecutor")
	public void performeNotification() {

		if (notificationRunning.get())
			return;

		notificationRunning.set(true);

		logger.trace("Startin notification thread");

		// patientDao.initializeDataList(getTmpTask());
		// taskDAO.initializeDiagnosisData(getTmpTask());

		StringBuilder sendLog = new StringBuilder();

		if (emailNotificationSettings.isUseEmail() || faxNotificationSettings.isUseFax()
				|| phoneNotificationSettings.isUsePhone()) {
			ArrayList<PDFContainer> resultPdfs = new ArrayList<PDFContainer>();

			// EMAIL
			sendLog.append(resourceBundle.get("pdf.notification.emailNotification.notify"));
			if (emailNotificationSettings.isUseEmail()) {
				logger.trace("Email notification");

				for (MedicalFindingsChooser notificationChooser : emailNotificationSettings
						.getNotificationEmailList()) {
					boolean emailSuccessful = false;

					// name and mail
					logger.trace("Email to " + notificationChooser.getContact().getPerson().getFullName() + " ("
							+ notificationChooser.getContact().getPerson().getEmail() + ")");
					sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.to",
							notificationChooser.getContact().getPerson().getFullName(),
							notificationChooser.getContact().getPerson().getEmail()));

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						logger.trace("No notification desired");
						sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.none"));
						continue;
					} else if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
							&& notificationChooser.getPrintTemplate() != null) {
						// attach pdf to mail
						PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(getTemporaryTask().getPatient(),
								getTemporaryTask(), notificationChooser.getPrintTemplate(),
								notificationChooser.getContact().getPerson());

						if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
								notificationChooser.getContact().getPerson().getEmail(),
								emailNotificationSettings.getEmailSubject(), emailNotificationSettings.getEmailText(),
								pdfToSend)) {
							emailSuccessful = true;
							// adding mail to the result array
							resultPdfs.add(pdfToSend);
							logger.trace("PDF successfully send");
							sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.success.pdf"));
						} else {
							sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.error.pdf"));
							// TODO: HAndle fault
						}

					} else {
						// plain text mail
						if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
								notificationChooser.getContact().getPerson().getEmail(),
								emailNotificationSettings.getEmailSubject(),
								emailNotificationSettings.getEmailText())) {
							emailSuccessful = true;
							logger.trace("Text successfully send");
							sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.success.text"));
						} else {
							// TODO: Handle fault
							sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.error.text"));
						}

					}

					// check if mail was send
					if (emailSuccessful) {

						// setting the contact to perfomed
						notificationChooser.getContact().setNotificationPerformed(true);
						notificationChooser.setPerformed(true);

						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());

					}
				}

				sendLog.append(resourceBundle.get("pdf.notification.spacer"));

			} else {
				logger.trace("No Email notification");
				sendLog.append(resourceBundle.get("pdf.notification.emailNotification.email.noNotification"));
				sendLog.append(resourceBundle.get("pdf.notification.spacer"));
			}

			// FAX
			sendLog.append(resourceBundle.get("pdf.notification.faxNotification.notify"));
			if (faxNotificationSettings.isUseFax()) {
				logger.trace("Fax notification");

				for (MedicalFindingsChooser notificationChooser : faxNotificationSettings.getNotificationFaxList()) {
					boolean faxSuccessful = false;

					// name and number
					logger.trace("Fax to " + notificationChooser.getContact().getPerson().getFullName() + " ("
							+ notificationChooser.getContact().getPerson().getEmail() + ")");
					sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.to",
							notificationChooser.getContact().getPerson().getFullName(),
							notificationChooser.getContact().getPerson().getFax()));

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						logger.trace("No notification desired");
						sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.none"));
						continue;
					} else if (notificationChooser.getPrintTemplate() == null
							|| notificationChooser.getContact().getPerson().getFax() == null
							|| notificationChooser.getContact().getPerson().getFax().isEmpty()) {
						// error no templat or number
						logger.trace("Error, no Fax-Number or TemplateUtil");
						sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.notPossible"));
					} else {
						// creating pdf
						PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(getTemporaryTask().getPatient(),
								getTemporaryTask(), notificationChooser.getPrintTemplate(),
								notificationChooser.getContact().getPerson());
						resultPdfs.add(pdfToSend);

						// TODO: SEND FAX

						logger.trace("Fax send successfully");
						sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.success"));

						// sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.error"));
					}

					if (faxSuccessful) {
						notificationChooser.getContact().setNotificationPerformed(true);
						notificationChooser.setPerformed(true);

						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());
					}
				}

				sendLog.append(resourceBundle.get("pdf.notification.spacer"));
			} else {
				logger.trace("No Fax notification");
				sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.noNotification"));
				sendLog.append(resourceBundle.get("pdf.notification.spacer"));
			}

			// PHONE
			sendLog.append(resourceBundle.get("pdf.notification.phoneNotification.notify"));
			if (phoneNotificationSettings.isUsePhone()) {
				logger.trace("Phone notification");

				for (MedicalFindingsChooser notificationChooser : phoneNotificationSettings
						.getNotificationPhoneList()) {

					// name and number
					logger.trace("Phone to " + notificationChooser.getContact().getPerson().getFullName() + " ("
							+ notificationChooser.getContact().getPerson().getPhoneNumber() + ")");
					sendLog.append(resourceBundle.get("pdf.notification.phoneNotification.phone.to",
							notificationChooser.getContact().getPerson().getFullName(),
							notificationChooser.getContact().getPerson().getPhoneNumber()));

					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						continue;
					} else {
						notificationChooser.getContact().setNotificationPerformed(true);
						notificationChooser.setPerformed(true);
						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.telefon.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());
					}

				}
				sendLog.append(resourceBundle.get("pdf.notification.spacer"));
			} else {
				logger.trace("No phone notification");
				sendLog.append(resourceBundle.get("pdf.notification.phoneNotification.phone.noNotification"));
			}

			// getting the template for the send report
			PrintTemplate sendReport = PrintTemplate
					.getDefaultTemplate(PrintTemplate.getTemplatesByType(DocumentType.MEDICAL_FINDINGS_SEND_REPORT));

			// sendreport has date and datafiled
			HashMap<String, String> addtionalFields = new HashMap<String, String>();
			addtionalFields.put("reportDate", mainHandlerAction.date(System.currentTimeMillis()));
			addtionalFields.put("reportData", sendLog.toString());

			PDFContainer sendReportPDF = pDFGeneratorHandler.generateSimplePDF(getTemporaryTask().getPatient(), sendReport,
					addtionalFields);

			resultPdfs.add(0, sendReportPDF);

			PDFContainer resultPdf = PDFGeneratorHandler.mergePdfs(resultPdfs, sendReportPDF.getName(),
					DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

			printHandlerAction.saveGeneratedPdf(getTemporaryTask(), resultPdf);

			getTemporaryTask().setNotificationPhase(false);
			getTemporaryTask().setNotificationCompletionDate(System.currentTimeMillis());

			mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.update");

			mediaHandlerAction.perpareBeanForExternalForSinglView(resultPdf);

			notificationRunning.set(false);
			notificationPerformed.set(true);
		}
	}

	/********************************************************
	 * Preview
	 ********************************************************/
	public void showPreviewForContact(MedicalFindingsChooser notificationEmailList) {
		printHandlerAction.perpareBeanForExternalView(getTemporaryTask(),
				new PrintTemplate[] { notificationEmailList.getPrintTemplate() },
				notificationEmailList.getPrintTemplate(), notificationEmailList.getContact());
		setShowPreview(true);
	}

	public void hidePreviewForContact() {
		setShowPreview(false);
	}

	/********************************************************
	 * Preview
	 ********************************************************/

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	public Task getTemporaryTask() {
		return temporaryTask;
	}

	public int getActiveTabIndex() {
		return activeTabIndex;
	}

	public void setTemporaryTask(Task temporaryTask) {
		this.temporaryTask = temporaryTask;
	}

	public void setActiveTabIndex(int activeTabIndex) {
		this.activeTabIndex = activeTabIndex;
	}

	public EmailNotificationSettings getEmailNotificationSettings() {
		return emailNotificationSettings;
	}

	public void setEmailNotificationSettings(EmailNotificationSettings emailNotificationSettings) {
		this.emailNotificationSettings = emailNotificationSettings;
	}

	public FaxNotificationSettings getFaxNotificationSettings() {
		return faxNotificationSettings;
	}

	public void setFaxNotificationSettings(FaxNotificationSettings faxNotificationSettings) {
		this.faxNotificationSettings = faxNotificationSettings;
	}

	public PhoneNotificationSettings getPhoneNotificationSettings() {
		return phoneNotificationSettings;
	}

	public void setPhoneNotificationSettings(PhoneNotificationSettings phoneNotificationSettings) {
		this.phoneNotificationSettings = phoneNotificationSettings;
	}

	public boolean getNotificationPerformed() {
		return notificationPerformed.get();
	}

	public void setNotificationPerformed(boolean notificationPerformed) {
		this.notificationPerformed.set(notificationPerformed);
	}

	public boolean isShowPreview() {
		return showPreview;
	}

	public void setShowPreview(boolean showPreview) {
		this.showPreview = showPreview;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
