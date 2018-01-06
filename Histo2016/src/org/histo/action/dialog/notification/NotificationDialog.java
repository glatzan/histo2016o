package org.histo.action.dialog.notification;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.routines.EmailValidator;
import org.histo.action.DialogHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractTabDialog;
import org.histo.action.handler.GlobalSettings;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
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
import org.histo.service.NotificationService;
import org.histo.template.DocumentTemplate;
import org.histo.template.MailTemplate;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.StreamUtils;
import org.histo.util.notification.FaxExecutor;
import org.histo.util.notification.MailContainer;
import org.histo.util.notification.MailExecutor;
import org.histo.util.notification.NotificationContainer;
import org.histo.util.notification.NotificationFeedback;
import org.histo.util.pdf.PDFGenerator;
import org.primefaces.context.RequestContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private NotificationService notificationService;

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

		setMailTab(new MailTab());
		setFaxTab(new FaxTab());
		setLetterTab(new LetterTab());
		setPhoneTab(new PhoneTab());
		setSendTab(new SendTab());
		setSendReportTab(new SendReportTab());

		tabs = new AbstractTab[] { mailTab, faxTab, letterTab, phoneTab, sendTab, sendReportTab };

		// disabling tabs if notification was performed
		if (task.getNotificationCompletionDate() != 0) {
			mailTab.setDisabled(true);
			faxTab.setDisabled(true);
			letterTab.setDisabled(true);
			phoneTab.setDisabled(true);
			sendTab.setDisabled(true);
			sendReportTab.setDisabled(false);
		} else {
			mailTab.setDisabled(false);
			faxTab.setDisabled(false);
			letterTab.setDisabled(false);
			phoneTab.setDisabled(false);
			sendTab.setDisabled(false);
			sendReportTab.setDisabled(true);
		}

		super.initBean(task, Dialog.NOTIFICATION);

		return true;
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

	@Getter
	@Setter
	public abstract class NotificationTab extends AbstractTab {

		protected List<NotificationContainer> holders;

		protected boolean initialized;

		protected boolean individualAddresses;

		protected boolean useTab;

		protected List<DocumentTemplate> templateList;

		protected DefaultTransformer<DocumentTemplate> templateTransformer;

		protected DocumentTemplate selectedTemplate;

		protected NotificationTyp notificationTyp;

		/**
		 * Method updates the contact list for the given contact type. It will not
		 * overwrite changes by the user.
		 * 
		 * @param contacts
		 * @param notificationTyp
		 */
		public void updateList(List<AssociatedContact> contacts, NotificationTyp notificationTyp) {

			// copie the list of current containers with notifications
			List<NotificationContainer> tmpContainers = new ArrayList<NotificationContainer>(getHolders());

			for (AssociatedContact associatedContact : contacts) {

				// getting the notification from the contact selected by the notification type
				List<AssociatedContactNotification> notification = associatedContact
						.getNotificationTypAsList(notificationTyp, true);

				// if there is no notification of this type do nothing, there should be only 1
				// notification pending of the given type, if there are more ignore the rest
				if (notification.size() != 0) {
					try {
						// searching if the notification is already in the list, updating
						NotificationContainer tmpContainer = tmpContainers.stream()
								.filter(p -> p.getContact().equals(associatedContact))
								.collect(StreamUtils.singletonCollector());
						// updating notification type
						tmpContainer.setNotification(notification.get(0));
						tmpContainer.setFaildPreviously(notification.get(0).isFailed());
						tmpContainers.remove(tmpContainer);
					} catch (IllegalStateException e) {
						// not in list creating a new notification
						NotificationContainer holder = (notificationTyp == NotificationTyp.EMAIL
								? new MailContainer(associatedContact, notification.get(0))
								: new NotificationContainer(associatedContact, notification.get(0)));

						holder.initAddressForNotificationType();
						getHolders().add(holder);
					}
				}
			}

			// container which are still in the tmpContainers array were removed, so delete
			// them from the current container list
			for (NotificationContainer contactContainer : tmpContainers)
				getHolders().remove(contactContainer);

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

		/**
		 * Renews all active notifications if they failed before
		 * 
		 * @param task
		 * @param contactHolder
		 */
		public void renewNotifications() {
			for (Iterator<NotificationContainer> it = getHolders().iterator(); it.hasNext();) {
				NotificationContainer container = it.next();
				if (container.isFaildPreviously() && container.isPerform()) {
					contactDAO.renewNotification(getTask(), container.getContact(), container.getNotification());
				}
			}

			updateData();
		}

		/**
		 * Updates the notification container list and if at least one notification for
		 * this task should be performed useTab is set to true
		 */
		@Override
		public void updateData() {
			logger.debug("Updating tab " + getNotificationTyp());
			updateList(task.getContacts(), getNotificationTyp());
			setUseTab(getHolders().size() > 0 ? true : false);
			setInitialized(true);
		}
	}

	@Getter
	@Setter
	public class MailTab extends NotificationTab {

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

			logger.debug("Mail data initialized");
			return true;
		}
	}

	@Getter
	@Setter
	public class FaxTab extends NotificationTab {

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

			logger.debug("Fax data initialized");
			return true;
		}
	}

	@Getter
	@Setter
	public class LetterTab extends NotificationTab {

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

			logger.debug("Letter data initialized");
			return true;
		}
	}

	@Getter
	@Setter
	public class PhoneTab extends NotificationTab {

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

			logger.debug("Phone data initialized");
			return true;
		}
	}

	@Getter
	@Setter
	public class SendTab extends NotificationTab implements NotificationFeedback {

		private boolean notificationCompleted;

		private boolean notificationRunning;

		private int progressPercent;

		private String feedbackText;

		private int steps;

		private int printCount;

		private List<NotificationContainer> currentMailHolders;
		private List<NotificationContainer> currentFaxHolders;
		private List<NotificationContainer> currentLetterHolders;
		private List<NotificationContainer> currentPhoneHolders;

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.notification.tab.send");
			setViewID("sendTab");
			setCenterInclude("include/send.xhtml");

			setPrintCount(2);
		}

		@Override
		public boolean initTab() {
			// clearing all holders
			setNotificationCompleted(false);
			setNotificationRunning(false);
			setProgressPercent(0);
			setFeedbackText("");
			setSteps(1);

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			setSelectedTemplate(DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultPrintDocument()));

			setInitialized(true);
			return true;
		}

		@Override
		public void updateData() {

			// mail
			if (!mailTab.isInitialized())
				mailTab.updateData();

			MailExecutor mailExecutor = new MailExecutor(null);
			// renews notifications
			mailTab.renewNotifications();
			System.out.println(mailTab.getHolders().size());
			List<NotificationContainer> mailContainers = mailTab.getHolders().stream().filter(p -> p.isPerform())
					.collect(Collectors.toList());

			mailContainers.forEach(p -> {
				if (!mailExecutor.isAddressApproved(p.getContactAddress())) {
					p.setWarning(true);
					p.setWarningInfo(resourceBundle.get("dialog.notification.sendProcess.mail.error.mailNotValid"));
				}
			});

			setCurrentMailHolders(mailContainers);

			// fax
			if (!faxTab.isInitialized())
				faxTab.updateData();

			FaxExecutor faxExecutor = new FaxExecutor(null);
			// renews notifications
			faxTab.renewNotifications();
			List<NotificationContainer> faxContainers = faxTab.getHolders().stream().filter(p -> p.isPerform())
					.collect(Collectors.toList());

			faxContainers.forEach(p -> {
				if (!faxExecutor.isAddressApproved(p.getContactAddress())) {
					p.setWarning(true);
					p.setWarningInfo(resourceBundle.get("dialog.notification.sendProcess.fax.error.numberNotValid"));
				}
			});

			setCurrentFaxHolders(faxContainers);

			// letters
			if (!letterTab.isInitialized())
				letterTab.updateData();

			letterTab.renewNotifications();
			List<NotificationContainer> letterContainers = letterTab.getHolders().stream().filter(p -> p.isPerform())
					.collect(Collectors.toList());
			setCurrentLetterHolders(letterContainers);

			// phone
			if (!phoneTab.isInitialized())
				phoneTab.updateData();

			List<NotificationContainer> phoneContainers = phoneTab.getHolders().stream().filter(p -> p.isPerform())
					.collect(Collectors.toList());
			setCurrentPhoneHolders(phoneContainers);
		}

		// @Async("taskExecutor")
		public void performeNotification() {

			logger.debug("Startin notification thread");

			try { 
				if (isNotificationRunning()) {
					logger.debug("Thread allready running, abort new request!");
					return;
				}

				setNotificationRunning(true);
				
				setSteps(calculateSteps());

//				progressStepText("dialog.notification.sendProcess.starting");

				if (mailTab.isUseTab()) {

					DiagnosisReportMail mail = mailTab.getMail();
					mail.setSubject(mailTab.getMailSubject());
					mail.setBody(mailTab.getMailBody());

					notificationService.executeMailNotification(this, getTask(), getCurrentMailHolders(), mail,
							(TemplateDiagnosisReport) mailTab.getSelectedTemplate(), mailTab.isIndividualAddresses());

				}

				// process to step 25

				if (faxTab.isUseTab()) {
					notificationService.executeFaxNotification(this, getTask(), getCurrentFaxHolders(),
							(TemplateDiagnosisReport) faxTab.getSelectedTemplate(), faxTab.isIndividualAddresses(),
							faxTab.isAutoSendFax(), faxTab.isPrint());
				}

				if (letterTab.isUseTab()) {
					notificationService.executeLetterNotification(this, getTask(), getCurrentLetterHolders(),
							(TemplateDiagnosisReport) letterTab.getSelectedTemplate(),
							letterTab.isIndividualAddresses(), letterTab.isPrint());
				}

				if (phoneTab.isUseTab()) {
					notificationService.executePhoneNotification(this, getTask(), getCurrentPhoneHolders());
				}

				// addition templates
				if (getSelectedTemplate() != null) {
					((TemplateDiagnosisReport) getSelectedTemplate()).initData(getTask().getPatient(), getTask(), "");
					PDFContainer report = (new PDFGenerator())
							.getPDF(((TemplateDiagnosisReport) getSelectedTemplate()));

					userHandlerAction.getSelectedPrinter().print(report, 1, getSelectedTemplate().getAttributes());

				}

				PDFContainer sendReport = notificationService.generateSendReport(getTask(), mailTab.isUseTab(), getCurrentMailHolders(),
						faxTab.isUseTab(), getCurrentLetterHolders(), letterTab.isUseTab(), getCurrentLetterHolders(),
						phoneTab.isUseTab(), getCurrentPhoneHolders(), new Date(System.currentTimeMillis()));

				setProgressPercent(100);

				//progressStepText("dialog.notification.sendProcess.success");

				logger.debug("Messaging ended");

				notificationService.saveContactsOfTask(getTask(), sendReport);

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
			sendTab.setDisabled(true);
			sendReportTab.setDisabled(false);

			// unblocking gui and updating content
			RequestContext.getCurrentInstance().execute("onNotificationCompleted();");
		}

		public int calculateSteps() {
			// one step for creating the send report
			int steps = 1;

			steps += mailTab.isUseTab() ? currentMailHolders.stream().filter(p -> p.isPerform()).count() : 0;
			steps += faxTab.isUseTab() ? currentFaxHolders.stream().filter(p -> p.isPerform()).count() : 0;
			steps += letterTab.isUseTab() ? currentLetterHolders.stream().filter(p -> p.isPerform()).count() : 0;
			steps += phoneTab.isUseTab() ? 1 : 0;

			return steps;
		}

		/**
		 * Progresses one step
		 */
		public void progressStep() {
			setSteps(getSteps() + 1);
			setProgressPercent(getProgressPercent() + (100 / getSteps()));
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

		@Synchronized
		public void setFeedback(String resKey, String... string) {
			setFeedbackText(resourceBundle.get(resKey, string));
		}

		@Synchronized
		public String getFeedback() {
			return getFeedbackText();
		}

	}

	@Getter
	@Setter
	public class SendReportTab extends NotificationTab {

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
