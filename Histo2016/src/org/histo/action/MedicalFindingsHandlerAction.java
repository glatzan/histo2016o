package org.histo.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.NotificationOption;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.PrintTemplate;
import org.histo.ui.medicalFindings.EmailNotificationSettings;
import org.histo.ui.medicalFindings.FaxNotificationSettings;
import org.histo.ui.medicalFindings.MedicalFindingsChooser;
import org.histo.ui.medicalFindings.PhoneNotificationSettings;
import org.histo.util.PdfGenerator;
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

	/**
	 * class for creating pdfs
	 */
	@Autowired
	private PdfGenerator pdfGenerator;
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

	public void prepareForNotification(Task task) {

		//
		//
		// HashMap<String, String> toReplace = new HashMap<String, String>();
		// toReplace.put("%name%",
		// task.getPatient().getPerson().getName() + ", " +
		// task.getPatient().getPerson().getSurname());
		// toReplace.put("%birthday%",
		// mainHandlerAction.date(task.getPatient().getPerson().getBirthday(),
		// DateFormat.GERMAN_DATE));
		// toReplace.put("%piz%", task.getPatient() == null ? "Keine Piz" :
		// task.getPatient().getPiz());
		//
		// MailTemplate taskReport =
		// mainHandlerAction.getSettings().getMail().getMailTemplate(MailPresetName.TaskReport);
		//
		// setEmailSubject(HistoUtil.replaceWildcardsInString(taskReport.getSubject(),
		// toReplace));
		//
		// setEmailText(taskReport.getContent());
		//
		// setNotificationPerformed(false);
		//
		// onAttachPdfToEmailChange();
		// onUseFaxChanges();
		// onUsePhoneChanges();
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
			ArrayList<PDFContainer> resultPdfsToDownload = new ArrayList<PDFContainer>();

			if (emailNotificationSettings.isUseEmail()) {
				logger.trace("Email notification");
				sendLog.append(resourceBundle.get("pdf.notification.email.text") + "\r\n");

				for (MedicalFindingsChooser notificationChooser : emailNotificationSettings
						.getNotificationEmailList()) {
					boolean emailSuccessful = false;

					// name and mail
					sendLog.append(notificationChooser.getContact().getPerson().getFullName() + "\t ");
					sendLog.append(resourceBundle.get("pdf.notification.email")
							+ notificationChooser.getContact().getPerson().getEmail() + "\t");

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						sendLog.append(resourceBundle.get("pdf.notification.none") + "\r\n");
						continue;
					} else if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
							&& notificationChooser.getPrintTemplate() != null) {
						// attach pdf to mail

						PDFContainer pdfToSend = pdfGenerator.generatePDFForReport(getTemporaryTask().getPatient(),
								getTemporaryTask(), notificationChooser.getPrintTemplate(),
								notificationChooser.getContact().getPerson());

						if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
								notificationChooser.getContact().getPerson().getEmail(),
								emailNotificationSettings.getEmailSubject(), emailNotificationSettings.getEmailText(),
								pdfToSend)) {
							emailSuccessful = true;
							// adding mail to the result array
							resultPdfs.add(pdfToSend);
							sendLog.append(pdfToSend.getName() + "\t");
						} else {
							// TODO: HAndle fault
						}

					} else {
						// plain text mail
						if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
								notificationChooser.getContact().getPerson().getEmail(),
								emailNotificationSettings.getEmailSubject(),
								emailNotificationSettings.getEmailText())) {
							emailSuccessful = true;
						} else {
							// TODO: HAndle fault
						}

						sendLog.append(resourceBundle.get("pdf.notification.email.text") + "\t");
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

						sendLog.append(resourceBundle.get("pdf.notification.email.performed") + "r\n");
					} else
						sendLog.append(resourceBundle.get("pdf.notification.email.failed") + "\r\n");
				}

				sendLog.append("\r\n");
				sendLog.append("\r\n");
			}

			if (faxNotificationSettings.isUseFax()) {
				logger.trace("Fax notification");
				sendLog.append(resourceBundle.get("pdf.notification.fax.text") + "\r\n");

				for (MedicalFindingsChooser notificationChooser : faxNotificationSettings.getNotificationFaxList()) {
					boolean faxSuccessful = false;

					sendLog.append(notificationChooser.getContact().getPerson().getFullName() + "\t");
					sendLog.append(resourceBundle.get("pdf.notification.fax.number") + " "
							+ notificationChooser.getContact().getPerson().getFax() + "\t");

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						sendLog.append(resourceBundle.get("pdf.notification.none") + "\r\n");
						continue;
					} else if (notificationChooser.getPrintTemplate() == null
							|| notificationChooser.getContact().getPerson().getFax() == null
							|| notificationChooser.getContact().getPerson().getFax().isEmpty()) {
						// error no templat or number
						sendLog.append(resourceBundle.get("pdf.notification.fax.notPossible") + "\r\n");
					} else {
						// creating pdf
						PDFContainer pdfToSend = pdfGenerator.generatePDFForReport(getTemporaryTask().getPatient(),
								getTemporaryTask(), notificationChooser.getPrintTemplate(),
								notificationChooser.getContact().getPerson());
						resultPdfs.add(pdfToSend);

						// TODO: SEND FAX

					}

					if (faxSuccessful) {
						notificationChooser.getContact().setNotificationPerformed(true);
						notificationChooser.setPerformed(true);

						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());

						sendLog.append(resourceBundle.get("pdf.notification.fax.performed") + "r\n");
					} else
						sendLog.append(resourceBundle.get("pdf.notification.fax.failed") + "\r\n");

				}

				sendLog.append("\r\n");
				sendLog.append("\r\n");
			}

			if (phoneNotificationSettings.isUsePhone()) {
				sendLog.append(resourceBundle.get("pdf.notification.phone.text"));

				for (MedicalFindingsChooser notificationChooser : phoneNotificationSettings
						.getNotificationPhoneList()) {

					sendLog.append(notificationChooser.getContact().getPerson().getFullName() + "\t");
					sendLog.append(resourceBundle.get("pdf.notification.phone.number") + " "
							+ notificationChooser.getContact().getPerson().getPhoneNumber() + "\t");

					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						sendLog.append(resourceBundle.get("pdf.notification.none") + "\r\n");
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
				sendLog.append("\r\n");
			}

			// getting the template for the send report
			PrintTemplate sendReport = PrintTemplate
					.getDefaultTemplate(PrintTemplate.getTemplatesByType(DocumentType.MEDICAL_FINDINGS_SEND_REPORT));

			// sendreport has date and datafiled
			HashMap<String, String> addtionalFields = new HashMap<String, String>();
			addtionalFields.put("REPORT_DATE", mainHandlerAction.date(System.currentTimeMillis()));
			addtionalFields.put("REPORT_DATA", sendLog.toString());

			PDFContainer sendReportPDF = pdfGenerator.generateSimplePDF(getTemporaryTask().getPatient(), sendReport,
					addtionalFields);

			resultPdfs.add(0, sendReportPDF);

			PDFContainer resultPdf = PdfGenerator.mergePdfs(resultPdfs, sendReportPDF.getName(),
					DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

			printHandlerAction.saveGeneratedPdf(resultPdf);

			getTemporaryTask().setNotificationPhase(false);
			getTemporaryTask().setNotificationCompletionDate(System.currentTimeMillis());

			mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.update");
			notificationRunning.set(false);
		}
	}

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
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
