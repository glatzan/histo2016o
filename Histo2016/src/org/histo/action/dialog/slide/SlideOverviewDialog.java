package org.histo.action.dialog.slide;

import java.util.ArrayList;

import org.histo.action.DialogHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.slide.CreateSlidesDialog.SlideSelectResult;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.ui.StainingTableChooser;
import org.histo.ui.task.TaskStatus;
import org.primefaces.PrimeFaces;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class SlideOverviewDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	private ArrayList<StainingTableChooser<?>> flatTaskEntityList;
	/**
	 * Is used for selecting a chooser from the generated list (generated by task).
	 * It is used to edit the names of the entities by an overlaypannel
	 */
	private StainingTableChooser<?> selectedStainingTableChooser;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (HistoDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		super.initBean(task, Dialog.SLIDE_OVERVIEW);

		updateData();

		return true;
	}

	public void updateData() {
		setFlatTaskEntityList(StainingTableChooser.factory(getTask(), false));
	}

	/**
	 * Creates slides if dialog returns the selected slides
	 * 
	 * @param event
	 */
	public void onSelectStainingDialogReturn(SelectEvent event) {
		logger.debug("On select staining dialog return ");

		if (event.getObject() != null && event.getObject() instanceof SlideSelectResult) {
			sampleService.createSlidesForSample((SlideSelectResult) event.getObject());

			// clicking button from backend in order to open dialog on close the select
			// dialog
			if (TaskStatus.checkIfReStainingFlag(getTask()) && !TaskStatus.checkIfStainingCompleted(getTask())
					&& getTask().getDiagnosisRevisions().size() == 1) {
				logger.debug("Opening dialog for creating a diagnosis revision");
				PrimeFaces.current().executeScript("executeFunctionFromBean(closeCreateDiagnosisRevision)");
			}
		}

		updateData();
	}
}
