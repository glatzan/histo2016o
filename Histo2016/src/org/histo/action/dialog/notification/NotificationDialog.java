package org.histo.action.dialog.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.xml.ws.Holder;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.print.PrintDialog;
import org.histo.action.handler.PDFGeneratorHandler;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.NotificationOption;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Task;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.ui.medicalFindings.MedicalFindingsChooser;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.StreamUtils;
import org.histo.util.mail.MailHandler;
import org.histo.util.printer.template.AbstractTemplate;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.Async;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class NotificationDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsHandler settingsHandler;

	private int activeIndex = 0;

	private MailTab mailTab = new MailTab();

	public AbstractTab[] tabs = new AbstractTab[] { mailTab, new FaxTab(), new LetterTab(), new PhoneTab(),
			new SendTab() };

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.NOTIFICATION);

		System.out.println(task);

		setActiveIndex(0);

		onTabChange(null);

		return true;
	}

	public void onTabChange(TabChangeEvent event) {
		if (getActiveIndex() >= 0 && getActiveIndex() < getTabs().length) {
			logger.debug("Updating Tab with index " + getActiveIndex());
			getTabs()[getActiveIndex()].updateData();
		}
	}

	public AbstractTab getTab(String tabName) {
		for (AbstractTab abstractSettingsTab : tabs) {
			if (abstractSettingsTab.getTabName().equals(tabName))
				return abstractSettingsTab;
		}

		return null;
	}

	public void nextStep() {
		logger.trace("Next step");
		if (getActiveIndex() < getTabs().length) {
			setActiveIndex(getActiveIndex() + 1);
			getTabs()[getActiveIndex()].updateData();
		}
	}

	public void previousStep() {
		logger.trace("Previous step");
		if (getActiveIndex() > 0) {
			setActiveIndex(getActiveIndex() - 1);
			getTabs()[getActiveIndex()].updateData();
		}
	}

	public void openSelectPDFDialog(Task task, AssociatedContact contact) {

		AbstractTemplate[] subSelect = AbstractTemplate.getTemplatesByTypes(
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		dialogHandlerAction.getPrintDialog().initBeanForSelecting(task, subSelect, subSelect[0],
				new AssociatedContact[] { contact }, true);
		dialogHandlerAction.getPrintDialog().setSingleAddressSelectMode(true);
		dialogHandlerAction.getPrintDialog().prepareDialog();
	}

	public void openMediaViewDialog(PDFContainer container) {

		HasDataList dataList = new HasDataList() {

			private List<PDFContainer> attachedPdfs = new ArrayList<PDFContainer>();

			@Override
			public long getId() {
				return 0;
			}

			@Override
			public void setAttachedPdfs(List<PDFContainer> attachedPdfs) {
				this.attachedPdfs = attachedPdfs;
			}

			@Override
			public String getDatalistIdentifier() {
				return "";
			}

			@Override
			public List<PDFContainer> getAttachedPdfs() {
				return attachedPdfs;
			}
		};

		dataList.getAttachedPdfs().add(container);

		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(getTask().getPatient(), new HasDataList[] { dataList }, dataList,
				container, false, false);

		// setting info text
		dialogHandlerAction.getMediaDialog()
				.setActionDescription(resourceBundle.get("dialog.media.headline.info.council", getTask().getTaskID()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}

	@Getter
	@Setter
	public abstract class AbstractTab {

		public abstract String getCenterView();

		public abstract void updateData();

		protected List<ContactHolder> holders;

		protected String name;

		protected String viewID;

		protected String tabName;

		protected boolean initialized;

		protected boolean useTab;

		protected List<AbstractTemplate> templateList;

		protected DefaultTransformer<AbstractTemplate> templateTransformer;

		protected AbstractTemplate selectedTemplate;

		public void updateList(List<AssociatedContact> contacts, NotificationTyp notificationTyp) {

			List<ContactHolder> tmpHolders = new ArrayList<ContactHolder>(getHolders());

			for (AssociatedContact associatedContact : contacts) {
				if (associatedContact.containsNotificationTyp(notificationTyp)) {
					try {
						ContactHolder tmpHolder = tmpHolders.stream()
								.filter(p -> p.getContact().equals(associatedContact))
								.collect(StreamUtils.singletonCollector());
						tmpHolders.remove(tmpHolder);
					} catch (IllegalStateException e) {
						// adding to list

						ContactHolder holder = new ContactHolder(associatedContact);

						switch (notificationTyp) {
						case EMAIL:
							holder.setContactAddress(associatedContact.getPerson().getContact().getEmail());
							break;
						case FAX:
							holder.setContactAddress(associatedContact.getPerson().getContact().getFax());
							break;
						case PHONE:
							holder.setContactAddress(associatedContact.getPerson().getContact().getPhone());
							break;
						case LETTER:

							break;
						}

						getHolders().add(holder);
					}
				}

			}

			for (ContactHolder contactHolder : tmpHolders) {
				getHolders().remove(contactHolder);
			}
		}

		public void copySelectedPdf(ContactHolder contactHolder) {
			if (dialogHandlerAction.getPrintDialog().getPdfContainer() != null) {
				logger.debug("Selecting pdf");
				contactHolder.setPdf(dialogHandlerAction.getPrintDialog().getPdfContainer());
			}
		}

		@Getter
		@Setter
		public class ContactHolder {
			private AssociatedContact contact;
			private PDFContainer pdf;
			private String contactAddress;
			private NotificationTyp notificationTyp;
			private boolean performed;

			public ContactHolder(AssociatedContact contact) {
				this.contact = contact;
			}
		}

	}

	@Getter
	@Setter
	public class MailTab extends AbstractTab {

		private String mailSubject;

		private String mailBody;

		private DiagnosisReportMail mail;

		public MailTab() {
			setTabName("MailTab");
			setName("dialog.notification.tab.mail");
			setViewID("mailTab");
			setHolders(new ArrayList<ContactHolder>());

			AbstractTemplate[] subSelect = AbstractTemplate.getTemplatesByTypes(
					new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN });

			setTemplateList(new ArrayList<AbstractTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<AbstractTemplate>(getTemplateList()));

			setSelectedTemplate(null);
		}

		@Override
		public String getCenterView() {
			return "mail/mail.xhtml";
		}

		@Override
		public void updateData() {

			updateList(task.getContacts(), NotificationTyp.EMAIL);

			if (!isInitialized()) {
				DiagnosisReportMail mail = MailHandler.getDefaultTemplate(DiagnosisReportMail.class);
				mail.prepareTemplate(task.getPatient(), task, null);
				mail.fillTemplate();

				setMail(mail);

				setMailSubject(mail.getSubject());
				setMailBody(mail.getBody());

				setUseTab(getHolders().size() > 0 ? true : false);

				setInitialized(true);
				logger.debug("Mails initialized");
			}
		}
	}

	@Getter
	@Setter
	public class FaxTab extends AbstractTab {

		public FaxTab() {
			setTabName("FaxTab");
			setName("dialog.notification.tab.fax");
			setViewID("faxTab");
			setHolders(new ArrayList<ContactHolder>());

			AbstractTemplate[] subSelect = AbstractTemplate.getTemplatesByTypes(
					new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN });

			setTemplateList(new ArrayList<AbstractTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<AbstractTemplate>(getTemplateList()));

			setSelectedTemplate(null);
		}

		@Override
		public String getCenterView() {
			return "fax/fax.xhtml";
		}

		@Override
		public void updateData() {
			updateList(task.getContacts(), NotificationTyp.FAX);

			if (!isInitialized()) {

				setUseTab(getHolders().size() > 0 ? true : false);

				setInitialized(true);
				logger.debug("Fax initialized");
			}

		}

	}

	@Getter
	@Setter
	public class LetterTab extends AbstractTab {

		public LetterTab() {
			setTabName("LetterTab");
			setName("dialog.notification.tab.letter");
			setViewID("letterTab");
			setHolders(new ArrayList<ContactHolder>());

			AbstractTemplate[] subSelect = AbstractTemplate.getTemplatesByTypes(
					new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN });

			setTemplateList(new ArrayList<AbstractTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<AbstractTemplate>(getTemplateList()));

			setSelectedTemplate(null);
		}

		@Override
		public String getCenterView() {
			return "letter/letter.xhtml";
		}

		@Override
		public void updateData() {

			updateList(task.getContacts(), NotificationTyp.LETTER);

			if (!isInitialized()) {
				setUseTab(getHolders().size() > 0 ? true : false);

				setInitialized(true);

				logger.debug("Letter initialized");
			}

		}

	}

	@Getter
	@Setter
	public class PhoneTab extends AbstractTab {

		public PhoneTab() {
			setTabName("PhoneTab");
			setName("dialog.notification.tab.phone");
			setViewID("phoneTab");
			setHolders(new ArrayList<ContactHolder>());
		}

		@Override
		public String getCenterView() {
			return "phone/phone.xhtml";
		}

		@Override
		public void updateData() {

			updateList(task.getContacts(), NotificationTyp.PHONE);

			if (!isInitialized()) {
				setUseTab(getHolders().size() > 0 ? true : false);

				setInitialized(true);

				logger.debug("Phone initialized");
			}

		}

	}

	@Getter
	@Setter
	public class SendTab extends AbstractTab {

		private boolean notificationCompleted;

		private AtomicBoolean notificationRunning;

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.notification.tab.send");
			setViewID("sendTab");
			setNotificationCompleted(false);
			setNotificationRunning(new AtomicBoolean(false));
		}

		@Override
		public String getCenterView() {
			return "send/send.xhtml";
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub
		}

		@Async("taskExecutor")
		public void performeNotification() {
			try {
				if (notificationRunning.get())
					return;

				notificationRunning.set(true);

				logger.trace("Startin notification thread");

				if (mailTab.isUseTab()) {
					DiagnosisReportMail mail = mailTab.getMail();
					mail.setSubject(mail.getSubject());
					mail.setBody(mail.getBody());

					for (ContactHolder holder : mailTab.getHolders()) {
						if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
							DiagnosisReportMail cloneMail = (DiagnosisReportMail) mail.clone();

							if (holder.getPdf() != null)
								// pdf was selected for the individual contact
								cloneMail.setAttachment(holder.getPdf());
							else if (mailTab.getSelectedTemplate() != null) {
								// template was selected for pdf
								
							}
							
							boolean success = settingsHandler.getMailHandler().sendMail(holder.contactAddress,
									cloneMail);
							holder.setPerformed(success);
						}
					}
				}

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
								// PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(
								// getTemporaryTask().getPatient(), getTemporaryTask(),
								// notificationChooser.getPrintTemplate(), notificationChooser);
								// TODO: rewrite
								// if (mainHandlerAction.getSettings().getMail().sendMailFromSystem(
								// notificationChooser.getContact().getPerson().getContact().getEmail(),
								// emailNotificationSettings.getEmailSubject(),
								// emailNotificationSettings.getEmailText(), pdfToSend)) {
								// emailSuccessful = true;
								// // adding mail to the result array
								//// resultPdfs.add(pdfToSend);
								// logger.trace("PDF successfully send");
								// } else {
								// // TODO: HAndle fault
								// }

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
								// PDFContainer pdfToSend = pDFGeneratorHandler.generatePDFForReport(
								// getTemporaryTask().getPatient(), getTemporaryTask(),
								// notificationChooser.getPrintTemplate(),
								// notificationChooser.getContact().getPerson());
								// resultPdfs.add(pdfToSend);

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
							getTemporaryTask().getPatient(), getEmailNotificationSettings(),
							getFaxNotificationSettings(), getPhoneNotificationSettings());

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
	}
}
