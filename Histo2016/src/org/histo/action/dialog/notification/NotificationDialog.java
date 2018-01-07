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
import org.histo.model.user.HistoPermissions;
import org.histo.service.NotificationService;
import org.histo.service.TaskService;
import org.histo.template.DocumentTemplate;
import org.histo.template.MailTemplate;
import org.histo.template.documents.TemplateDiagnosisReport;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.StreamUtils;
import org.histo.util.notification.FaxExecutor;
import org.histo.util.notification.MailContainer;
import org.histo.util.notification.MailContainerList;
import org.histo.util.notification.MailExecutor;
import org.histo.util.notification.NotificationContainer;
import org.histo.util.notification.NotificationContainerList;
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

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskService taskService;

	
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
		return initBean(task, false);
	}

	public boolean initBean(Task task, boolean resend) {
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
		} else if (resend) {
			mailTab.setDisabled(false);
			faxTab.setDisabled(false);
			letterTab.setDisabled(false);
			phoneTab.setDisabled(false);
			sendTab.setDisabled(false);
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

		protected NotificationContainerList containerList;

		protected boolean initialized;

		protected List<DocumentTemplate> templateList;

		protected DefaultTransformer<DocumentTemplate> templateTransformer;

		public void copySelectedPdf(NotificationContainer contactHolder) {
			if (dialogHandlerAction.getPrintDialog().getPdfContainer() != null) {
				logger.debug("Selecting pdf");
				contactHolder.setPdf(dialogHandlerAction.getPrintDialog().getPdfContainer());
			}
		}

		/**
		 * Updates the notification container list and if at least one notification for
		 * this task should be performed useTab is set to true
		 */
		@Override
		public void updateData() {
			logger.debug("Updating tab " + containerList.getNotificationTyp());
			containerList.updateList(task.getContacts(), containerList.getNotificationTyp());
			containerList.setUse(containerList.getContainer().size() > 0);
			setInitialized(true);
		}
	}

	@Getter
	@Setter
	public class MailTab extends NotificationTab {

		private String mailSubject;

		private String mailBody;

		public MailTab() {
			setTabName("MailTab");
			setName("dialog.notification.tab.mail");
			setViewID("mailTab");
			setCenterInclude("include/mail.xhtml");
		}

		@Override
		public boolean initTab() {
			setContainerList(new MailContainerList(NotificationTyp.EMAIL));

			getContainerList().setSend(true);

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			getContainerList().setDefaultReport((TemplateDiagnosisReport) DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultEmailDocument()));

			DiagnosisReportMail mail = MailTemplate
					.getTemplateByID(globalSettings.getDefaultDocuments().getNotificationDefaultEmail());

			mail.prepareTemplate(task.getPatient(), task, null);
			mail.fillTemplate();

			((MailContainerList) getContainerList()).setSelectedMail(mail);

			setMailSubject(mail.getSubject());
			setMailBody(mail.getBody());

			logger.debug("Mail data initialized");
			return true;
		}
	}

	@Getter
	@Setter
	public class FaxTab extends NotificationTab {

		public FaxTab() {
			setTabName("FaxTab");
			setName("dialog.notification.tab.fax");
			setViewID("faxTab");
			setCenterInclude("include/fax.xhtml");
		}

		@Override
		public boolean initTab() {
			setContainerList(new NotificationContainerList(NotificationTyp.FAX));

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			getContainerList().setDefaultReport((TemplateDiagnosisReport) DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultFaxDocument()));

			getContainerList().setSend(true);

			getContainerList().setPrint(false);

			logger.debug("Fax data initialized");
			return true;
		}
	}

	@Getter
	@Setter
	public class LetterTab extends NotificationTab {

		public LetterTab() {
			setTabName("LetterTab");
			setName("dialog.notification.tab.letter");
			setViewID("letterTab");
			setCenterInclude("include/letter.xhtml");
		}

		@Override
		public boolean initTab() {
			setContainerList(new NotificationContainerList(NotificationTyp.LETTER));

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			getContainerList().setDefaultReport((TemplateDiagnosisReport) DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultLetterDocument()));

			getContainerList().setPrint(true);

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
		}

		@Override
		public boolean initTab() {
			setContainerList(new NotificationContainerList(NotificationTyp.PHONE));

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

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.notification.tab.send");
			setViewID("sendTab");
			setCenterInclude("include/send.xhtml");
		}

		@Override
		public boolean initTab() {
			// clearing all holders
			setNotificationCompleted(false);
			setNotificationRunning(false);
			setProgressPercent(0);
			setFeedbackText("");
			setSteps(1);

			setContainerList(new NotificationContainerList(NotificationTyp.PRINT));

			DocumentTemplate[] subSelect = DocumentTemplate.getTemplates(DocumentType.DIAGNOSIS_REPORT,
					DocumentType.DIAGNOSIS_REPORT_EXTERN);

			setTemplateList(new ArrayList<DocumentTemplate>(Arrays.asList(subSelect)));

			setTemplateTransformer(new DefaultTransformer<DocumentTemplate>(getTemplateList()));

			getContainerList().setDefaultReport((TemplateDiagnosisReport) DocumentTemplate.getTemplateByID(subSelect,
					globalSettings.getDefaultDocuments().getNotificationDefaultPrintDocument()));

			getContainerList().setPrintCount(2);

			setInitialized(true);
			return true;
		}

		@Override
		public void updateData() {
			logger.debug("Update Data send");

			// mail
			if (!mailTab.isInitialized())
				mailTab.updateData();

			MailExecutor mailExecutor = new MailExecutor(null);
			mailTab.getContainerList().renewNotifications(getTask());
			mailTab.getContainerList().getContainerToNotify().forEach(p -> {
				if (!mailExecutor.isAddressApproved(p.getContactAddress()))
					p.setWarning(true, "dialog.notification.sendProcess.mail.error.mailNotValid");
				else
					p.clearWarning();
			});

			// fax
			if (!faxTab.isInitialized())
				faxTab.updateData();

			FaxExecutor faxExecutor = new FaxExecutor(null);
			faxTab.getContainerList().renewNotifications(getTask());
			faxTab.getContainerList().getContainerToNotify().forEach(p -> {
				if (!faxExecutor.isAddressApproved(p.getContactAddress()))
					p.setWarning(true, "dialog.notification.sendProcess.fax.error.numberNotValid");
				else
					p.clearWarning();
			});

			// letters
			if (!letterTab.isInitialized())
				letterTab.updateData();

			letterTab.getContainerList().renewNotifications(getTask());

			// phone
			if (!phoneTab.isInitialized())
				phoneTab.updateData();

			phoneTab.getContainerList().renewNotifications(getTask());
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

				// progressStepText("dialog.notification.sendProcess.starting");

				// copy mail text
				((MailContainerList) mailTab.getContainerList()).getSelectedMail().setSubject(mailTab.getMailSubject());
				((MailContainerList) mailTab.getContainerList()).getSelectedMail().setBody(mailTab.getMailBody());

				notificationService.executeNotification(this, getTask(), (MailContainerList) mailTab.getContainerList(),
						faxTab.getContainerList(), getLetterTab().getContainerList(), phoneTab.getContainerList(),
						this.getContainerList());

				setProgressPercent(100);

				// progressStepText("dialog.notification.sendProcess.success");

				logger.debug("Messaging ended");

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

			steps += mailTab.getContainerList().isUse()
					? mailTab.getContainerList().getContainer().stream().filter(p -> p.isPerform()).count()
					: 0;
			steps += faxTab.getContainerList().isUse()
					? faxTab.getContainerList().getContainer().stream().filter(p -> p.isPerform()).count()
					: 0;
			steps += letterTab.getContainerList().isUse()
					? letterTab.getContainerList().getContainer().stream().filter(p -> p.isPerform()).count()
					: 0;
			steps += phoneTab.getContainerList().isUse() ? 1 : 0;

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

		private boolean archiveTask;
		
		public SendReportTab() {
			setTabName("SendReport");
			setName("dialog.notification.tab.sendReport");
			setViewID("sendReportTab");
			setCenterInclude("include/sendReport.xhtml");
			
			if(userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT_ARCHIVE))
				setArchiveTask(true);
		}

		@Override
		public void updateData() {
			logger.debug("Update Data sendReport");
			// getting all sendreports
			List<PDFContainer> lists = PDFGenerator.getPDFsofType(task.getAttachedPdfs(),
					DocumentType.MEDICAL_FINDINGS_SEND_REPORT_COMPLETED);

			// updating mediadialog
			dialogHandlerAction.getMediaDialog().initiBeanForExternalView(lists,
					PDFGenerator.getLatestPDFofType(lists));

			setSendReportConverter(new DefaultTransformer<>(lists));

		}

		public void repeatNotification() {
			initBean(getTask(), true);
			onTabChange(mailTab);
		}
		
		public void endNotification() {
			notificationService.endNotificationPhase(getTask());

			if(archiveTask)
				taskService.archiveTask(getTask());
		}

	}
}
