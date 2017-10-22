package org.histo.action.dialog.task;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.handler.TaskManipulationHandler;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class CreateSampleDialog extends AbstractDialog {

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private TaskManipulationHandler taskManipulationHandler;

	@Autowired
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

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

			taskManipulationHandler.createNewSample(getTask(), getSelectedMaterial());
			// updating names
			task.updateAllNames();
			genericDAO.savePatientData(getTask(), "log.patient.task.update", task.getTaskID());
			// checking if staining flag of the task object has to be false
			receiptlogViewHandlerAction.checkStainingPhase(getTask(), true);
			// generating gui list
			task.generateSlideGuiList();
			// saving patient
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public List<MaterialPreset> getMaterials() {
		return materials;
	}

	public void setMaterials(List<MaterialPreset> materials) {
		this.materials = materials;
	}

	public DefaultTransformer<MaterialPreset> getMaterialTransformer() {
		return materialTransformer;
	}

	public void setMaterialTransformer(DefaultTransformer<MaterialPreset> materialTransformer) {
		this.materialTransformer = materialTransformer;
	}

	public MaterialPreset getSelectedMaterial() {
		return selectedMaterial;
	}

	public void setSelectedMaterial(MaterialPreset selectedMaterial) {
		this.selectedMaterial = selectedMaterial;
	}
}
