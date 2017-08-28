package org.histo.action.dialog.notification;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.faces.context.FacesContext;

import org.apache.commons.validator.routines.EmailValidator;
import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.SettingsHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;
import org.histo.model.patient.Task;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.util.HistoUtil;
import org.histo.util.StreamUtils;
import org.histo.util.mail.MailHandler;
import org.histo.util.printer.template.AbstractTemplate;
import org.histo.util.printer.template.PDFGenerator;
import org.histo.util.printer.template.TemplateDiagnosisReport;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

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
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SettingsHandler settingsHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ThreadPoolTaskExecutor taskExecutor;

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

			// list of all previously generated holders
			List<ContactHolder> tmpHolders = new ArrayList<ContactHolder>(getHolders());

			for (AssociatedContact associatedContact : contacts) {

				List<AssociatedContactNotification> notification = null;

				// holder is in list, remove holder from temporary list
				if ((notification = associatedContact.getNotificationTypAsList(notificationTyp, false)).size() != 0) {
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

			// removing old holders
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
			private AssociatedContactNotification notification;

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
			if (!getSendTab().isNotificationCompleted())
				return "mail/mail.xhtml";
			return "send/status.xhtml";
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

		private boolean autoSendFax;

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
			if (!getSendTab().isNotificationCompleted())
				return "letter/letter.xhtml";
			return "send/status.xhtml";
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
			if (!getSendTab().isNotificationCompleted())
				return "phone/phone.xhtml";
			return "send/status.xhtml";
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

		private AtomicBoolean renderStepProgress;

		private int progressPercent;

		private String progressText;

		private int steps;

		private Locale locale;

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.notification.tab.send");
			setViewID("sendTab");
			setNotificationCompleted(false);
			setNotificationRunning(new AtomicBoolean(false));
			setRenderStepProgress(new AtomicBoolean(false));
			setProgressPercent(0);
			setProgressText("");
			setSteps(1);
			setLocale(FacesContext.getCurrentInstance().getViewRoot().getLocale());
		}

		@Override
		public String getCenterView() {
			if (!isNotificationCompleted())
				return "send/send.xhtml";
			return "send/status.xhtml";
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub
		}

		@Async("taskExecutor")
		public void performeNotification() {

			logger.debug("Startin notification thread");

			try {
				if (notificationRunning.get()) {
					logger.debug("Thread allready running, abort new request!");
					return;
				}

				notificationRunning.set(true);

				setSteps(calculateSteps());

				getRenderStepProgress().set(true);

				setProgressPercent(0);

				setProgressText(resourceBundle.get("pdf.notification.status.starting", locale));

				if (mailTab.isUseTab()) {

					logger.debug("Mail is used");

					DiagnosisReportMail mail = mailTab.getMail();
					mail.setSubject(mail.getSubject());
					mail.setBody(mail.getBody());

					for (ContactHolder holder : mailTab.getHolders()) {

						if (HistoUtil.isNotNullOrEmpty(holder.getContactAddress())
								&& EmailValidator.getInstance().isValid(holder.getContactAddress())) {
							logger.debug("Send mail to " + holder.getContactAddress());

							DiagnosisReportMail cloneMail = (DiagnosisReportMail) mail.clone();
							if (holder.getPdf() != null) {
								// pdf was selected for the individual
								// contact
								cloneMail.setAttachment(holder.getPdf());
								logger.debug("Attaching pdf to mail");
							} else if (mailTab.getSelectedTemplate() != null) {

								logger.debug("Creating PDF from selected template");
								setProgressText(resourceBundle.get("pdf.notification.status.generatingPDF", locale,
										holder.getContact().getPerson().getFullName()));
								// generating pdf from list
								// Template has a TemplateDiagnosisReport
								// generator

								((TemplateDiagnosisReport) mailTab.getSelectedTemplate())
										.initData(getTask().getPatient(), getTask(), holder.getContact());

								PDFGenerator t = new PDFGenerator();
								PDFContainer container = mailTab.getSelectedTemplate().generatePDF(t);
								cloneMail.setAttachment(container);
								holder.setPdf(container);
							}

							setProgressText(
									resourceBundle.get("pdf.notification.status.sendMail", locale, holder.getContactAddress()));

							boolean success = false;
							// // success =
							// //
							// settingsHandler.getMailHandler().sendMail(holder.contactAddress,
							// // cloneMail);

							if (success) {
								holder.setPerformed(true);
								holder.getNotification().setPerformed(true);
								holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
								setProgressText(resourceBundle.get("pdf.notification.status.sendMail.success", locale,
										holder.getContactAddress()));
							} else {
								holder.getNotification().setFailed(true);
								holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
								holder.getNotification().setCommentary(resourceBundle
										.get("pdf.notification.status.sendMail.failed", locale, holder.getContactAddress()));
								setProgressText(resourceBundle.get("pdf.notification.status.sendMail.failed", locale,
										holder.getContactAddress()));
							}

						} else {
							logger.debug("No email provided!");

							// no email provided
							holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
							holder.getNotification()
									.setCommentary(resourceBundle.get("pdf.notification.status.sendMail.noMail", locale)); // resourceBundle.get("")
							setProgressText(resourceBundle.get("pdf.notification.status.sendMail.noMail", locale));
						}

						progressStep();
					}
				}

				if (faxTab.isUseTab()) {

					logger.debug("fax is used");

					for (ContactHolder holder : faxTab.getHolders()) {

						if (faxTab.getSelectedTemplate() != null
								|| HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {

							((TemplateDiagnosisReport) faxTab.getSelectedTemplate()).initData(getTask().getPatient(),
									getTask(), holder.getContact());
							PDFContainer container = ((TemplateDiagnosisReport) faxTab.getSelectedTemplate())
									.generatePDF(new PDFGenerator());

							if (container != null) {
								settingsHandler.getFaxHandler().sendFax(holder.getContactAddress(), container);
								holder.setPerformed(true);
								holder.getNotification().setPerformed(true);
								holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
								progressStep();
								continue;
							}

						}
						// no template or no number
						holder.setPerformed(false);
						holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
						holder.getNotification().setCommentary(resourceBundle.get("hallo", locale));
						progressStep();
					}
				}

				if (letterTab.isUseTab()) {
					logger.debug("letter is used");
					for (ContactHolder holder : letterTab.getHolders()) {
						if (letterTab.getSelectedTemplate() != null
								|| HistoUtil.isNotNullOrEmpty(holder.getContactAddress())) {

							((TemplateDiagnosisReport) letterTab.getSelectedTemplate()).initData(getTask().getPatient(),
									getTask(), holder.getContact());
							PDFContainer container = ((TemplateDiagnosisReport) letterTab.getSelectedTemplate())
									.generatePDF(new PDFGenerator());

							if (container != null) {
								settingsHandler.getFaxHandler().sendFax(holder.getContactAddress(), container);
								holder.setPerformed(true);
								holder.getNotification().setPerformed(true);
								holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
								progressStep();
								continue;
							}
						}

						// no template or no number
						holder.setPerformed(false);
						holder.getNotification().setDateOfAction(new Date(System.currentTimeMillis()));
						holder.getNotification().setCommentary(resourceBundle.get("hallo", locale));
						progressStep();

					}
				}

				if (phoneTab.isUseTab()) {
					logger.debug("Phone is used");
					progressStep();
				}

				// TODO generating send report
				
				progressStep();

				setProgressText("Notification completed");

				logger.debug("Messaging ended");
			} catch (Exception e) {
				System.out.println(e);
				e.printStackTrace();
			}

			notificationRunning.set(false);

			setNotificationCompleted(true);

			getRenderStepProgress().set(false);
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

		/**
		 * Increment steps
		 */
		public void progressStep() {
			setProgressPercent(getProgressPercent() + (100 / getSteps()));
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

	}
}
