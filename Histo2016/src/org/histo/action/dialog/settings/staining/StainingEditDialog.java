package org.histo.action.dialog.settings.staining;


import java.util.ArrayList;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.model.StainingPrototype;
import org.histo.model.StainingPrototypeDetails;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class StainingEditDialog extends AbstractDialog {

	private boolean newStaining;
	private StainingPrototype stainingPrototype;

	public void initAndPrepareBean() {
		StainingPrototype staining = new StainingPrototype();
		
		if (initBean(staining))
			prepareDialog();
	}
	
	public void initAndPrepareBean(StainingPrototype staining) {
		if (initBean(staining))
			prepareDialog();
	}

	public boolean initBean(StainingPrototype staining) {
		setStainingPrototype(staining);
		setNewStaining(staining.getId() == 0 ? true : false);
		super.initBean(task, Dialog.STAINING_EDIT);
		return true;
	}
	
	public void addBatch() {
		if(getStainingPrototype().getBatchDetails() == null)
			getStainingPrototype().setBatchDetails(new ArrayList<StainingPrototypeDetails>());
		
		getStainingPrototype().getBatchDetails().add(new StainingPrototypeDetails());
	}
}
