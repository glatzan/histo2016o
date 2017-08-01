package org.histo.ui.medicalFindings;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.dialog.media.MediaDialog;
import org.histo.action.dialog.print.PrintDialog;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SlideManipulationHandler;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.handler.TaskStatusHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.MailType;
import org.histo.config.enums.NotificationOption;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.mail.MailTemplate;
import org.histo.util.printer.template.AbstractTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class NotificationDialog {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	private MediaDialog mediaDialog;
	/**
	 * class for creating pdfs
	 */
	@Autowired
	private PDFGeneratorHandler pDFGeneratorHandler;

	@Autowired
	private SlideManipulationHandler slideManipulationHandler;

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private TaskStatusHandler taskStatusHandler;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

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
	 * Contains all contacts with no associatedContact data available
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
		try {
			setTemporaryTask((Task) utilDAO.initializeDataList(task));
		} catch (CustomDatabaseInconsistentVersionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

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

		DiagnosisReportMail taskReport = DiagnosisReportMail.factroy(MailType.MedicalFindingsReport);

		if (getEmailNotificationSettings().getEmailSubject() == null)
			emailNotificationSettings.setEmailSubject(taskReport.getSubject());

		if (getEmailNotificationSettings().getEmailText() == null)
			emailNotificationSettings.setEmailText(taskReport.getContent());

		setShowPreview(false);
	}

	public void finalizeTask() {
		try {
			logger.trace("Finalize Task");

			getTemporaryTask().setFinalized(true);

			// ending stating phase
			if (!taskStatusHandler.isStainingCompleted(getTemporaryTask())) {
				slideManipulationHandler.setStainingCompletedForAllSlides(getTemporaryTask(), true);
				mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.end");

				if (getTemporaryTask().getStainingCompletionDate() == 0) {
					getTemporaryTask().setStainingCompletionDate(System.currentTimeMillis());
					mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.stainingPhase.end");
				}
			}

			if (!taskStatusHandler.isDiagnosisCompleted(getTemporaryTask())) {
				taskManipulationHandler.finalizeAllDiangosisRevisions(
						getTemporaryTask().getDiagnosisContainer().getDiagnosisRevisions(), true);

				if (getTemporaryTask().getDiagnosisCompletionDate() == 0) {
					getTemporaryTask().setDiagnosisCompletionDate(System.currentTimeMillis());

					mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.change.diagnosisPhase.end");
				}
			}

			favouriteListDAO.removeTaskFromList(getTemporaryTask(),
					new PredefinedFavouriteList[] { PredefinedFavouriteList.DiagnosisList,
							PredefinedFavouriteList.ReDiagnosisList, PredefinedFavouriteList.StainingList,
							PredefinedFavouriteList.ReStainingList });

			mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.finalized");

			hideMedicalFindingsDialog();
		} catch (CustomDatabaseInconsistentVersionException e) {
			// TODO init as dialog
			e.printStackTrace();
			// onDatabaseVersionConflict();
		}
	}

	/**
	 * Updates all notification lists. Is used if the contacts were changed
	 * using the associatedContact dialog!
	 */
	public void updateNotificationLists() {
		getEmailNotificationSettings().updateNotificationEmailList();
		getFaxNotificationSettings().updateNotificationFaxList();
		getPhoneNotificationSettings().updateNotificationPhoneList();
		getNoContactDataNotificationSettings().updateNoContactDataList();
	}

	@Async("taskExecutor")
	public void performeNotification() {
		try {
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
								+ notificationChooser.getContact().getPerson().getContact().getEmail() + ")");

						// no notification
						if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
							logger.trace("No notification desired");
							continue;
						} else if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
								&& notificationChooser.getPrintTemplate() != null) {
							// attach pdf to mail
//							PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(
//									getTemporaryTask().getPatient(), getTemporaryTask(),
//									notificationChooser.getPrintTemplate(), notificationChooser);
//TODO: rewrite
//							if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
//									notificationChooser.getContact().getPerson().getContact().getEmail(),
//									emailNotificationSettings.getEmailSubject(),
//									emailNotificationSettings.getEmailText(), pdfToSend)) {
//								emailSuccessful = true;
//								// adding mail to the result array
////								resultPdfs.add(pdfToSend);
//								logger.trace("PDF successfully send");
//							} else {
//								// TODO: HAndle fault
//							}

						} else {
							// plain text mail
							if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
									notificationChooser.getContact().getPerson().getContact().getEmail(),
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

							// setting the associatedContact to perfomed
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

					for (MedicalFindingsChooser notificationChooser : faxNotificationSettings
							.getNotificationFaxList()) {
						boolean faxSuccessful = false;

						// name and number
						logger.trace("Fax to " + notificationChooser.getContact().getPerson().getFullName() + " ("
								+ notificationChooser.getContact().getPerson().getContact().getEmail() + ")");

						// no notification
						if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
							logger.trace("No notification desired");
							continue;
						} else if (notificationChooser.getPrintTemplate() == null
								|| notificationChooser.getContact().getPerson().getContact().getFax() == null
								|| notificationChooser.getContact().getPerson().getContact().getFax().isEmpty()) {
							// error no templat or number
							logger.trace("Error, no Fax-Number or TemplateUtil");
						} else {
							// creating pdf
//							PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(
//									getTemporaryTask().getPatient(), getTemporaryTask(),
//									notificationChooser.getPrintTemplate(),
//									notificationChooser.getContact().getPerson());
//							resultPdfs.add(pdfToSend);

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
								+ notificationChooser.getContact().getPerson().getContact().getPhone() + ")");

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
				AbstractTemplate sendReport = AbstractTemplate.getDefaultTemplate(
						AbstractTemplate.getTemplatesByType(DocumentType.MEDICAL_FINDINGS_SEND_REPORT));

				// sendreport has date and datafiled
				HashMap<String, String> addtionalFields = new HashMap<String, String>();
				addtionalFields.put("reportDate", mainHandlerAction.date(System.currentTimeMillis()));

				PDFContainer sendReportPDF = pDFGeneratorHandler.generateSendReport(sendReport,
						getTemporaryTask().getPatient(), getEmailNotificationSettings(), getFaxNotificationSettings(),
						getPhoneNotificationSettings());

				resultPdfs.add(0, sendReportPDF);

				PDFContainer resultPdf = PDFGeneratorHandler.mergePdfs(resultPdfs, sendReportPDF.getName(),
						DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

				dialogHandlerAction.getPrintDialog().savePdf(getTemporaryTask(), resultPdf);

				favouriteListDAO.removeTaskFromList(getTemporaryTask(), PredefinedFavouriteList.NotificationList);
				getTemporaryTask().setNotificationCompletionDate(System.currentTimeMillis());

				mainHandlerAction.saveDataChange(getTemporaryTask(), "log.patient.task.update");

				mediaDialog.setTemporaryPdfContainer(resultPdf);

				notificationRunning.set(false);
				notificationPerformed.set(true);
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			// TODO move to dialog
		}
	}

	/********************************************************
	 * Preview
	 ********************************************************/
	public void showPreviewForContact(MedicalFindingsChooser notificationEmailList) {
		dialogHandlerAction.getPrintDialog().initBeanForExternalDisplay(getTemporaryTask(),
				new AbstractTemplate[] { notificationEmailList.getPrintTemplate() },
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
