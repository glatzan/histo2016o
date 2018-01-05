package org.histo.action.dialog.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.routines.EmailValidator;
import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.AbstractTabDialog;
import org.histo.action.dialog.AbstractTabDialog.AbstractTab;
import org.histo.action.handler.GlobalSettings;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.ContactDAO;
import org.histo.dao.FavouriteListDAO;
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
import org.histo.util.StreamUtils;
import org.histo.util.notification.NotificationContainer;
import org.histo.util.pdf.PDFGenerator;
import org.primefaces.context.RequestContext;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
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
public class NotificationDialog extends AbstractTabDialog {

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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	private MailTab mailTab;
	private FaxTab faxTab;
	private LetterTab letterTab;
	private PhoneTab phoneTab;
	private SendTab sendTab;
	private SendReportTab sendReportTab;

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
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		if (getTask() != task) {
			logger.debug("Resetting Bean");
			setMailTab(new MailTab());
			setFaxTab(new FaxTab());
			setLetterTab(new LetterTab());
			setPhoneTab(new PhoneTab());
			setSendTab(new SendTab());
			setSendReportTab(new SendReportTab());

			tabs = new AbstractTab[] { mailTab, faxTab, letterTab, phoneTab, sendTab, sendReportTab };
		}

		super.initBean(task, Dialog.NOTIFICATION);

		// resetting even resend settings
		sendTab.setReperformSend(false);

		return true;
	}

	public void nextStep() {
		logger.trace("Next step");
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] == selectedTab) {
				if (i <= tabs.length - 1)
					onTabChange(tabs[i]);

				break;
			}
		}
	}

	public void previousStep() {
		logger.trace("Previous step");
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] == selectedTab) {
				if (i >= 0)
					onTabChange(tabs[i]);
				break;
			}
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

	// public void initTabs() {
	//
	// mailTab.setInitialized(false);
	// mailTab.updateData();
	//
	// faxTab.setInitialized(false);
	// faxTab.updateData();
	//
	// letterTab.setInitialized(false);
	// letterTab.updateData();
	//
	// phoneTab.setInitialized(false);
	// phoneTab.updateData();
	//
	// sendTab.setInitialized(false);
	// sendTab.updateData();
	//
	// sendReportTab.updateData();
	// }

	@Getter
	@Setter
	public abstract class AbstractTab extends org.histo.action.dialog.AbstractTabDialog.AbstractTab {

		protected List<NotificationContainer> holders;

		protected boolean initialized;

		protected boolean useTab;

		protected List<DocumentTemplate> templateList;

		protected DefaultTransformer<DocumentTemplate> templateTransformer;

		protected DocumentTemplate selectedTemplate;

		protected NotificationTyp notificationTyp;

		public void updateList(List<AssociatedContact> contacts, NotificationTyp notificationTyp) {

			// list of all previously generated holders
			List<NotificationContainer> tmpHolders = new ArrayList<NotificationContainer>(getHolders());

			for (AssociatedContact associatedContact : contacts) {

				List<AssociatedContactNotification> notification = null;

				// holder is in list, remove holder from temporary list
				if ((notification = associatedContact.getNotificationTypAsList(notificationTyp, true)).size() != 0) {
					// there should only be one AssociatedContactNotification
					// for the given type
					try {
						NotificationContainer tmpHolder = tmpHolders.stream()
								.filter(p -> p.getContact().equals(associatedContact))
								.collect(StreamUtils.singletonCollector());
						// updating notification type
						tmpHolder.setNotification(notification.get(0));
						tmpHolders.remove(tmpHolder);
					} catch (IllegalStateException e) {
						// adding to list

						NotificationContainer holder = new NotificationContainer(associatedContact);
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
							default:
								// do nothing for e.g. print
								break;
							}
						} else {
							holder.setContactAddress(notification.get(0).getContactAddress());
						}

						getHolders().add(holder);
					}
				}

			}

			// removing old holders
			for (NotificationContainer contactHolder : tmpHolders) {
				getHolders().remove(contactHolder);
			}

			// sorting
			Collections.sort(getHolders(), (NotificationContainer p1, NotificationContainer p2) -> {
				if (p1.isFaildPreviously() == p2.isFaildPreviously()) {
					return 0;
				} else if (p1.isFaildPreviously()) {
					return 1;
				} else {
					return -1;
				}
			});
		}

		public void copySelectedPdf(NotificationContainer contactHolder) {
			if (dialogHandlerAction.getPrintDialog().getPdfContainer() != null) {
				logger.debug("Selecting pdf");
				contactHolder.setPdf(dialogHandlerAction.getPrintDialog().getPdfContainer());
			}
		}

		public void renewNotification(Task task, NotificationContainer contactHolder) {
			getHolders().remove(contactHolder);
			contactDAO.renewNotification(task, contactHolder.getContact(), contactHolder.getNotification());
			updateList(task.getContacts(), getNotificationTyp());
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
			setCenterInclude("include/mail.xhtml");

			setNotificationTyp(NotificationTyp.EMAIL);
		}

		@Override
		public boolean initTab() {
			// cleaing all holders
			setHolders(new ArrayList<NotificationContainer>());

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultEmailDocument()));

			DiagnosisReportMail mail = MailTemplate
					.getTemplateByID(globalSettings.getDefaultDocuments().getNotificationDefaultEmail());

			mail.prepareTemplate(task.getPatient(), task, null);
			mail.fillTemplate();

			setMail(mail);

			setMailSubject(mail.getSubject());
			setMailBody(mail.getBody());

			updateData();

			setUseTab(getHolders().size() > 0 ? true : false);

			logger.debug("Mail data initialized");
			return true;
		}

		@Override
		public void updateData() {
			logger.debug("Updating Mail data");
			updateList(task.getContacts(), getNotificationTyp());
		}

	}

	@Getter
	@Setter
	public class FaxTab extends AbstractTab {

		/**
		 * If true the program will send the fax
		 */
		private boolean autoSendFax;

		/**
		 * If true the fax pdf will be printet
		 */
		private boolean print;

		public FaxTab() {
			setTabName("FaxTab");
			setName("dialog.notification.tab.fax");
			setViewID("faxTab");
			setNotificationTyp(NotificationTyp.FAX);
			setCenterInclude("include/fax.xhtml");
		}

		@Override
		public boolean initTab() {
			// clearing all holders
			setHolders(new ArrayList<NotificationContainer>());

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultFaxDocument()));

			setAutoSendFax(true);

			setPrint(false);

			setUseTab(getHolders().size() > 0 ? true : false);

			logger.debug("Fax data initialized");
			return true;
		}

		@Override
		public void updateData() {
			logger.debug("Updating Fax data");
			updateList(task.getContacts(), getNotificationTyp());
		}

	}

	@Getter
	@Setter
	public class LetterTab extends AbstractTab {

		/**
		 * If true the pdfs will be printed
		 */
		private boolean print;

		public LetterTab() {
			setTabName("LetterTab");
			setName("dialog.notification.tab.letter");
			setViewID("letterTab");
			setCenterInclude("include/letter.xhtml");

			setNotificationTyp(NotificationTyp.LETTER);
		}

		@Override
		public boolean initTab() {
			// clearing all holders
			setHolders(new ArrayList<NotificationContainer>());
			updateList(task.getContacts(), getNotificationTyp());

			setPrint(true);

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultLetterDocument()));

			setUseTab(getHolders().size() > 0 ? true : false);

			logger.debug("Letter data initialized");
			return true;
		}

		@Override
		public void updateData() {
			logger.debug("Updating Letter data");
			updateList(task.getContacts(), getNotificationTyp());
		}

	}

	@Getter
	@Setter
	public class PhoneTab extends AbstractTab {

		public PhoneTab() {
			setTabName("PhoneTab");
			setName("dialog.notification.tab.phone");
			setViewID("phoneTab");
			setCenterInclude("include/phone.xhtml");

			setNotificationTyp(NotificationTyp.PHONE);
		}

		@Override
		public boolean initTab() {
			// clearing all holders
			setHolders(new ArrayList<NotificationContainer>());

			setUseTab(getHolders().size() > 0 ? true : false);

			logger.debug("Phone data initialized");
			return true;
		}

		@Override
		public void updateData() {
			logger.debug("Updating Phone data");
			updateList(task.getContacts(), getNotificationTyp());

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

		private int printCount;

		private List<NotificationContainer> currentMailHolders;
		private boolean failedMailHolders;
		private List<NotificationContainer> currentFaxHolders;
		private boolean failedFaxHolders;
		private List<NotificationContainer> currentLetterHolders;
		private boolean failedLetterHolders;

		private boolean reperformSend;

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.notification.tab.send");
			setViewID("sendTab");
			setCenterInclude("include/send.xhtml");

			setPrintCount(2);
		}

		// @Override
		// public String getCenterView() {
		// if (!isNotificationCompleted())
		// return "send/send.xhtml";
		// return "send/status.xhtml";
		// }

		@Override
		public void updateData() {

			if (!isInitialized()) {
				sendTab.setProgressPercent(0);
				sendTab.setProgressText("");
				setNotificationCompleted(false);
				setNotificationRunning(false);
				setProgressPercent(0);
				setProgressText("");
				setSteps(1);
				setLocale(FacesContext.getCurrentInstance().getViewRoot().getLocale());

				if (isReperformSend()) {
					setNotificationCompleted(false);
				} else {
					// notification has been performed
					if (getTask().getNotificationCompletionDate() != 0) {
						setNotificationCompleted(true);
					}
				}

				mailTab.setDisabled(sendTab.isNotificationCompleted());
				faxTab.setDisabled(sendTab.isNotificationCompleted());
				letterTab.setDisabled(sendTab.isNotificationCompleted());
				phoneTab.setDisabled(sendTab.isNotificationCompleted());
				// never disable
				sendTab.setDisabled(false);

				// if resend there is a sendreport, so the sendreport tab is
				// shown!
				if (!isReperformSend()) {
					sendReportTab.setDisabled(!sendTab.isNotificationCompleted());
				} else {
					sendReportTab.setDisabled(false);
				}

				// setActiveIndex(sendTab.isNotificationCompleted() ? 4 : 0);

				DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
						DocumentType.DIAGNOSIS_REPORT_EXTERN);

				setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

				setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

				setSelectedTemplate(DocumentTemplate.getTemplateByID(subSelect, 11));

				setInitialized(true);
			}

			if (!isNotificationCompleted()) {

				setFailedMailHolders(mailTab.getHolders().stream().anyMatch(p -> p.isFaildPreviously()));
				setCurrentMailHolders(
						mailTab.getHolders().stream().filter(p -> !p.isFaildPreviously()).collect(Collectors.toList()));

				for (NotificationContainer holder : getCurrentMailHolders()) {

					if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo(
								resourceBundle.get("dialog.notification.sendProcess.mail.error.noaddress"));
					} else if (!EmailValidator.getInstance().isValid(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo(
								resourceBundle.get("dialog.notification.sendProcess.mail.error.mailNotValid"));
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}

					if (holder.isWarning())
						logger.debug("Warning for Email (" + holder.getContact().getPerson().getFullName() + ") = "
								+ holder.getWarningInfo());
				}

				setFailedFaxHolders(faxTab.getHolders().stream().anyMatch(p -> p.isFaildPreviously()));
				setCurrentFaxHolders(
						faxTab.getHolders().stream().filter(p -> !p.isFaildPreviously()).collect(Collectors.toList()));

				for (NotificationContainer holder : getCurrentFaxHolders()) {
					if (faxTab.getSelectedTemplate() == null && holder.getPdf() == null) {
						holder.setWarning(true);
						holder.setWarningInfo(
								resourceBundle.get("dialog.notification.sendProcess.pdf.error.noTemplate"));
					} else if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo(resourceBundle.get("dialog.notification.sendProcess.fax.error.nonumber"));
					} else if (!holder.getContactAddress()
							.matches(globalSettings.getProgramSettings().getPhoneRegex())) {
						holder.setWarning(true);
						holder.setWarningInfo(
								resourceBundle.get("dialog.notification.sendProcess.fax.error.numberNotValid"));
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}

					if (holder.isWarning())
						logger.debug("Warning for Fax (" + holder.getContact().getPerson().getFullName() + ") = "
								+ holder.getWarningInfo());
				}

				setFailedLetterHolders(letterTab.getHolders().stream().anyMatch(p -> p.isFaildPreviously()));
				setCurrentLetterHolders(letterTab.getHolders().stream().filter(p -> !p.isFaildPreviously())
						.collect(Collectors.toList()));

				for (NotificationContainer holder : getCurrentLetterHolders()) {
					if (letterTab.getSelectedTemplate() == null && holder.getPdf() == null) {
						holder.setWarning(true);
						holder.setWarningInfo(
								resourceBundle.get("dialog.notification.sendProcess.pdf.error.noTemplate"));
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}
				}

				for (NotificationContainer holder : phoneTab.getHolders()) {
					if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {
						holder.setWarning(true);
						holder.setWarningInfo(
								resourceBundle.get("dialog.notification.sendProcess.phone.error.nonumber"));
					} else {
						holder.setWarning(false);
						holder.setWarningInfo("");
					}
				}
			}
		}

		public void reperformNotification() {
			setReperformSend(true);
			// initTabs();
			// setActiveIndex(0);
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

				progressStepText("dialog.notification.sendProcess.starting");

				if (mailTab.isUseTab()) {

					logger.debug("Mail is used");

					// ********************************
					DiagnosisReportMail mail = mailTab.getMail();
					mail.setSubject(mailTab.getMailSubject());
					mail.setBody(mailTab.getMailBody());
					// ********************************
					
					for (NotificationContainer holder : mailTab.getHolders()) {

						// failed and not renewed holders will not be executed
						// again
						if (holder.isFaildPreviously())
							continue;

						try {

							// copy contact address before sending -> save
							// before error
							holder.getNotification().setContactAddress(holder.getContactAddress());

							if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress()))
								throw new IllegalArgumentException(
										"dialog.notification.sendProcess.mail.error.noaddress");

							if (!EmailValidator.getInstance().isValid(holder.getContactAddress()))
								throw new IllegalArgumentException(
										"dialog.notification.sendProcess.mail.error.mailNotValid");

							logger.debug("Send mail to " + holder.getContactAddress());

							DiagnosisReportMail cloneMail = (DiagnosisReportMail) mail.clone();

							if (holder.getPdf() != null) {
								// pdf was selected for the individual
								// contact
								cloneMail.setAttachment(holder.getPdf());
								logger.debug("Attaching pdf to mail");
							} else if (mailTab.getSelectedTemplate() != null) {

								logger.debug("Creating PDF from selected template");

								progressStepText("dialog.notification.sendProcess.pdf.generating",
										holder.getContact().getPerson().getFullName());

								// generating pdf from list
								// Template has a TemplateDiagnosisReport
								// generator

								// generating the address field of the pdf
								String reportAddressField = AssociatedContact.generateAddress(holder.getContact());

								((TemplateDiagnosisReport) mailTab.getSelectedTemplate())
										.initData(getTask().getPatient(), getTask(), reportAddressField);

								PDFContainer container = mailTab.getSelectedTemplate().generatePDF(new PDFGenerator());

								if (container == null)
									throw new IllegalArgumentException(
											"dialog.notification.sendProcess.pdf.error.failed");

								cloneMail.setAttachment(container);
								holder.setPdf(container);
							}

							logger.debug("Sending mail to " + holder.getContactAddress());

							progressStepText("dialog.notification.sendProcess.mail.send", holder.getContactAddress());

							boolean success = false;

							if (!globalSettings.getProgramSettings().isOffline())
								success = globalSettings.getMailHandler().sendMail(holder.getContactAddress(), cloneMail);
							else {
								logger.debug("Offline mode, not sending email!");
								throw new IllegalArgumentException(
										"dialog.notification.sendProcess.mail.error.offline");
							}

							if (!success)
								throw new IllegalArgumentException("dialog.notification.sendProcess.mail.error.failed");

							holder.getNotification().setActive(false);
							holder.getNotification().setPerformed(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));

							progressStepText("dialog.notification.sendProcess.mail.success",
									holder.getContactAddress());

							logger.debug("Sending completed " + holder.getNotification().getCommentary());

						} catch (IllegalArgumentException e) {
							holder.getNotification().setPerformed(true);
							holder.getNotification().setFailed(true);
							holder.getNotification().setActive(true);
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							holder.getNotification()
									.setCommentary(resourceBundle.get(e.getMessage(), holder.getContactAddress()));
							progressStepText(e.getMessage(), holder.getContactAddress());
							logger.debug("Sending failed");
						}

						progressStep();
					}
				}

				if (faxTab.isUseTab()) {

					logger.debug("fax is used");

					for (NotificationContainer holder : faxTab.getHolders()) {

						// failed and not renewed holders will not be executed
						// again
						if (holder.isFaildPreviously())
							continue;

						try {

							// copy contact address before sending -> save
							// before error
							holder.getNotification().setContactAddress(holder.getContactAddress());

							progressStepText("dialog.notification.sendProcess.fax.send", holder.getContactAddress());

							if (!HistoUtil.isNotNullOrEmpty(holder.getContactAddress()))
								throw new IllegalArgumentException(
										"dialog.notification.sendProcess.fax.error.nonumber");

							if (!holder.getContactAddress()
									.matches(globalSettings.getProgramSettings().getPhoneRegex()))
								throw new IllegalArgumentException(
										"dialog.notification.sendProcess.fax.error.numberNotValid");

							// only generate pdf is no pdf was selected
							if (holder.getPdf() == null) {
								if (faxTab.getSelectedTemplate() == null)
									throw new IllegalArgumentException(
											"dialog.notification.sendProcess.pdf.error.noTemplate");

								progressStepText("dialog.notification.sendProcess.pdf.generating",
										holder.getContact().getPerson().getFullName());

								// generating the address field of the pdf
								String reportAddressField = AssociatedContact.generateAddress(holder.getContact());

								// generating pdf
								((TemplateDiagnosisReport) faxTab.getSelectedTemplate())
										.initData(getTask().getPatient(), getTask(), reportAddressField);
								PDFContainer container = ((TemplateDiagnosisReport) faxTab.getSelectedTemplate())
										.generatePDF(new PDFGenerator());

								if (container == null)
									throw new IllegalArgumentException(
											"dialog.notification.sendProcess.pdf.error.failed");

								holder.setPdf(container);
							}

							// offline mode
							if (globalSettings.getProgramSettings().isOffline()) {
								logger.debug("Offline mode, not sending email!");
								throw new IllegalArgumentException("dialog.notification.sendProcess.fax.error.offline");
							} else if (faxTab.isAutoSendFax()) {
								globalSettings.getFaxHandler().sendFax(holder.getContactAddress(), holder.getPdf());
								progressStepText("dialog.notification.sendProcess.fax.success");
							} else if (faxTab.isPrint()) {
								progressStepText("dialog.notification.sendProcess.pdf.print");
								userHandlerAction.getSelectedPrinter().print(holder.getPdf());
							}

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
							holder.getNotification().setCommentary(resourceBundle.get(e.getMessage()));
							progressStepText(e.getMessage());
						}
						progressStep();
					}
				}

				if (letterTab.isUseTab()) {
					logger.debug("letter is used");

					for (NotificationContainer holder : letterTab.getHolders()) {

						// failed and not renewed holders will not be executed
						// again
						if (holder.isFaildPreviously())
							continue;

						try {
							// TODO maybe dialog for adding address without
							// creating a pdf

							// only generate pdf if no pdf was selected
							if (holder.getPdf() == null) {
								if (letterTab.getSelectedTemplate() == null)
									throw new IllegalArgumentException(
											"dialog.notification.sendProcess.pdf.error.noTemplate");

								progressStepText("dialog.notification.sendProcess.pdf.generating",
										holder.getContact().getPerson().getFullName());

								// generating the address field of the pdf
								String reportAddressField = AssociatedContact.generateAddress(holder.getContact());

								((TemplateDiagnosisReport) letterTab.getSelectedTemplate())
										.initData(getTask().getPatient(), getTask(), reportAddressField);
								PDFContainer container = ((TemplateDiagnosisReport) letterTab.getSelectedTemplate())
										.generatePDF(new PDFGenerator());

								if (container == null)
									throw new IllegalArgumentException(
											"dialog.notification.sendProcess.pdf.error.failed");

								holder.setPdf(container);
							}

							if (letterTab.isPrint()) {
								progressStepText("dialog.notification.sendProcess.pdf.print");
								userHandlerAction.getSelectedPrinter().print(holder.getPdf());
							}

							holder.getNotification().setContactAddress(holder.getContactAddress());
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
							progressStepText(e.getMessage());
						}

						progressStep();
					}

				}

				if (phoneTab.isUseTab()) {
					logger.debug("Phone is used");
					for (NotificationContainer holder : phoneTab.getHolders()) {
						holder.getNotification().setActive(false);
						holder.getNotification().setPerformed(true);
						holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
						holder.getNotification().setContactAddress(holder.getContactAddress());
					}

					progressStep();
				}

				if (getSelectedTemplate() != null) {
					((TemplateDiagnosisReport) getSelectedTemplate()).initData(getTask().getPatient(), getTask(), "");
					PDFContainer report = getSelectedTemplate().generatePDF(new PDFGenerator());
					for (int i = 0; i < printCount; i++) {
						userHandlerAction.getSelectedPrinter().print(report, getSelectedTemplate().getAttributes());
					}
				}

				DocumentTemplate sendreportTemplate = DocumentTemplate
						.getDefaultTemplate(DocumentTemplate.getTemplates(DocumentType.NOTIFICATION_SEND_REPORT));

				progressStepText("dialog.notification.sendProcess.sendReport.generating");

				if (sendreportTemplate != null) {
					TemplateSendReport template = (TemplateSendReport) sendreportTemplate;

					template.initData(task.getPatient(), task, mailTab.isUseTab(), mailTab.getHolders(),
							faxTab.isUseTab(), faxTab.getHolders(), letterTab.isUseTab(), letterTab.getHolders(),
							phoneTab.isUseTab(), phoneTab.getHolders(), new Date(System.currentTimeMillis()));

					sendReport = template.generatePDF(new PDFGenerator());

					if (sendReport == null) {
						// TODO handle error
						progressStepText("dialog.notification.sendProcess.pdf.error.failed");
					}
				}

				setProgressPercent(100);

				progressStep();

				progressStepText("dialog.notification.sendProcess.success");

				logger.debug("Messaging ended");

				try {

					transactionTemplate.execute(new TransactionCallbackWithoutResult() {

						public void doInTransactionWithoutResult(TransactionStatus transactionStatus) {

							genericDAO.reattach(getTask().getPatient());

							logger.debug("Saving progress...");
							for (NotificationContainer holder : mailTab.getHolders()) {
								if (holder.isFaildPreviously())
									continue;
								logger.debug("Saving mail: " + holder.getContact().toString());
								genericDAO.save(holder.getContact());
							}

							for (NotificationContainer holder : faxTab.getHolders()) {
								if (holder.isFaildPreviously())
									continue;
								genericDAO.save(holder.getContact());
							}

							for (NotificationContainer holder : letterTab.getHolders()) {
								if (holder.isFaildPreviously())
									continue;
								genericDAO.save(holder.getContact());
							}

							for (NotificationContainer holder : phoneTab.getHolders()) {
								genericDAO.save(holder.getContact());
							}

							genericDAO.savePatientData(getTask(), "log.patient.task.notification.send");
						}
					});

				} catch (Exception e) {
					e.printStackTrace();
				}

				getTask().setNotificationCompletionDate(System.currentTimeMillis());

				pdfDAO.attachPDF(getTask().getPatient(), getTask(), sendReport);

				logger.debug("Saving progress, completed");

				// removing from diagnosis list
				favouriteListDAO.removeTaskFromList(getTask(), PredefinedFavouriteList.NotificationList,
						PredefinedFavouriteList.StayInNotificationList);

			} catch (Exception e) {
				e.printStackTrace();
			}

			setNotificationCompleted(true);

			setNotificationRunning(false);

			// updating data, loading sendreports
			mailTab.setDisabled(true);
			faxTab.setDisabled(true);
			letterTab.setDisabled(true);
			phoneTab.setDisabled(true);
			sendTab.setDisabled(false);
			sendReportTab.setDisabled(false);

			// unblocking gui and updating content
			RequestContext.getCurrentInstance().execute("PF('blockUIWidget').unblock();chagneTab(5);");
		}

		public int calculateSteps() {
			// one step for creating the send report
			int steps = 1;

			steps += mailTab.isUseTab() ? currentMailHolders.size() : 0;
			steps += faxTab.isUseTab() ? currentFaxHolders.size() : 0;
			steps += letterTab.isUseTab() ? currentLetterHolders.size() : 0;
			steps += phoneTab.isUseTab() ? 1 : 0;

			logger.debug("Steps calculated = " + steps);
			return steps;
		}

		public void progressStep() {
			setProgressPercent(getProgressPercent() + (100 / getSteps()));
		}

		public void progressStepText(String resourcesKey, Object... args) {
			setProgressText(resourceBundle.get(resourcesKey, args));
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

	@Getter
	@Setter
	public class SendReportTab extends AbstractTab {

		private DefaultTransformer<PDFContainer> sendReportConverter;

		public SendReportTab() {
			setTabName("SendReport");
			setName("dialog.notification.tab.sendReport");
			setViewID("sendReportTab");

			setCenterInclude("include/sendReport.xhtml");
		}

		@Override
		public void updateData() {
			// getting all sendreports
			List<PDFContainer> lists = PDFGenerator.getPDFsofType(task.getAttachedPdfs(),
					DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

			// updating mediadialog
			dialogHandlerAction.getMediaDialog().initiBeanForExternalView(lists,
					PDFGenerator.getLatestPDFofType(lists));

			setSendReportConverter(new DefaultTransformer<>(lists));

		}

	}
}
