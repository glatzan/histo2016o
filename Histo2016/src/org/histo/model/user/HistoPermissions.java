package org.histo.model.user;

public enum HistoPermissions {
	PROGRAM_SETTINGS, 
	PROGRAM_SETTINGS_USER (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_GROUP (PROGRAM_SETTINGS), 
	PROGRAM_SETTINGS_DIAGNOSES (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_MATERIAL (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_STAINING (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_LISTS (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_FAVOURITE (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_PHYSICIAN (PROGRAM_SETTINGS),
	PROGRAM_SETTINGS_ORGANIZAZIONS (PROGRAM_SETTINGS),
	TASK_EDIT,
	TASK_EDIT_NEW (TASK_EDIT),
	TASK_EDIT_ID (TASK_EDIT),
	TASK_EDIT_ARCHIVE (TASK_EDIT),
	TASK_EDIT_RESTORE (TASK_EDIT),
	TASK_EDIT_DELETE (TASK_EDIT),
	TASK_EDIT_DELETE_EDITED (TASK_EDIT),
	PATIENT_EDIT,
	/**
	 * User can add a patient found in the clinic database to the local database 
	 * -> Used in AddPatientDialogHandler to only update local, not display clinic patients
	 * -> Used in GlobalEditViewHandler in quicksearch to search only in local database
	 */
	PATIENT_EDIT_ADD_CLINIC(PATIENT_EDIT),
	/**
	 * User can add new external patients to the local and clinic database, a new piz is therefore created
	 * -> Used in AddPatientDialogHandler to disable the Tab for creating an external patient.
	 */
	PATIENT_EDIT_ADD_EXTERN(PATIENT_EDIT),
	
	/**
	 * User can upload pdfs for a patient
	 * -> Used in worklist/patient (patient.xhtml) for hiding the pdf upload button
	 */
	PATIENT_EDIT_UPLOAD_DATA(PATIENT_EDIT),
	
	/**
	 * Father of USER permissions 
	 * -> Not used
	 */
	USER,
	
	/**
	 * User can use worklist/search
	 * -> Used in worklist/patientlist (patientList.xhtnl) to hide the worklist select dialog button
	 */
	USER_WORKLIST (USER),
	
	/**
	 * If not set user can not use favourite lists, tab in usersettings is not shown
	 * -> Used in UserSettingsDialog to disable the favouritelist tab
	 */
	USER_FAVOURITE_LIST (USER);
	
	private HistoPermissions parent;

	HistoPermissions() {
		this(null);
	}

	HistoPermissions(HistoPermissions parent) {
		this.parent = parent;
	}

	public HistoPermissions getParent() {
		return parent;
	}
	
	public int getLevel() {
		if(parent != null)
			return 1 + parent.getLevel();
		return 1;
	}

}
