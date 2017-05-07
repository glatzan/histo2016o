package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.histo.model.transitory.SearchOptions;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class WorklistSearchDialogHandler extends AbstractDialog {

	/**
	 * Search Options
	 */
	private SearchOptions simpleSearchOptions;
	
	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public void initBean() {
		super.initBean(null, Dialog.WORKLIST_SEARCH);
	}
}
