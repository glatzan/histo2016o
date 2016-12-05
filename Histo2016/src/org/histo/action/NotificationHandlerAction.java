package org.histo.action;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.DateFormat;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.MailPresetName;
import org.histo.config.enums.Notification;
import org.histo.config.enums.NotificationOption;
import org.histo.dao.GenericDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.model.transitory.json.MailTemplate;
import org.histo.model.transitory.json.PdfTemplate;
import org.histo.ui.NotificationChooser;
import org.histo.util.HistoUtil;
import org.histo.util.PdfGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class NotificationHandlerAction implements Serializable {

	private static final long serialVersionUID = -3672859612072175725L;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private PdfHandlerAction pdfHandlerAction;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	/**
	 * Task to perfome notification
	 */
	private Task tmpTask;

	/**
	 * The current notificationTab
	 */
	private Notification notificationTab;

	/**
	 * If only a single pdf should be change the physician is stored here
	 */
	private NotificationChooser customPdfToPhysician;

	/********************************************************
	 * Email
	 ********************************************************/

	/**
	 * True if email should be send;
	 */
	private boolean useEmail;

	/**
	 * List of physician notify via email
	 */
	private List<NotificationChooser> notificationEmailList;

	/**
	 * If Pdf is chosen via the general choose option it is saved here.
	 */
	private PDFContainer defaultEmailPdf;

	/**
	 * The subject of the email to send
	 */
	private String emailSubject;

	/**
	 * The text of the email to send
	 */
	private String emailText;

	/**
	 * True if the report should be send as well
	 */
	private boolean attachPdfToEmail;

	/********************************************************
	 * Email
	 ********************************************************/

	/********************************************************
	 * Fax
	 ********************************************************/
	/**
	 * True if fax should be send;
	 */
	private boolean useFax;

	/**
	 * List of physician notify via email
	 */
	private List<NotificationChooser> notificationFaxList;

	/********************************************************
	 * Fax
	 ********************************************************/

	/********************************************************
	 * Phone
	 ********************************************************/
	/**
	 * True if fax should be send;
	 */
	private boolean usePhone;

	/**
	 * List of physician notify via email
	 */
	private List<NotificationChooser> notificationPhoneList;
	/********************************************************
	 * Phone
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

	public void showinfoDialog(Task task) {
		showinfoDialog(task, false);
	}

	public void showinfoDialog(Task task, boolean show) {
		setTmpTask(task);

		// loading all pfs
		taskDAO.initializePdfData(task);

		if (task.isNotificationCompleted() && !show) {
			// setting last manual report ready for download
			PDFContainer maunalReport = task.getReport(PdfTemplate.MANUAL_REPOT);
			pdfHandlerAction.setTmpPdfContainer(maunalReport);
			mainHandlerAction.showDialog(Dialog.NOTIFICATION_ALREADY_PERFORMED);
		} else {
			prepareForNotification(task);
			mainHandlerAction.showDialog(Dialog.NOTIFICATION);
		}
	}

	public void prepareForNotification(Task task) {

		setNotificationEmailList(NotificationChooser.getSublist(task.getContacts(), ContactMethod.EMAIL));
		setNotificationFaxList(NotificationChooser.getSublist(task.getContacts(), ContactMethod.FAX));
		setNotificationPhoneList(NotificationChooser.getSublist(task.getContacts(), ContactMethod.PHONE));

		setNotificationTab(Notification.EMAIL);

		setUseEmail(!getNotificationEmailList().isEmpty() ? true : false);
		setUseFax(!getNotificationFaxList().isEmpty() ? true : false);
		setUsePhone(!getNotificationPhoneList().isEmpty() ? true : false);

		HashMap<String, String> toReplace = new HashMap<String, String>();
		toReplace.put("%name%",
				task.getPatient().getPerson().getName() + ", " + task.getPatient().getPerson().getSurname());
		toReplace.put("%birthday%",
				mainHandlerAction.date(task.getPatient().getPerson().getBirthday(), DateFormat.GERMAN_DATE));
		toReplace.put("%piz%", task.getPatient() == null ? "Keine Piz" : task.getPatient().getPiz());

		MailTemplate taskReport = mainHandlerAction.getSettings().getMail().getMailTemplate(MailPresetName.TaskReport);

		setEmailSubject(HistoUtil.replaceWildcardsInString(taskReport.getSubject(), toReplace));

		setEmailText(taskReport.getContent());

		setNotificationPerformed(false);

		onAttachPdfToEmailChange();
		onUseFaxChanges();
		onUsePhoneChanges();
	}

	public void hideNotificatonDialog() {
		// TODO CLEAR
		// setNotifications(null);
		mainHandlerAction.hideDialog(Dialog.NOTIFICATION);
	}

	public void hideAlreadyPerformedDialogAndShowNotification(boolean notify) {
		if (notify) {
			// TODO CLEAR
			// workaround for showing and hiding two dialogues
			mainHandlerAction.setQueueDialog("#headerForm\\\\:notificationBtnShowOnly");
		}
		mainHandlerAction.hideDialog(Dialog.NOTIFICATION_ALREADY_PERFORMED);
	}

	public void onAttachPdfToEmailChange() {
		for (NotificationChooser notificationChooser : getNotificationEmailList()) {
			if (isAttachPdfToEmail()) {
				if (notificationChooser.getNotificationAttachment() != NotificationOption.NONE) {
					notificationChooser.setNotificationAttachment(NotificationOption.PDF);
					notificationChooser.setPdf(getDefaultEmailPdf());
				}
			} else {
				if (notificationChooser.getNotificationAttachment() != NotificationOption.NONE) {
					notificationChooser.setNotificationAttachment(NotificationOption.TEXT);
					notificationChooser.setPdf(null);
				}
			}
		}
	}

	public void onUseFaxChanges() {
		for (NotificationChooser notificationChooser : getNotificationFaxList()) {
			if (isUseFax()) {
				notificationChooser.setNotificationAttachment(NotificationOption.FAX);
			} else {
				notificationChooser.setNotificationAttachment(NotificationOption.NONE);
			}
		}
	}

	public void onUsePhoneChanges() {
		for (NotificationChooser notificationChooser : getNotificationPhoneList()) {
			if (isUsePhone()) {
				notificationChooser.setNotificationAttachment(NotificationOption.PHONE);
			} else {
				notificationChooser.setNotificationAttachment(NotificationOption.NONE);
			}
		}
	}

	public void showPreviewDialog() {
		showPreviewDialog(null);
	}

	public void showPreviewDialog(NotificationChooser chooser) {
		setCustomPdfToPhysician(chooser);

		if (getNotificationTab() == Notification.EMAIL) {

			PDFContainer container = getTmpTask().getReport(PdfTemplate.INTERNAL);

			if (container != null) {
				pdfHandlerAction.prepareForAttachedPdf(getTmpTask(), container);
			} else {
				pdfHandlerAction.prepareForPdf(getTmpTask(), PdfTemplate.INTERNAL);
			}

		} else if (getNotificationTab() == Notification.FAX) {
			PDFContainer container = getTmpTask().getReportByName(
					chooser.getContact().getPhysician().getPerson().getFullName().replace(" ", "_").replace(".", ""));

			if (container != null) {
				pdfHandlerAction.prepareForAttachedPdf(getTmpTask(), container);
			} else {
				pdfHandlerAction.setExternalPhysician(chooser.getContact().getPhysician());
				pdfHandlerAction.setExternalReportPhysicianType(ContactRole.OTHER);
				if (pdfHandlerAction.getSignatureTmpPhysician() == null) {
					pdfHandlerAction.setSignatureTmpPhysician(userHandlerAction.getCurrentUser().getPhysician());
				}
				pdfHandlerAction.prepareForPdf(getTmpTask(), PdfTemplate.EXTERN);
			}
		}

		mainHandlerAction.showDialog(Dialog.NOTIFICATION_PREVIEW);
	}

	public void hidePreviewDialog(boolean useSelectedPdf) {

		mainHandlerAction.hideDialog(Dialog.NOTIFICATION_PREVIEW);

		if (useSelectedPdf) {
			if (getNotificationTab() == Notification.EMAIL) {
				if (getCustomPdfToPhysician() == null) {
					setDefaultEmailPdf(pdfHandlerAction.getTmpPdfContainer());

					for (NotificationChooser notificationChooser : notificationEmailList) {
						if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
								&& notificationChooser.getPdf() == null) {
							notificationChooser.setPdf(getDefaultEmailPdf());
						}
					}
				} else {
					if (getCustomPdfToPhysician().getNotificationAttachment() == NotificationOption.PDF)
						getCustomPdfToPhysician().setPdf(pdfHandlerAction.getTmpPdfContainer());
				}
			} else if (getNotificationTab() == Notification.FAX) {
				if (getCustomPdfToPhysician().getNotificationAttachment() == NotificationOption.FAX)
					getCustomPdfToPhysician().setPdf(pdfHandlerAction.getTmpPdfContainer());
			}
		}

		// pdfHandlerAction.clearData();
	}

	@Async("taskExecutor")
	public void performeNotification() {

		if (notificationRunning.get())
			return;

		notificationRunning.set(true);

		taskDAO.initializeCouncilData(getTmpTask());
		taskDAO.initializeReportData(getTmpTask());

		if (isUseFax() || isUsePhone() || isUseEmail()) {

			ArrayList<PDFContainer> resultPdfs = new ArrayList<PDFContainer>();
			ArrayList<PDFContainer> resultPdfsToDownload = new ArrayList<PDFContainer>();

			StringBuilder result = new StringBuilder();

			// using email notification
			if (isUseEmail()) {

				// email
				result.append(resourceBundle.get("pdf.notification.email.text") + "\r\n");

				for (NotificationChooser notificationChooser : notificationEmailList) {

					boolean emailSuccessful = false;

					// name and mail
					result.append(notificationChooser.getContact().getPhysician().getPerson().getFullName() + "\t ");
					result.append(resourceBundle.get("pdf.notification.email")
							+ notificationChooser.getContact().getPhysician().getPerson().getEmail() + "\t");

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						result.append(resourceBundle.get("pdf.notification.none") + "\r\n");
						continue;
					} else if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
							&& notificationChooser.getPdf() != null) {
						// attach pdf to mail

						// check if pdf was selected
						if (notificationChooser.getPdf() != null) {

							// MailUtil.sendMail(notificationChooser.getContact().getPhysician().getEmail(),
							// HistoSettings.EMAIL_FROM,
							// HistoSettings.EMAIL_FROM_NAME, getEmailSubject(),
							// getEmailText(), notificationChooser.getPdf());
							emailSuccessful = true;

							// adding mail to the result array
							resultPdfs.add(notificationChooser.getPdf());
							// adding the name of the pdf file to the report
							// list pdf
							result.append(notificationChooser.getPdf().getName() + "\t");

							// if a new pdf was created save this to database
							if (notificationChooser.getPdf().getId() == 0) {
								genericDAO
										.save(notificationChooser.getPdf(),
												resourceBundle.get("log.patient.task.pdf.created",
														notificationChooser.getPdf().getName()),
												getTmpTask().getPatient());

								getTmpTask().addReport(notificationChooser.getPdf());

								genericDAO.save(
										getTmpTask(), resourceBundle.get("log.patient.task.pdf.attached",
												getTmpTask().getTaskID(), notificationChooser.getPdf().getName()),
										getTmpTask().getPatient());
							}

						} else {
							// user has forgotten to add pdf, so send plain mail

							// MailUtil.sendMail(notificationChooser.getContact().getPhysician().getEmail(),
							// HistoSettings.EMAIL_FROM,
							// HistoSettings.EMAIL_FROM_NAME, getEmailSubject(),
							// getEmailText());

							result.append(resourceBundle.get("pdf.notification.email.text") + "\t");
						}

					} else {
						// only plain mail should be send

						// MailUtil.sendMail(notificationChooser.getContact().getPhysician().getEmail(),
						// HistoSettings.EMAIL_FROM,
						// HistoSettings.EMAIL_FROM_NAME, getEmailSubject(),
						// getEmailText());
						emailSuccessful = true;

						result.append(resourceBundle.get("pdf.notification.email.text") + "\t");

					}

					// check if mail was send
					if (emailSuccessful) {

						// setting the contact to perfomed
						notificationChooser.getContact().setNotificationPerformed(true);
						genericDAO
								.save(notificationChooser.getContact(),
										resourceBundle.get("log.patient.task.contact.notification.performed",
												getTmpTask().getTaskID(), notificationChooser.getContact()
														.getPhysician().getPerson().getFullName()),
										getTmpTask().getPatient());

						notificationChooser.setPerformed(true);
						result.append(resourceBundle.get("pdf.notification.email.performed") + "r\n");
					} else
						result.append(resourceBundle.get("pdf.notification.email.failed") + "\r\n");

				}

				result.append("\r\n");
			}

			// if use fax
			if (isUseFax()) {

				result.append(resourceBundle.get("pdf.notification.fax.text") + "\r\n");

				for (NotificationChooser notificationChooser : notificationFaxList) {

					result.append(notificationChooser.getContact().getPhysician().getPerson().getFullName() + "\t");
					result.append(resourceBundle.get("pdf.notification.fax.number") + " "
							+ notificationChooser.getContact().getPhysician().getPerson().getFax() + "\t");

					// no notification
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						result.append(resourceBundle.get("pdf.notification.none") + "\r\n");
						continue;
					} else {

						if (notificationChooser.getPdf() != null) {

							resultPdfs.add(notificationChooser.getPdf());
							resultPdfsToDownload.add(notificationChooser.getPdf());

							result.append(notificationChooser.getPdf().getName() + "\t");

							// saving because new pdf
							if (notificationChooser.getPdf().getId() == 0) {
								genericDAO
										.save(notificationChooser.getPdf(),
												resourceBundle.get("log.patient.task.pdf.created",
														notificationChooser.getPdf().getName()),
												getTmpTask().getPatient());

								getTmpTask().addReport(notificationChooser.getPdf());

								genericDAO.save(
										getTmpTask(), resourceBundle.get("log.patient.task.pdf.attached",
												getTmpTask().getTaskID(), notificationChooser.getPdf().getName()),
										getTmpTask().getPatient());
							}

						} else
							result.append(resourceBundle.get("pdf.notification.fax.noPdf") + "\r\n");

						notificationChooser.getContact().setNotificationPerformed(true);

						genericDAO
								.save(notificationChooser.getContact(),
										resourceBundle.get("log.patient.task.contact.notification.fax.performed",
												getTmpTask().getTaskID(), notificationChooser.getContact()
														.getPhysician().getPerson().getFullName()),
										getTmpTask().getPatient());
					}
				}

				result.append("\r\n");
			}

			// if use phone
			if (isUsePhone()) {

				result.append(resourceBundle.get("pdf.notification.phone.text"));

				for (NotificationChooser notificationChooser : notificationPhoneList) {

					result.append(notificationChooser.getContact().getPhysician().getPerson().getFullName() + "\t");
					result.append(resourceBundle.get("pdf.notification.phone.number") + " "
							+ notificationChooser.getContact().getPhysician().getPerson().getPhoneNumber() + "\t");

					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE) {
						result.append(resourceBundle.get("pdf.notification.none") + "\r\n");
						continue;
					} else {
						notificationChooser.getContact().setNotificationPerformed(true);
						genericDAO
								.save(getTmpTask(),
										resourceBundle.get("log.patient.task.contact.notification.telefon.performed",
												getTmpTask().getTaskID(), notificationChooser.getContact()
														.getPhysician().getPerson().getFullName()),
										getTmpTask().getPatient());
					}

				}

				result.append("\r\n");
			}

			// getting the template for the report
			PdfTemplate manuelReport = PdfTemplate.getTemplateByType(HistoSettings.PDF_TEMPLATE_JSON,
					PdfTemplate.MANUAL_REPOT);

			// addition field
			HashMap<String, String> addtionalFields = new HashMap<String, String>();
			addtionalFields.put("B_NOTIFY", result.toString());

			// generating report
			PDFContainer manelReportPdf = (new PdfGenerator(mainHandlerAction, resourceBundle))
					.generatePdf(getTmpTask(), manuelReport, System.currentTimeMillis(), null, null, addtionalFields);

			// adding as first page to the printout
			resultPdfs.add(0, manelReportPdf);
			resultPdfsToDownload.add(0, manelReportPdf);

			// generating full report to save in database
			String pdfName = (manuelReport.isNameAsResources() ? resourceBundle.get(manuelReport.getName())
					: manuelReport.getName());

			PDFContainer resultPdf = PdfGenerator.mergePdfs(resultPdfs,
					pdfName + "_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf",
					"MANUAL_REPOT");
			getTmpTask().addReport(resultPdf);
			getTmpTask().setNotificationCompleted(true);
			getTmpTask().setNotificationCompletionDate(System.currentTimeMillis());

			// savin complete report
			genericDAO.save(resultPdf, resourceBundle.get("log.patient.task.pdf.created", resultPdf.getName()),
					getTmpTask().getPatient());

			// saving task
			genericDAO.save(getTmpTask(),
					resourceBundle.get("log.patient.task.pdf.attached", getTmpTask().getTaskID(), resultPdf.getName()),
					getTmpTask().getPatient());

			// generated report without email pdfs is returned to download
			PDFContainer resultPdfToDownload = PdfGenerator.mergePdfs(resultPdfs,
					pdfName + "_" + mainHandlerAction.date(System.currentTimeMillis()).replace(".", "_") + ".pdf",
					"MANUAL_REPOT");

			pdfHandlerAction.setTmpPdfContainer(resultPdfToDownload);

			setNotificationPerformed(true);

		}

		notificationRunning.set(false);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	public Task getTmpTask() {
		return tmpTask;
	}

	public void setTmpTask(Task tmpTask) {
		this.tmpTask = tmpTask;
	}

	public Notification getNotificationTab() {
		return notificationTab;
	}

	public void setNotificationTab(Notification notificationTab) {
		this.notificationTab = notificationTab;
	}

	public String getEmailSubject() {
		return emailSubject;
	}

	public String getEmailText() {
		return emailText;
	}

	public void setEmailSubject(String emailSubject) {
		this.emailSubject = emailSubject;
	}

	public void setEmailText(String emailText) {
		this.emailText = emailText;
	}

	public List<NotificationChooser> getNotificationEmailList() {
		return notificationEmailList;
	}

	public void setNotificationEmailList(List<NotificationChooser> notificationEmailList) {
		this.notificationEmailList = notificationEmailList;
	}

	public PDFContainer getDefaultEmailPdf() {
		return defaultEmailPdf;
	}

	public void setDefaultEmailPdf(PDFContainer defaultEmailPdf) {
		this.defaultEmailPdf = defaultEmailPdf;
	}

	public boolean isAttachPdfToEmail() {
		return attachPdfToEmail;
	}

	public void setAttachPdfToEmail(boolean attachPdfToEmail) {
		this.attachPdfToEmail = attachPdfToEmail;
	}

	public NotificationChooser getCustomPdfToPhysician() {
		return customPdfToPhysician;
	}

	public void setCustomPdfToPhysician(NotificationChooser customPdfToPhysician) {
		this.customPdfToPhysician = customPdfToPhysician;
	}

	public List<NotificationChooser> getNotificationFaxList() {
		return notificationFaxList;
	}

	public void setNotificationFaxList(List<NotificationChooser> notificationFaxList) {
		this.notificationFaxList = notificationFaxList;
	}

	public boolean isUseEmail() {
		return useEmail;
	}

	public void setUseEmail(boolean useEmail) {
		this.useEmail = useEmail;
	}

	public boolean isUseFax() {
		return useFax;
	}

	public void setUseFax(boolean useFax) {
		this.useFax = useFax;
	}

	public boolean isUsePhone() {
		return usePhone;
	}

	public List<NotificationChooser> getNotificationPhoneList() {
		return notificationPhoneList;
	}

	public void setUsePhone(boolean usePhone) {
		this.usePhone = usePhone;
	}

	public void setNotificationPhoneList(List<NotificationChooser> notificationPhoneList) {
		this.notificationPhoneList = notificationPhoneList;
	}

	public boolean isNotificationRunning() {
		return notificationRunning.get();
	}

	public void setNotificationRunning(boolean notificationRunning) {
		this.notificationRunning.set(notificationRunning);
	}

	public boolean getNotificationPerformed() {
		return notificationPerformed.get();
	}

	public void setNotificationPerformed(boolean notificationPerformed) {
		this.notificationPerformed.set(notificationPerformed);
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}