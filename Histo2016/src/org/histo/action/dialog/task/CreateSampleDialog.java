package org.histo.action.dialog.task;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CreateSampleDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

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
	private GlobalEditViewHandler globalEditViewHandler;
	
	private List<MaterialPreset> materials;

	private DefaultTransformer<MaterialPreset> materialTransformer;

	private MaterialPreset selectedMaterial;

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
			worklistViewHandlerAction.onVersionConflictTask(task, false);
		}

		super.initBean(task, Dialog.SAMPLE_CREATE);

		setMaterials(utilDAO.getAllMaterialPresets(true));

		if (!getMaterials().isEmpty()) {
			setMaterialTransformer(new DefaultTransformer<>(getMaterials()));
		}

		return true;
	}

	public void createNewSample() {
		try {
			sampleService.createSampleForTask(getTask(), getSelectedMaterial());
			// generating gui list
			globalEditViewHandler.updateDataOfTask(true, false, true, true);
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
