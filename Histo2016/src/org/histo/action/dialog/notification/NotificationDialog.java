package org.histo.action.dialog.notification;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
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

	private int activeSettingsIndex = 0;

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

		setActiveSettingsIndex(0);

		return true;
	}

	public void onTabChange(TabChangeEvent event) {
		if (getActiveSettingsIndex() >= 0 && getActiveSettingsIndex() < getTabs().length) {
			logger.debug("Updating Tab with index " + getActiveSettingsIndex());
			getTabs()[getActiveSettingsIndex()].updateData();
		}
	}

	public AbstractTab getTab(String tabName) {
		for (AbstractTab abstractSettingsTab : tabs) {
			if (abstractSettingsTab.getTabName().equals(tabName))
				return abstractSettingsTab;
		}

		return null;
	}
	
	@Getter
	@Setter
	public abstract class AbstractTab {

		public abstract String getCenterView();

		public abstract void updateData();

		protected String name;

		protected String viewID;

		protected String tabName;
	}

	@Getter
	@Setter
	public class MailTab extends AbstractTab {

		private boolean useTab;
		
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
			// TODO Auto-generated method stub

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
