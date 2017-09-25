package org.histo.action.dialog.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.routines.EmailValidator;
import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.notification.ContactDialog.ContactHolder;
import org.histo.action.dialog.print.PrintDialog;
import org.histo.action.handler.GlobalSettings;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PdfDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Task;
import org.histo.template.DocumentTemplate;
import org.histo.template.MailTemplate;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.documents.TemplateSendReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.PDFGenerator;
import org.histo.util.StreamUtils;
import org.primefaces.context.RequestContext;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallbackWithoutResult;
import org.springframework.transaction.support.TransactionTemplate;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.Synchronized;

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
	private PdfDAO pdfDAO;

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
	private GlobalSettings globalSettings;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TransactionTemplate transactionTemplate;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ContactDAO contactDAO;

	private int activeIndex = 0;

	private MailTab mailTab;

	private FaxTab faxTab;

	private LetterTab letterTab;

	private PhoneTab phoneTab;

	private SendTab sendTab;

	public AbstractTab[] tabs = new AbstractTab[5];

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

		if (getTask() != task) {
			logger.debug("Resetting Bean");
			tabs[0] = mailTab = new MailTab();
			tabs[1] = faxTab = new FaxTab();
			tabs[2] = letterTab = new LetterTab();
			tabs[3] = phoneTab = new PhoneTab();
			tabs[4] = sendTab = new SendTab();
		}

		super.initBean(task, Dialog.NOTIFICATION);

		setActiveIndex(0);

		initTabs();

		// notification has been performed
		if (task.getNotificationCompletionDate() != 0) {
			sendTab.setNotificationCompleted(true);
		}

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

		DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
				DocumentType.DIAGNOSIS_REPORT_EXTERN);

		dialogHandlerAction.getPrintDialog().initBeanForSelecting(task, subSelect, subSelect[0],
				new AssociatedContact[] { contact }, true);
		dialogHandlerAction.getPrintDialog().setSingleAddressSelectMode(true);
		dialogHandlerAction.getPrintDialog().setFaxMode(false);
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

	public void initTabs() {
		mailTab.setInitialized(false);
		mailTab.setHolders(new ArrayList<NotificationDialog.AbstractTab.ContactHolder>());
		mailTab.updateData();

		faxTab.setInitialized(false);
		faxTab.setHolders(new ArrayList<NotificationDialog.AbstractTab.ContactHolder>());
		faxTab.updateData();

		letterTab.setInitialized(false);
		letterTab.setHolders(new ArrayList<NotificationDialog.AbstractTab.ContactHolder>());
		letterTab.updateData();

		phoneTab.setInitialized(false);
		phoneTab.setHolders(new ArrayList<NotificationDialog.AbstractTab.ContactHolder>());
		phoneTab.updateData();

		sendTab.setNotificationCompleted(false);
		sendTab.setProgressPercent(0);
		sendTab.setProgressText("");
		sendTab.updateData();
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

		protected List<DocumentTemplate> templateList;

		protected DefaultTransformer<DocumentTemplate> templateTransformer;

		protected DocumentTemplate selectedTemplate;

		protected NotificationTyp notificationTyp;

		public void updateList(List<AssociatedContact> contacts, NotificationTyp notificationTyp) {

			// list of all previously generated holders
			List<ContactHolder> tmpHolders = new ArrayList<ContactHolder>(getHolders());

			for (AssociatedContact associatedContact : contacts) {

				List<AssociatedContactNotification> notification = null;

				// holder is in list, remove holder from temporary list
				if ((notification = associatedContact.getNotificationTypAsList(notificationTyp, true)).size() != 0) {
					// there should only be one AssociatedContactNotification
					// for the given type
					try {
						ContactHolder tmpHolder = tmpHolders.stream()
								.filter(p -> p.getContact().equals(associatedContact))
								.collect(StreamUtils.singletonCollector());
						// updating notification type
						tmpHolder.setNotification(notification.get(0));
						tmpHolders.remove(tmpHolder);
					} catch (IllegalStateException e) {
						// adding to list

						ContactHolder holder = new ContactHolder(associatedContact);
						holder.setNotification(notification.get(0));
						holder.setFaildPreviously(notification.get(0).isFailed());

						if (!notification.get(0).isFailed()) {
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
						}

						getHolders().add(holder);
					}
				}

			}

			// removing old holders
			for (ContactHolder contactHolder : tmpHolders) {
				getHolders().remove(contactHolder);
			}

			// sorting
			Collections.sort(getHolders(), (ContactHolder p1, ContactHolder p2) -> {
				if (p1.isFaildPreviously() == p2.isFaildPreviously()) {
					return 0;
				} else if (p1.isFaildPreviously()) {
					return 1;
				} else {
					return -1;
				}
			});
		}

		public void copySelectedPdf(ContactHolder contactHolder) {
			if (dialogHandlerAction.getPrintDialog().getPdfContainer() != null) {
				logger.debug("Selecting pdf");
				contactHolder.setPdf(dialogHandlerAction.getPrintDialog().getPdfContainer());
			}
		}

		public void renewNotification(Task task, ContactHolder contactHolder) {
			getHolders().remove(contactHolder);
			contactDAO.renewNotification(task, contactHolder.getContact(), contactHolder.getNotification());
			updateList(task.getContacts(), getNotificationTyp());
		}

		@Getter
		@Setter
		public class ContactHolder {

			private AssociatedContact contact;
			private AssociatedContactNotification notification;

			private PDFContainer pdf;
			private String contactAddress;
			private NotificationTyp notificationTyp;

			/**
			 * True if the notification failed on a previous notification attemt
			 */
			private boolean faildPreviously;
			/**
			 * True if e.g. the address is not correct
			 */
			private boolean warning;
			/**
			 * Warning text
			 */
			private String warningInfo;

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
			setNotificationTyp(NotificationTyp.EMAIL);

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(null);
		}

		@Override
		public String getCenterView() {
			if (!getSendTab().isNotificationCompleted())
				return "mail/mail.xhtml";
			return "send/status.xhtml";
		}

		@Override
		public void updateData() {

			updateList(task.getContacts(), getNotificationTyp());

			if (!isInitialized()) {
				DiagnosisReportMail mail = MailTemplate.getDefaultTemplate(DiagnosisReportMail.class);
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

		private boolean autoSendFax;

		public FaxTab() {
			setTabName("FaxTab");
			setName("dialog.notification.tab.fax");
			setViewID("faxTab");
			setHolders(new ArrayList<ContactHolder>());
			setNotificationTyp(NotificationTyp.FAX);

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(null);

			setAutoSendFax(true);
		}

		@Override
		public String getCenterView() {
			if (!getSendTab().isNotificationCompleted())
				return "fax/fax.xhtml";
			return "send/status.xhtml";
		}

		@Override
		public void updateData() {
			updateList(task.getContacts(), getNotificationTyp());

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
			setNotificationTyp(NotificationTyp.LETTER);
			setHolders(new ArrayList<ContactHolder>());

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(null);
		}

		@Override
		public String getCenterView() {
			if (!getSendTab().isNotificationCompleted())
				return "letter/letter.xhtml";
			return "send/status.xhtml";
		}

		@Override
		public void updateData() {

			updateList(task.getContacts(), getNotificationTyp());

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
			setNotificationTyp(NotificationTyp.PHONE);
		}

		@Override
		public String getCenterView() {
			if (!getSendTab().isNotificationCompleted())
				return "phone/phone.xhtml";
			return "send/status.xhtml";
		}

		@Override
		public void updateData() {

			updateList(task.getContacts(), getNotificationTyp());

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

		private boolean notificationRunning;

		private int progressPercent;

		private String progressText;

		private int steps;

		private Locale locale;

		private DefaultTransformer<PDFContainer> sendReportConverter;

		private List<ContactHolder> currentMailHolders;
		private boolean failedMailHolders;
		private List<ContactHolder> currentFaxHolders;
		private boolean failedFaxHolders;
		private List<ContactHolder> currentLetterHolders;
		private boolean failedLetterHolders;

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.notification.tab.send");
			setViewID("sendTab");
			setNotificationCompleted(false);
			setNotificationRunning(false);
			setProgressPercent(0);
			setProgressText("");
			setSteps(1);
			setLocale(FacesContext.getCurrentInstance().getViewRoot().getLocale());
		}

		@Override
		public String getCenterView() {
			if (!isNotificationCompleted())
				return "send/send.xhtml";
			return "send/sendReport.xhtml";
		}

		@Override
		public void updateData() {
			if (!isNotificationCompleted()) {

				setFailedMailHolders(mailTab.getHolders().stream().anyMatch(p -> p.isFaildPreviously()));
				setCurrentMailHolders(
						mailTab.getHolders().stream().filter(p -> !p.isFaildPreviously()).collect(Collectors.toList()));

				for (ContactHolder holder : getCurrentMailHolders()) {

					if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo("Keine Email angegeben");
					} else if (!EmailValidator.getInstance().isValid(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo("Emailadresse ist nicht gült!");
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}

					if (holder.isWarning())
						logger.debug("Warning for Email (" + holder.getContact().getPerson().getFullName() + ") = "
								+ holder.warningInfo);
				}

				setFailedFaxHolders(faxTab.getHolders().stream().anyMatch(p -> p.isFaildPreviously()));
				setCurrentFaxHolders(
						faxTab.getHolders().stream().filter(p -> !p.isFaildPreviously()).collect(Collectors.toList()));

				for (ContactHolder holder : getCurrentFaxHolders()) {
					if (faxTab.getSelectedTemplate() == null && holder.getPdf() == null) {
						holder.setWarning(true);
						holder.setWarningInfo("Kein Pdf auswählt");
					} else if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo("Keine Nummer angegeben");
					} else if (!holder.getContactAddress()
							.matches(globalSettings.getProgramSettings().getPhoneRegex())) {
						holder.setWarning(true);
						holder.setWarningInfo("Nummer nicht gültig");
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}

					if (holder.isWarning())
						logger.debug("Warning for Fax (" + holder.getContact().getPerson().getFullName() + ") = "
								+ holder.warningInfo);
				}

				setFailedLetterHolders(letterTab.getHolders().stream().anyMatch(p -> p.isFaildPreviously()));
				setCurrentLetterHolders(letterTab.getHolders().stream().filter(p -> !p.isFaildPreviously())
						.collect(Collectors.toList()));

				for (ContactHolder holder : getCurrentLetterHolders()) {
					if (letterTab.getSelectedTemplate() == null && holder.getPdf() == null) {
						holder.setWarning(true);
						holder.setWarningInfo("Kein Pdf auswählt");
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}
				}

				for (ContactHolder holder : phoneTab.getHolders()) {
					if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo("Keine Telefonunummer angegeben");
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}
				}
			} else {

				// getting all sendreports
				List<PDFContainer> lists = PDFGenerator.getPDFsofType(task.getAttachedPdfs(),
						DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

				// updating mediadialog
				dialogHandlerAction.getMediaDialog().initiBeanForExternalView(lists,
						PDFGenerator.getLatestPDFofType(lists));
				
				setSendReportConverter(new DefaultTransformer<>(lists));
			}
		}

		public void reperformNotification() {
			this.setNotificationCompleted(false);
			initTabs();
		}

		// @Async("taskExecutor")
		public void performeNotification() {

			logger.debug("Startin notification thread");

			try {
				if (isNotificationRunning()) {
					logger.debug("Thread allready running, abort new request!");
					return;
				}

				PDFContainer sendReport = null;

				setNotificationRunning(true);

				setSteps(calculateSteps());

				setProgressPercent(0);

				setProgressText(resourceBundle.get("pdf.notification.status.starting", locale));

				if (mailTab.isUseTab()) {

					logger.debug("Mail is used");

					DiagnosisReportMail mail = mailTab.getMail();
					mail.setSubject(mail.getSubject());
					mail.setBody(mail.getBody());

					for (ContactHolder holder : mailTab.getHolders()) {

						// failed and not renewed holders will not be executed
						// again
						if (holder.isFaildPreviously())
							continue;

						try {

							// holder.getNotification().setCommentary("");
							// holder.setPerformed(false);
							// holder.getNotification().setPerformed(false);
							// holder.getNotification().setFailed(false);

							if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress()))
								throw new IllegalArgumentException("pdf.notification.status.sendMail.error.noMail");

							if (!EmailValidator.getInstance().isValid(holder.getContactAddress()))
								throw new IllegalArgumentException(
										"pdf.notification.status.sendMail.error.mailNotValid");

							logger.debug("Send mail to " + holder.getContactAddress());

							DiagnosisReportMail cloneMail = (DiagnosisReportMail) mail.clone();

							if (holder.getPdf() != null) {
								// pdf was selected for the individual
								// contact
								cloneMail.setAttachment(holder.getPdf());
								logger.debug("Attaching pdf to mail");
							} else if (mailTab.getSelectedTemplate() != null) {

								logger.debug("Creating PDF from selected template");

								setProgressText(resourceBundle.get("pdf.notification.status.pdf.generating", locale,
										holder.getContact().getPerson().getFullName()));

								// generating pdf from list
								// Template has a TemplateDiagnosisReport
								// generator

								((TemplateDiagnosisReport) mailTab.getSelectedTemplate())
										.initData(getTask().getPatient(), getTask(), holder.getContact());

								PDFContainer container = mailTab.getSelectedTemplate().generatePDF(new PDFGenerator());

								if (container == null)
									throw new IllegalArgumentException("pdf.notification.status.pdf.noTemplate");

								cloneMail.setAttachment(container);
								holder.setPdf(container);
							}

							logger.debug("Sending mail to " + holder.getContactAddress());

							setProgressText(resourceBundle.get("pdf.notification.status.sendMail.send", locale,
									holder.getContactAddress()));

							boolean success = false;

							if (!globalSettings.getProgramSettings().isOffline())
								success = globalSettings.getMailHandler().sendMail(holder.contactAddress, cloneMail);
							else {
								logger.debug("Offline mode, not sending email!");
								throw new IllegalArgumentException("pdf.notification.status.sendMail.error.offline");
							}

							if (!success)
								throw new IllegalArgumentException("pdf.notification.status.sendMail.error.failed");

							holder.getNotification().setActive(false);
							holder.getNotification().setPerformed(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							progressStep(resourceBundle.get("pdf.notification.status.sendMail.success", locale,
									holder.getContactAddress()));

							logger.debug("Sending completed " + holder.getNotification().getCommentary());

						} catch (IllegalArgumentException e) {
							holder.getNotification().setPerformed(true);
							holder.getNotification().setFailed(true);
							holder.getNotification().setActive(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							holder.getNotification().setCommentary(
									resourceBundle.get(e.getMessage(), locale, holder.getContactAddress()));
							progressStep(resourceBundle.get(e.getMessage(), locale, holder.getContactAddress()));
							logger.debug("Sending failed");
						}

					}
				}

				if (faxTab.isUseTab()) {

					logger.debug("fax is used");

					for (ContactHolder holder : faxTab.getHolders()) {

						// failed and not renewed holders will not be executed
						// again
						if (holder.isFaildPreviously())
							continue;

						try {

							setProgressText(resourceBundle.get("pdf.notification.status.sendFax.send", locale,
									holder.getContactAddress()));

							if (faxTab.getSelectedTemplate() == null)
								throw new IllegalArgumentException("pdf.notification.status.pdf.noTemplate");

							if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress()))
								throw new IllegalArgumentException("pdf.notification.status.sendFax.error.noNumber");

							if (!holder.getContactAddress()
									.matches(globalSettings.getProgramSettings().getPhoneRegex()))
								throw new IllegalArgumentException(
										"pdf.notification.status.sendFax.error.numberNotValid");

							setProgressText(resourceBundle.get("pdf.notification.status.pdf.generating", locale,
									holder.getContact().getPerson().getFullName()));

							// generating pdf
							((TemplateDiagnosisReport) faxTab.getSelectedTemplate()).initData(getTask().getPatient(),
									getTask(), holder.getContact());
							PDFContainer container = ((TemplateDiagnosisReport) faxTab.getSelectedTemplate())
									.generatePDF(new PDFGenerator());

							if (container == null)
								throw new IllegalArgumentException("pdf.notification.status.pdf.pdfError");

							// offline mode
							if (!globalSettings.getProgramSettings().isOffline())
								globalSettings.getFaxHandler().sendFax(holder.getContactAddress(), container);
							else {
								logger.debug("Offline mode, not sending email!");
								throw new IllegalArgumentException("pdf.notification.status.sendFax.error.offline");
							}

							holder.setPdf(container);
							holder.getNotification().setActive(false);
							holder.getNotification().setPerformed(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							progressStep(resourceBundle.get("pdf.notification.status.sendFax.success", locale,
									holder.getContactAddress()));

							progressStep();
						} catch (IllegalArgumentException e) {
							// no template or no number
							holder.getNotification().setPerformed(true);
							holder.getNotification().setFailed(true);
							holder.getNotification().setActive(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							holder.getNotification().setCommentary(resourceBundle.get(e.getMessage(), locale));
							progressStep(resourceBundle.get(e.getMessage(), locale));
						}
					}
				}

				if (letterTab.isUseTab()) {
					logger.debug("letter is used");

					for (ContactHolder holder : letterTab.getHolders()) {

						// failed and not renewed holders will not be executed
						// again
						if (holder.isFaildPreviously())
							continue;

						try {
							if (faxTab.getSelectedTemplate() == null)
								throw new IllegalArgumentException("pdf.notification.status.pdf.noTemplate");

							if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress()))
								throw new IllegalArgumentException(
										"pdf.notification.status.sendLetter.error.noAddress");

							setProgressText(resourceBundle.get("pdf.notification.status.pdf.generating", locale,
									holder.getContact().getPerson().getFullName()));

							((TemplateDiagnosisReport) letterTab.getSelectedTemplate()).initData(getTask().getPatient(),
									getTask(), holder.getContact());
							PDFContainer container = ((TemplateDiagnosisReport) letterTab.getSelectedTemplate())
									.generatePDF(new PDFGenerator());

							if (container == null)
								throw new IllegalArgumentException("pdf.notification.status.pdf.pdfError");

							holder.setPdf(container);
							holder.getNotification().setActive(false);
							holder.getNotification().setPerformed(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));

							progressStep();
						} catch (IllegalArgumentException e) {
							// no template or no number
							holder.getNotification().setPerformed(true);
							holder.getNotification().setFailed(true);
							holder.getNotification().setActive(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							holder.getNotification().setCommentary(e.getMessage());
							progressStep(resourceBundle.get(e.getMessage(), locale));
						}

					}

				}

				if (phoneTab.isUseTab()) {
					logger.debug("Phone is used");
					for (ContactHolder holder : phoneTab.getHolders()) {
						holder.getNotification().setActive(false);
						holder.getNotification().setPerformed(true);
						holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
					}

					progressStep();
				}

				DocumentTemplate sendreportTemplate = DocumentTemplate
						.getDefaultTemplate(DocumentTemplate.getTemplates(DocumentType.NOTIFICATION_SEND_REPORT));

				if (sendreportTemplate != null) {
					TemplateSendReport template = (TemplateSendReport) sendreportTemplate;

					template.initData(task.getPatient(), task, mailTab.isUseTab(), mailTab.getHolders(),
							faxTab.isUseTab(), faxTab.getHolders(), letterTab.isUseTab(), letterTab.getHolders(),
							phoneTab.isUseTab(), phoneTab.getHolders(), new Date(System.currentTimeMillis()));

					sendReport = template.generatePDF(new PDFGenerator());

					// TODO error
				}

				progressStep();

				setProgressPercent(100);
				setProgressText(resourceBundle.get("pdf.notification.status.completed", locale));

				logger.debug("Messaging ended");

				try {

					transactionTemplate.execute(new TransactionCallbackWithoutResult() {

						public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

							genericDAO.reattach(getTask().getPatient());

							logger.debug("Saving progress...");
							for (ContactHolder holder : mailTab.getHolders()) {
								if (holder.isFaildPreviously())
									continue;
								logger.debug("Saving mail: " + holder.getContact().toString());
								genericDAO.save(holder.getContact());
							}

							for (ContactHolder holder : faxTab.getHolders()) {
								if (holder.isFaildPreviously())
									continue;
								genericDAO.save(holder.getContact());
							}

							for (ContactHolder holder : letterTab.getHolders()) {
								if (holder.isFaildPreviously())
									continue;
								genericDAO.save(holder.getContact());
							}

							for (ContactHolder holder : phoneTab.getHolders()) {
								genericDAO.save(holder.getContact());
							}

						}
					});

				} catch (Exception e) {
					e.printStackTrace();
				}

				getTask().setNotificationCompletionDate(System.currentTimeMillis());

				pdfDAO.attachPDF(getTask(), sendReport);

				logger.debug("Saving progress, completed");

			} catch (Exception e) {
				e.printStackTrace();
			}

			setNotificationCompleted(true);

			setNotificationRunning(false);

			// updating data, loading sendreports
			updateData();

			// unblocking gui and updating content
			RequestContext.getCurrentInstance().execute("PF('blockUIWidget').unblock();updateContent();");
		}

		public int calculateSteps() {
			// one step for creating the send report
			int steps = 1;

			steps += mailTab.isUseTab() ? mailTab.getHolders().size() : 0;
			steps += faxTab.isUseTab() ? faxTab.getHolders().size() : 0;
			steps += letterTab.isUseTab() ? letterTab.getHolders().size() : 0;
			steps += phoneTab.isUseTab() ? 1 : 0;

			logger.debug("Steps calculated = " + steps);
			return steps;
		}

		public void progressStep() {
			progressStep(null);
		}

		/**
		 * Increment steps
		 */
		public void progressStep(String message) {
			setProgressPercent(getProgressPercent() + (100 / getSteps()));
			if (message != null)
				setProgressText(message);

			logger.debug("Setting Progress to " + getProgressPercent());
		}

		@Synchronized
		public int getProgressPercent() {
			return progressPercent;
		}

		@Synchronized
		public void setProgressPercent(int progressPercent) {
			this.progressPercent = progressPercent;
		}

		@Synchronized
		public String getProgressText() {
			return progressText;
		}

		@Synchronized
		public void setProgressText(String progressText) {
			this.progressText = progressText;
		}

		@Synchronized
		public boolean isNotificationRunning() {
			return notificationRunning;
		}

		@Synchronized
		public void setNotificationRunning(boolean notificationRunning) {
			this.notificationRunning = notificationRunning;
		}

		@Synchronized
		public boolean isNotificationCompleted() {
			return notificationCompleted;
		}

		@Synchronized
		public void setNotificationCompleted(boolean notificationCompleted) {
			this.notificationCompleted = notificationCompleted;
		}

	}
}
