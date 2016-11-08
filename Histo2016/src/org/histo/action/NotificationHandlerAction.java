package org.histo.action;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.histo.config.HistoSettings;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactMethod;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.Notification;
import org.histo.config.enums.NotificationOption;
import org.histo.dao.GenericDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.experimental.NotificationHandler;
import org.histo.model.PDFContainer;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.model.transitory.PdfTemplate;
import org.histo.ui.NotificationChooser;
import org.histo.util.MailUtil;
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
	private ThreadPoolTaskExecutor taskExecutor;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private PdfHandlerAction pdfHandlerAction;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private TaskDAO taskDAO;

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

	private String emailSubject;

	private String emailText;

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

	private PDFContainer manualNotifyList;

	/********************************************************
	 * Notification
	 ********************************************************/

	public void showNotificationDialog(Task task) {

		setTmpTask(task);

		setNotificationEmailList(NotificationChooser.getSublist(task.getContacts(), ContactMethod.EMAIL));
		setNotificationFaxList(NotificationChooser.getSublist(task.getContacts(), ContactMethod.FAX));
		setNotificationFaxList(NotificationChooser.getSublist(task.getContacts(), ContactMethod.PHONE));

		setNotificationTab(Notification.EMAIL);

		onAttachPdfToEmailChange();

		mainHandlerAction.showDialog(Dialog.NOTIFICATION);
	}

	public void hideNotificatonDialog() {
		// setNotifications(null);
		mainHandlerAction.hideDialog(Dialog.NOTIFICATION);
	}

	public void onAttachPdfToEmailChange() {
		for (NotificationChooser notificationChooser : notificationEmailList) {
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

	public void showPreviewDialog() {
		showPreviewDialog(null);
	}

	public void showPreviewDialog(NotificationChooser chooser) {
		setCustomPdfToPhysician(chooser);

		taskDAO.initializePdfData(getTmpTask());

		if (getNotificationTab() == Notification.EMAIL) {

			PDFContainer container = getTmpTask().getReport(PdfTemplate.INTERNAL);

			if (container != null) {
				pdfHandlerAction.prepareForAttachedPdf(getTmpTask(), container);
			} else {
				pdfHandlerAction.prepareForPdf(getTmpTask(), PdfTemplate.INTERNAL);
			}

		} else if (getNotificationTab() == Notification.FAX) {
			PDFContainer container = getTmpTask().getReportByName(
					chooser.getContact().getPhysician().getFullName().replace(" ", "_").replace(".", ""));

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
				if (getCustomPdfToPhysician().getNotificationAttachment() == NotificationOption.PDF)
					getCustomPdfToPhysician().setPdf(pdfHandlerAction.getTmpPdfContainer());
			}
		}

		// pdfHandlerAction.clearData();
	}

	private int test = 1;

	public void updateTest() {
	}

	public void sendTest() {

		// System.out.println(taskExecutor);
		// NotificationHandler test = new NotificationHandler(this, genericDAO);
		// test.setName("was geht");
		//
		// taskExecutor.execute(test);
		// // FileUtil.loadTextFile(null);

		// System.out.println("ok");
		// SimpleEmail email = new SimpleEmail();
		// email.setHostName("smtp.ukl.uni-freiburg.de");
		// email.setDebug(true);
		// email.setSmtpPort(465);
		// email.setSSLOnConnect(true);
		// try {
		// email.addTo("andreas.glatz@uniklinik-freiburg.de");
		// email.setFrom("augenklinik.histologie@uniklinik-freiburg.de", "Name
		// des Senders");
		// email.setSubject("Testnachricht");
		// email.setMsg("Hallo, das ist nur ein simpler Test");
		// email.send();
		// } catch (EmailException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }

	}

	@Async("taskExecutor")
	public void performeNotification() {
		if (notificationRunning.get())
			return;
		notificationRunning.set(true);

		if (isUseFax() || isUsePhone() || isUseEmail()) {

			if (isUseEmail()) {
				for (NotificationChooser notificationChooser : notificationEmailList) {
					if (notificationChooser.getNotificationAttachment() == NotificationOption.NONE)
						continue;
					else if (notificationChooser.getNotificationAttachment() == NotificationOption.PDF
							&& notificationChooser.getPdf() != null) {
						MailUtil.sendMail(notificationChooser.getContact().getPhysician().getEmail(),
								HistoSettings.EMAIL_FROM, HistoSettings.EMAIL_FROM_NAME, getEmailSubject(),
								getEmailText(), notificationChooser.getPdf());
					} else {
						MailUtil.sendMail(notificationChooser.getContact().getPhysician().getEmail(),
								HistoSettings.EMAIL_FROM, HistoSettings.EMAIL_FROM_NAME, getEmailSubject(),
								getEmailText());
					}
				}
				// TODO 
			}
			
			if(isUseFax()){
				for (NotificationChooser notificationChooser : notificationEmailList) {
//					mergePdfs
				}
			}
			
			while (true) {
				test++;
				System.out.println("updating");
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}

		notificationRunning.set(false);
	}

	public int getTest() {
		return test;
	}

	public void setTest(int test) {
		this.test = test;
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

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

}