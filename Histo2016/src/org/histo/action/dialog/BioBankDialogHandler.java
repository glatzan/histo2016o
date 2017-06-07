package org.histo.action.dialog;

import org.histo.action.WorklistHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.BioBankDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.TaskDAO;
import org.histo.model.BioBank;
import org.histo.model.patient.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "session")
public class BioBankDialogHandler extends AbstractDialog {

	@Autowired
	private BioBankDAO bioBankDAO;

	@Autowired
	private PatientDao patientDao;

	@Autowired
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	private BioBank bioBank;

	/**
	 * Initializes the bean and shows the biobank dialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	/**
	 * Initializes all field of the biobank object
	 * 
	 * @param task
	 */
	public boolean initBean(Task task) {
		try {
			taskDAO.initializeTask(task, false);

			super.initBean(task, Dialog.BIO_BANK);

			// setting associatedBioBank
			setBioBank(bioBankDAO.getAssociatedBioBankObject(task));

			if (getBioBank() != null) {
				setBioBank(bioBankDAO.initializeBioBank(getBioBank()));
			}

			return true;
		} catch (CustomDatabaseInconsistentVersionException e) {
			logger.debug("Version conflict, updating entity");
			task = taskDAO.getTaskAndPatientInitialized(task.getId());
			worklistViewHandlerAction.replacePatientTaskInCurrentWorklistAndSetSelected(task);
			return false;
		}
	}

	/**
	 * Saves the biobank to the database
	 */
	public void saveBioBank() {
		try {
			// saving biobank
			patientDao.savePatientAssociatedDataFailSave(getBioBank(), getTask(), "log.patient.bioBank.save");
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	// ************************ Getter/Setter ************************
	public BioBank getBioBank() {
		return bioBank;
	}

	public void setBioBank(BioBank bioBank) {
		this.bioBank = bioBank;
	}

}
