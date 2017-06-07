package org.histo.action.dialog.slide;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.patient.Block;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class SlideNamingDialog extends AbstractDialog {

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistHandlerAction worklistHandlerAction;

	@Autowired
	private PatientDao patientDao;

	private boolean useAutoNomeclature;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("!! Version inconsistent with Database updating");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistHandlerAction.updatePatientInCurrentWorklist(task.getPatient());
		}

		super.initBean(task, Dialog.SLIDE_NAMING);

		setUseAutoNomeclature(task.isUseAutoNomenclature());
		return true;
	}

	public void renameTaskEntities(boolean ignoreManuallyChangedEntities) {
		try {
			for (Sample sample : task.getSamples()) {

				if (sample.updateNameOfSample(true, ignoreManuallyChangedEntities)) {
					patientDao.savePatientAssociatedDataFailSave(sample, "log.patient.task.sample.updateName");
				}

				for (Block block : sample.getBlocks()) {

					if (block.updateNameOfBlock(true, ignoreManuallyChangedEntities)) {
						patientDao.savePatientAssociatedDataFailSave(block, "log.patient.task.sample.block.updateName");
					}

					for (Slide slide : block.getSlides()) {

						if (slide.updateNameOfSlide(true, ignoreManuallyChangedEntities)) {
							patientDao.savePatientAssociatedDataFailSave(slide,
									"log.patient.task.sample.block.slide.updateName");
						}
					}
				}
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void saveAutoNomeclature() {
		try {
			if (task.isUseAutoNomenclature() != isUseAutoNomeclature()) {
				task.setUseAutoNomenclature(isUseAutoNomeclature());
				patientDao.savePatientAssociatedDataFailSave(task, "log.patient.task.update");
			}
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public boolean isUseAutoNomeclature() {
		return useAutoNomeclature;
	}

	public void setUseAutoNomeclature(boolean useAutoNomeclature) {
		this.useAutoNomeclature = useAutoNomeclature;
	}
}