package org.histo.action.dialog;

import org.histo.action.DialogHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.DocumentType;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.BioBankDAO;
import org.histo.dao.TaskDAO;
import org.histo.model.BioBank;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;
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
	private TaskDAO taskDAO;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	private DialogHandlerAction dialogHandlerAction;

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
			worklistViewHandlerAction.onVersionConflictTask(task, false);
			return false;
		}
	}

	/**
	 * Saves the biobank to the database
	 */
	public void saveBioBank() {
		try {
			// saving biobank
			genericDAO.savePatientData(getBioBank(), getTask(), "log.patient.bioBank.save");
		} catch (CustomDatabaseInconsistentVersionException e) {
			onDatabaseVersionConflict();
		}
	}

	public void showMediaSelectDialog() {
		try {

			// init dialog for patient and task
			dialogHandlerAction.getMediaDialog().initBean(getTask().getPatient(),
					new HasDataList[] { getTask(), getTask().getPatient() }, true);

			// setting advance copy mode with move as true and target to task
			// and biobank
			dialogHandlerAction.getMediaDialog().enableAutoCopyMode(new HasDataList[] { getTask(), getBioBank() }, true,
					true);

			// enabeling upload to task
			dialogHandlerAction.getMediaDialog().enableUpload(new HasDataList[] { getTask() },
					new DocumentType[] { DocumentType.BIOBANK_INFORMED_CONSENT });

			// setting info text
			dialogHandlerAction.getMediaDialog().setActionDescription(
					resourceBundle.get("dialog.media.headline.info.biobank", getTask().getTaskID()));

			// show dialog
			dialogHandlerAction.getMediaDialog().prepareDialog();
		} catch (CustomDatabaseInconsistentVersionException e) {
			// do nothing
			// TODO: infom user
		}
	}

	public void showMediaViewDialog(PDFContainer pdfContainer) {
		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(getTask().getPatient(), getBioBank(), pdfContainer, false);

		// setting info text
		dialogHandlerAction.getMediaDialog()
				.setActionDescription(resourceBundle.get("dialog.media.headline.info.biobank", getTask().getTaskID()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}

	// ************************ Getter/Setter ************************
	public BioBank getBioBank() {
		return bioBank;
	}

	public void setBioBank(BioBank bioBank) {
		this.bioBank = bioBank;
	}

}
