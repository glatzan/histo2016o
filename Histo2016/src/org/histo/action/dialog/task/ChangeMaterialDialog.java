package org.histo.action.dialog.task;

import java.util.List;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.action.dialog.WorklistSearchDialogHandler;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.SettingsDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.DefaultTransformer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class ChangeMaterialDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private SettingsDAO settingsDAO;

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private PatientDao patientDao;

	private Sample sample;

	private List<MaterialPreset> materials;

	private MaterialPreset selectedMaterial;

	public void initAndPrepareBean(Sample sample) {
		if (initBean(sample))
			prepareDialog();
	}

	public boolean initBean(Sample sample) {
		try {
			taskDAO.initializeTask(sample.getTask(), false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
		}

		super.initBean(task, Dialog.SELECT_MATERIAL);

		setSample(sample);

		setMaterials(settingsDAO.getAllMaterialPresets());
		utilDAO.initStainingPrototypeList(getMaterials());

		return true;
	}

	public void changeMaterialOfSample() {
		try {
			if (getSelectedMaterial() != null) {
				getSample().setMaterial(getSelectedMaterial().getName());
				sample.setMaterilaPreset(getSelectedMaterial());

				patientDao.savePatientAssociatedDataFailSave(sample, "log.patient.task.sample.material.update",
						getSelectedMaterial().toString());

			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public Sample getSample() {
		return sample;
	}

	public void setSample(Sample sample) {
		this.sample = sample;
	}

	public List<MaterialPreset> getMaterials() {
		return materials;
	}

	public void setMaterials(List<MaterialPreset> materials) {
		this.materials = materials;
	}

	public MaterialPreset getSelectedMaterial() {
		return selectedMaterial;
	}

	public void setSelectedMaterial(MaterialPreset selectedMaterial) {
		this.selectedMaterial = selectedMaterial;
	}
}
