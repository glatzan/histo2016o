package org.histo.action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.histo.action.dialog.PrintDialogHandler;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SlideManipulationHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.MailType;
import org.histo.config.enums.NotificationOption;
import org.histo.dao.GenericDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.mail.MailTemplate;
import org.histo.ui.medicalFindings.EmailNotificationSettings;
import org.histo.ui.medicalFindings.FaxNotificationSettings;
import org.histo.ui.medicalFindings.MedicalFindingsChooser;
import org.histo.ui.medicalFindings.NoContactDataNotificationSettings;
import org.histo.ui.medicalFindings.PhoneNotificationSettings;
import org.histo.util.HistoUtil;
import org.histo.util.printer.PrintTemplate;
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
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private PrintDialogHandler printDialogHandler;

	@Autowired
	private MediaHandlerAction mediaHandlerAction;
	/**
	 * class for creating pdfs
	 */
	@Autowired
	private PDFGeneratorHandler pDFGeneratorHandler;

	@Autowired
	private SlideManipulationHandler slideManipulationHandler;

	@Autowired
	private UtilDAO utilDAO;

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

	/**
	 * Contains all contacts with no contact data available
	 */
	private NoContactDataNotificationSettings noContactDataNotificationSettings;
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
		initBean(task);
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

	public void reSendMedicialFindings() {
		logger.trace("Resend medical findings");
		notificationPerformed.set(false);
		initBean(getTemporaryTask());
	}

	public void initBean(Task task) {
		setTemporaryTask(task);

		utilDAO.initializeDataList(task);

		setActiveTabIndex(0);

		if (getEmailNotificationSettings() == null)
			setEmailNotificationSettings(new EmailNotificationSettings(task));
		if (getFaxNotificationSettings() == null)
			setFaxNotificationSettings(new FaxNotificationSettings(task));
		if (getPhoneNotificationSettings() == null)
			setPhoneNotificationSettings(new PhoneNotificationSettings(task));
		if (getNoContactDataNotificationSettings() == null)
			setNoContactDataNotificationSettings(new NoContactDataNotificationSettings(task));

		updateNotificationLists();

		MailTemplate taskReport = MailTemplate.factroy(MailType.MedicalFindingsReport);

		if (getEmailNotificationSettings().getEmailSubject() == null)
			emailNotificationSettings.setEmailSubject(taskReport.getSubject());

		if (getEmailNotificationSettings().getEmailText() == null)
			emailNotificationSettings.setEmailText(taskReport.getContent());

		setShowPreview(false);
	}

	public void finalizeTask() {
		logger.trace("Finalize Task");

		getTemporaryTask().setFinalized(true);

		// ending stating phase
		if (getTemporaryTask().isStainingPhase()) {
			slideManipulationHandler.setStainingCompletedForAllSlidesTo(getTemporaryTask(), true);
			temporaryTask.setStainingPhase(false);
			mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.end");
		}

		// ending diagnosis phase
		if (getTemporaryTask().isDiagnosisPhase()) {
			getTemporaryTask().setDiagnosisPhase(false);
		}

		mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.finalized");

		hideMedicalFindingsDialog();
	}

	/**
	 * Updates all notification lists. Is used if the contacts were changed
	 * using the contact dialog!
	 */
	public void updateNotificationLists() {
		getEmailNotificationSettings().updateNotificationEmailList();
		getFaxNotificationSettings().updateNotificationFaxList();
		getPhoneNotificationSettings().updateNotificationPhoneList();
		getNoContactDataNotificationSettings().updateNoContactDataList();
	}

	@Async("taskExecutor")
	public void performeNotification() {

		if (notificationRunning.get())
			return;

		notificationRunning.set(true);

		logger.trace("Startin notification thread");

		// patientDao.initializeDataList(getTmpTask());
		// taskDAO.initializeDiagnosisData(getTmpTask());

		if (emailNotificationSettings.isUseEmail() || faxNotificationSettings.isUseFax()
				|| phoneNotificationSettings.isUsePhone()) {
			ArrayList<PDFContainer> resultPdfs = new ArrayList<PDFContainer>();

			// EMAIL
			if (emailNotificationSettings.isUseEmail()) {
				logger.trace("Email notification");

				for (MedicalFindingsChooser notificationChooser : emailNotificationSettings
						.getNotificationEmailList()) {
					boolean emailSuccessful = false;

					// name and mail
					logger.trace("Email to " + notificationChooser.getContact().getPerson().getFullName() + " ("
							+ notificationChooser.getContact().getPerson().getEmail() + ")");

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						logger.trace("No notification desired");
						continue;
					} else if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
							&& notificationChooser.getPrintTemplate() != null) {
						// attach pdf to mail
						PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(
								getTemporaryTask().getPatient(), getTemporaryTask(),
								notificationChooser.getPrintTemplate(), notificationChooser.getContact().getPerson());

						if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
								notificationChooser.getContact().getPerson().getEmail(),
								emailNotificationSettings.getEmailSubject(), emailNotificationSettings.getEmailText(),
								pdfToSend)) {
							emailSuccessful = true;
							// adding mail to the result array
							resultPdfs.add(pdfToSend);
							logger.trace("PDF successfully send");
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
							logger.trace("Text successfully send");
						} else {
							// TODO: Handle fault
						}
					}

					// check if mail was send
					if (emailSuccessful) {

						// setting the contact to perfomed
						notificationChooser.getContact().setEmailNotificationPerformed(true);
						notificationChooser.setPerformed(true);

						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());

					}
				}

			} else {
				logger.trace("No Email notification");
			}

			// FAX
			if (faxNotificationSettings.isUseFax()) {
				logger.trace("Fax notification");

				for (MedicalFindingsChooser notificationChooser : faxNotificationSettings.getNotificationFaxList()) {
					boolean faxSuccessful = false;

					// name and number
					logger.trace("Fax to " + notificationChooser.getContact().getPerson().getFullName() + " ("
							+ notificationChooser.getContact().getPerson().getEmail() + ")");

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						logger.trace("No notification desired");
						continue;
					} else if (notificationChooser.getPrintTemplate() == null
							|| notificationChooser.getContact().getPerson().getFax() == null
							|| notificationChooser.getContact().getPerson().getFax().isEmpty()) {
						// error no templat or number
						logger.trace("Error, no Fax-Number or TemplateUtil");
					} else {
						// creating pdf
						PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(
								getTemporaryTask().getPatient(), getTemporaryTask(),
								notificationChooser.getPrintTemplate(), notificationChooser.getContact().getPerson());
						resultPdfs.add(pdfToSend);

						// TODO: SEND FAX

						faxSuccessful = true;
						logger.trace("Fax send successfully");

						// sendLog.append(resourceBundle.get("pdf.notification.faxNotification.fax.error"));
					}

					if (faxSuccessful) {
						notificationChooser.getContact().setFaxNotificationPerformed(true);
						notificationChooser.setPerformed(true);

						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());
					}
				}

			} else {
				logger.trace("No Fax notification");
			}

			// PHONE
			if (phoneNotificationSettings.isUsePhone()) {
				logger.trace("Phone notification");

				for (MedicalFindingsChooser notificationChooser : phoneNotificationSettings
						.getNotificationPhoneList()) {

					// name and number
					logger.trace("Phone to " + notificationChooser.getContact().getPerson().getFullName() + " ("
							+ notificationChooser.getContact().getPerson().getPhoneNumber() + ")");

					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						continue;
					} else {
						notificationChooser.getContact().setPhoneNotificationPerformed(true);
						notificationChooser.setPerformed(true);
						genericDAO.save(notificationChooser.getContact(),
								resourceBundle.get("log.patient.task.contact.notification.telefon.performed",
										getTemporaryTask().getTaskID(),
										notificationChooser.getContact().getPerson().getFullName()),
								getTemporaryTask().getPatient());
					}

				}
			} else {
				logger.trace("No phone notification");
			}

			// getting the template for the send report
			PrintTemplate sendReport = PrintTemplate
					.getDefaultTemplate(PrintTemplate.getTemplatesByType(DocumentType.MEDICAL_FINDINGS_SEND_REPORT));

			// sendreport has date and datafiled
			HashMap<String, String> addtionalFields = new HashMap<String, String>();
			addtionalFields.put("reportDate", mainHandlerAction.date(System.currentTimeMillis()));

			PDFContainer sendReportPDF = pDFGeneratorHandler.generateSendReport(sendReport,
					getTemporaryTask().getPatient(), getEmailNotificationSettings(), getFaxNotificationSettings(),
					getPhoneNotificationSettings());

			resultPdfs.add(0, sendReportPDF);

			PDFContainer resultPdf = PDFGeneratorHandler.mergePdfs(resultPdfs, sendReportPDF.getName(),
					DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

			printDialogHandler.savePdf(getTemporaryTask(), resultPdf);

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
		printDialogHandler.initBeanForExternalDisplay(getTemporaryTask(),
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

	public NoContactDataNotificationSettings getNoContactDataNotificationSettings() {
		return noContactDataNotificationSettings;
	}

	public void setNoContactDataNotificationSettings(
			NoContactDataNotificationSettings noContactDataNotificationSettings) {
		this.noContactDataNotificationSettings = noContactDataNotificationSettings;
	}
	/********************************************************
	 * Getter/Setter
	 ********************************************************/
}
