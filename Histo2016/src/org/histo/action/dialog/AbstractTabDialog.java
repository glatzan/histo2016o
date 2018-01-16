package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.histo.model.patient.Task;
import org.primefaces.event.TabChangeEvent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTabDialog extends AbstractDialog {

	protected AbstractTab[] tabs;

	protected AbstractTab selectedTab;

	public boolean initBean(Task task, Dialog dialog) {
		super.initBean(task, dialog);

		for (int i = 0; i < tabs.length; i++) {
			tabs[i].initTab();
		}

		onTabChange(tabs[0]);

		return true;
	}

	public boolean initBean(Dialog dialog) {
		return initBean(null, dialog);
	}

	public void onTabChange(AbstractTab tab) {
		logger.debug("Changing tab to " + tab.getName());
		setSelectedTab(tab);
		tab.updateData();
	}

	public void nextTab() {
		logger.trace("Next tab");
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] == selectedTab) {
				while (i++ <= tabs.length - 1) {
					if (!tabs[i].isDisabled()) {
						onTabChange(tabs[i]);
						return;
					}
				}
			}
		}
	}

	public void previousTab() {
		logger.trace("Previous step");
		for (int i = 0; i < tabs.length; i++) {
			if (tabs[i] == selectedTab) {
				while (--i >= 0) {
					if (!tabs[i].isDisabled()) {
						onTabChange(tabs[i]);
						return;
					}
				}
			}
		}
	}

	@Getter
	@Setter
	public abstract class AbstractTab {

		public void updateData() {
			return;
		}

		public boolean initTab() {
			return false;
		}

		protected String name;

		protected String viewID;

		protected String tabName;

		protected String centerInclude;

		protected boolean disabled;

		protected AbstractTab parentTab;

		public boolean isParent() {
			return parentTab != null;
		}
	}

}
