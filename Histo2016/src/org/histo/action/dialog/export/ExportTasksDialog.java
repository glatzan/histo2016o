package org.histo.action.dialog.export;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.model.patient.Task;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
@Getter
public class ExportTasksDialog extends AbstractDialog {
	
	private List<Task> tasks;
	
	private List<Task> tasksToExport;
	
	public void initAndPrepareBean(List<Task> tasks) {
		if (initBean(tasks))
			prepareDialog();
	}

	public boolean initBean(List<Task> tasks) {
		super.initBean(null, Dialog.WORKLIST_EXPORT);
		
		this.tasks = tasks;
		this.tasksToExport = tasks;

		return true;
	}

}
