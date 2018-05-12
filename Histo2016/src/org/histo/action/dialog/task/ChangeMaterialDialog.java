package org.histo.action.dialog.task;

import java.util.List;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.HistoDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.MaterialPreset;
import org.histo.model.patient.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class ChangeMaterialDialog extends AbstractDialog {

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
	private UtilDAO utilDAO;

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
		} catch (HistoDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replaceTaskInCurrentWorklist(task, false);
		}

		super.initBean(task, Dialog.SELECT_MATERIAL);

		setSample(sample);

		setMaterials(utilDAO.getAllMaterialPresets(true));

		setSelectedMaterial(null);

		return true;
	}

	public void changeMaterialOfSample() {
		try {
			if (getSelectedMaterial() != null) {
				getSample().setMaterial(getSelectedMaterial().getName());
				sample.setMaterialPreset(getSelectedMaterial());

				genericDAO.savePatientData(sample, "log.patient.task.sample.material.update",
						getSelectedMaterial().toString());

			}
		} catch (HistoDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}
}
