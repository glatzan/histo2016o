package org.histo.action.dialog;

import org.histo.config.enums.Dialog;
import org.histo.dao.BioBankDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.UtilDAO;
import org.histo.model.BioBank;
import org.histo.model.patient.Patient;
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

	private BioBank bioBank;

	/**
	 * Initializes the bean and shows the biobank dialog
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Task task) {
		initBean(task);
		prepareDialog();
	}

	/**
	 * Initializes all field of the biobank object
	 * @param task
	 */
	public void initBean(Task task) {
		super.initBean((Task)patientDao.savePatientAssociatedData(task), Dialog.BIO_BANK);
		// setting associatedBioBank
		setBioBank(bioBankDAO.getAssociatedBioBankObject(task));

		if (getBioBank() != null) {
			bioBankDAO.initializeBioBank(getBioBank());
		}
	}

	/**
	 * Saves the biobank to the database
	 */
	public void saveBioBank() {
		genericDAO.saveDataChange(getBioBank(), "log.patient.bioBank.save");
	}

	// ************************ Getter/Setter ************************
	public BioBank getBioBank() {
		return bioBank;
	}

	public void setBioBank(BioBank bioBank) {
		this.bioBank = bioBank;
	}

}
