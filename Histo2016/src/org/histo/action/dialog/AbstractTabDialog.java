package org.histo.action.dialog;

import javax.faces.event.AbortProcessingException;

import org.apache.log4j.Logger;
import org.histo.action.MainHandlerAction;
import org.histo.action.dialog.SettingsDialogHandler.AbstractSettingsTab;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomNotUniqueReqest;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Task;
import org.histo.util.UniqueRequestID;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public abstract class AbstractTabDialog extends AbstractDialog {

	protected AbstractTab[] tabs;

	protected int activeIndex = 0;

	public void onTabChange(TabChangeEvent event) {
		if (getActiveIndex() >= 0 && getActiveIndex() < getTabs().length) {
			logger.debug("Updating Tab with index " + getActiveIndex());
			getTabs()[getActiveIndex()].updateData();
		}
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
