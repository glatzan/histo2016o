package org.histo.action.dialog;

import org.primefaces.event.TabChangeEvent;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTabDialog extends AbstractDialog {

	protected AbstractTab[] tabs;

	//TODO  DELETE if all dialogs are converted to the new system
	protected int activeIndex = 0;

	protected AbstractTab selectedTab;
	
	public void onTabChange() {
		TabChangeEvent e = null;
		this.onTabChange(e);
	}
	
	//TODO DELETE if all dialogs are converted to the new system
	public void onTabChange(TabChangeEvent event) {
		if (getActiveIndex() >= 0 && getActiveIndex() < getTabs().length) {
			logger.debug("Updating Tab with index " + getActiveIndex());
			getTabs()[getActiveIndex()].updateData();
		}
	}
	
	public void onTabChange(AbstractTab tab) {
		setSelectedTab(tab);
		tab.updateData();
	}
	
	@Getter
	@Setter
	public abstract class AbstractTab {

		public abstract void updateData();

		public boolean initTab() {
			return false;
		}

		protected String name;

		protected String viewID;

		protected String tabName;

		protected String centerInclude;
	}

}
