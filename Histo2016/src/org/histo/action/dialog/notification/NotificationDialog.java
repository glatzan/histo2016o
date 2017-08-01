package org.histo.action.dialog.notification;

import java.util.ArrayList;
import java.util.List;

import javax.xml.ws.Holder;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.print.PrintDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.AssociatedContact;
import org.histo.model.AssociatedContactNotification.NotificationTyp;
import org.histo.model.patient.Task;
import org.histo.template.mail.DiagnosisReportMail;
import org.histo.util.StreamUtils;
import org.histo.util.mail.MailHandler;
import org.histo.util.printer.template.AbstractTemplate;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

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
	private PrintDialog printDialog;

	private int activeIndex = 0;

	public AbstractTab[] tabs = new AbstractTab[] { new MailTab(), new FaxTab(), new LetterTab(), new SendTab() };

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
		if (getActiveIndex() < 2)
			setActiveIndex(getActiveIndex() + 1);
	}

	public void previousStep() {
		logger.trace("Next step");
		if (getActiveIndex() > 0)
			setActiveIndex(getActiveIndex() - 1);
	}

	public void openSelectDialog(Task task, AssociatedContact contact) {

		AbstractTemplate[] subSelect = AbstractTemplate.getTemplatesByTypes(
				new DocumentType[] { DocumentType.DIAGNOSIS_REPORT, DocumentType.DIAGNOSIS_REPORT_EXTERN });

		printDialog.initBeanForSelecting(task, subSelect, subSelect[0], new AssociatedContact[] { contact }, true);
		printDialog.setSingleAddress(true);
		printDialog.prepareDialog();
	}

	@Getter
	@Setter
	public abstract class AbstractTab {

		public abstract String getCenterView();

		public abstract void updateData();

		protected String name;

		protected String viewID;

		protected String tabName;

		protected boolean initialized;
	}

	@Getter
	@Setter
	public class MailTab extends AbstractTab {

		private boolean useTab;

		private String mailSubject;

		private String mailBody;

		private DiagnosisReportMail mail;

		private List<ContactHolder> holders;

		public MailTab() {
			setTabName("MailTab");
			setName("dialog.medicalFindings.tab.mail");
			setViewID("mailTab");
		}

		@Override
		public String getCenterView() {
			return "mail/mail.xhtml";
		}

		@Override
		public void updateData() {
			if (!isInitialized()) {
				System.out.println(DiagnosisReportMail.class + " " + task + " init");

				DiagnosisReportMail mail = MailHandler.getDefaultTemplate(DiagnosisReportMail.class);
				mail.prepareTemplate(task.getPatient(), task, null);
				mail.fillTemplate();

				setMailSubject(mail.getSubject());
				setMailBody(mail.getBody());

				setHolders(new ArrayList<ContactHolder>());

				setInitialized(true);
			}

			List<AssociatedContact> contacts = task.getContacts();
			List<ContactHolder> tmpHolders = new ArrayList<ContactHolder>(getHolders());

			for (AssociatedContact associatedContact : contacts) {
				if (associatedContact.containsNotificationTyp(NotificationTyp.EMAIL)) {

					try {
						ContactHolder tmpHolder = tmpHolders.stream()
								.filter(p -> p.getContact().equals(associatedContact))
								.collect(StreamUtils.singletonCollector());
						tmpHolders.remove(tmpHolder);
					} catch (IllegalStateException e) {
						// adding to list
						getHolders().add(new ContactHolder(associatedContact, mail));
					}
				}

			}

			for (ContactHolder contactHolder : tmpHolders) {
				getHolders().remove(contactHolder);
			}
		}

		@Getter
		@Setter
		@AllArgsConstructor
		public class ContactHolder {
			private AssociatedContact contact;
			private DiagnosisReportMail mail;
		}
	}

	@Getter
	@Setter
	public class FaxTab extends AbstractTab {

		public FaxTab() {
			setTabName("FaxTab");
			setName("dialog.medicalFindings.tab.fax");
			setViewID("faxTab");
		}

		@Override
		public String getCenterView() {
			return "fax/fax.xhtml";
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub

		}

	}

	@Getter
	@Setter
	public class LetterTab extends AbstractTab {

		public LetterTab() {
			setTabName("LetterTab");
			setName("dialog.medicalFindings.tab.letter");
			setViewID("letterTab");
		}

		@Override
		public String getCenterView() {
			return "letter/letter.xhtml";
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub

		}
	}

	@Getter
	@Setter
	public class SendTab extends AbstractTab {

		public SendTab() {
			setTabName("SendTab");
			setName("dialog.medicalFindings.tab.send");
			setViewID("sendTab");
		}

		@Override
		public String getCenterView() {
			return "send/send.xhtml";
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub

		}
	}
}
