package org.histo.config.enums;

/**
 * mainHandlerAction.showDialog(enumProvider.getDialog('WORKLIST_ORDER'))
 * @author andi
 *
 */
public enum Dialog {
	
	WORKLIST_SEARCH("/pages/dialog/search/worklistSearch", null, 1024, 600, false, false, true), // 16:9
	WORKLIST_ADD_PATIENT("/pages/dialog/patient/addPatient", null, 1024, 600, false, false, true), // 16:9
	WORKLIST_ACCOUNTING("/pages/dialog/task/accounting", null,480, 272, false, false, true), 
	WORKLIST_ORDER("/pages/dialog/worklist/worklistOrder", null,480, 272, false, false, true),  // 16:9
	WORKLIST_SETTINGS("/pages/dialog/worklist/worklistSettings", null,480, 272, false, false, true), 
	PATIENT_EDIT("/pages/dialog/patient/editPatient", null, 1024, 600, false, false, true), // 16:9
	PATIENT_LOG("/pages/dialog/history/patientLog", null,  1024, 600, false, false, true),// 16:9
	TASK_CREATE("/pages/dialog/task/createTask", null, 858, 484, false, false, true), // 16:9
	TASK_DELETE("/pages/dialog/task/deleteTask", null, 480, 272, false, false, true), // 16:9
	SAMPLE_CREATE("/pages/dialog/task/createSample", null, 480, 272, false, false, true), // 16:9
	BIO_BANK("/pages/dialog/biobank/biobank", null, 858, 484, false, false, true), // 16:9
	FAVOURITE_LISTS("/pages/dialog/favouriteLists/favouriteLists", null, 858, 484, false, false, true), // 16:9
	SLIDE_NAMING("/pages/dialog/task/slideNaming", null,430, 270, false, false, true), 
	SLIDE_CREATE("/pages/dialog/task/addSlide", null, 640, 360, false, false, true),// 16:9
	DIAGNOSIS_RECORD_OVERWRITE("/pages/dialog/task/diagnosisRecordOverwrite", null, 480, 272, false, false, true), // 16:9
	DIAGNOSIS_REVISION_CREATE("/pages/dialog/task/diagnosisCreateRevision", null, 640, 360, false, false, true), // 16:9
	CONTACTS("/pages/dialog/contact/contacts", null, 1024, 600, false, false, true), // 16:9
	CONTACTS_NOTIFICATION("/pages/dialog/contact/contactNotification", null, 858, 484, false, false, true), // 16:9
	QUICK_CONTACTS("/pages/dialog/contact/contactSelect", null, 858, 484, false, false, true), // 16:9
	SETTINGS("/pages/dialog/settings/settings", null, 1024, 600, false, false, true), // 16:9
	PRINT("/pages/dialog/print/print", null, 1280, 720, false, false, true), // 16:9
	PRINT_ADDRESS("/pages/dialog/print/address", null,  480, 272, false, false, true), // 16:9
	PRINT_FAX("/pages/dialog/print/printFax", null,  480, 272, false, false, true), // 16:9
	COUNCIL("/pages/dialog/council/council", null,  1024, 600, false, false, true), // 16:9
	USER_SETTINGS("/pages/dialog/user/userSettings", null,  1024, 600, false, false, true), // 16:9
	MEDICAL_FINDINGS("/pages/dialog/medicalFindings/medicalFindings", null,  1024, 600, false, false, true),  // 16:9
	NOTIFICATION_ALREADY_PERFORMED("/pages/dialog/notification/notification_already_performed", null,  480, 272, false, false, true), //  16:9
	NOTIFICATION("/pages/dialog/notification/notification", null,  1024, 600, false, false, true),  // 16:9
	NOTIFICATION_PREVIEW("/pages/dialog/notification/notification_preview", null,  1024, 600, false, false, true), //  16:9
	INFO("/pages/dialog/info/info", null,  1024, 600, false, false, true), //  16:9
	UPLOAD("/pages/dialog/upload/upload", null,  640, 360, false, false, true),//  16:9
	SELECT_MATERIAL("/pages/dialog/task/selectMaterial", null, 640, 360, false, false, true), // 16:9
	DELETE_TREE_ENTITY("/pages/dialog/task/deleteTreeEntity", null,  480, 272, false, false, true), //  16:9
	MEDIA_PREVIEW("/pages/dialog/upload/mediaPreview", null,  1280, 720,  false, false, true),  // 16:9
	MEDIA("/pages/dialog/upload/media", null,  1280, 720,  false, false, true), // 16:9
	STAINING_PHASE_LEAVE("/pages/dialog/task/stainingPhaseLeave", null, 480, 272, false, false, true), // 16:9
	STAINING_PHASE_FORCE_ENTER("/pages/dialog/task/stainingPhaseForceEnter", null, 480, 272, false, false, true), // 16:9
	STAINING_PHASE_FORCE_LEAVE("/pages/dialog/task/stainingPhaseForceLeave", null, 480, 272, false, false, true), // 16:9
	DIAGNOSIS_PHASE_LEAVE("/pages/dialog/task/diagnosisPhaseLeave", null, 1024, 600, false, false, true), // 16:9
	DIAGNOSIS_PHASE_FORCE_ENTER("/pages/dialog/task/diagnosisPhaseForceEnter", null, 480, 272, false, false, true), // 16:9
	DIAGNOSIS_PHASE_FORCE_LEAVE("/pages/dialog/task/diagnosisPhaseForceLeave", null, 480, 272, false, false, true), // 16:9
	ORGANIZATION_EDIT("/pages/dialog/organization/organizationEdit", null, 858, 484, false, false, true), // 16:9
	ORGANIZATION_LIST("/pages/dialog/organization/organizationList", null, 858, 484, false, false, true); // 16:9
	
	
	
	private final String path;
	private final boolean useOptions;
	private final String header;
	
	private final int width;
	private final int height;
	private final boolean resizeable;
	private final boolean draggable;
	private final boolean modal;
	
	Dialog(final String path) {
		this.path = path;
		this.width = 0;	
		this.height = 0;
		this.resizeable = false;
		this.draggable = false;
		this.modal = false;
		this.useOptions = false;
		this.header = null;
	}

	Dialog(final String path, final int width, final int heigt) {
		this.path = path;
		this.width = width;	
		this.height = heigt;
		this.resizeable = true;
		this.draggable = true;
		this.modal = false;
		this.useOptions = true;
		this.header = null;
	}
	
	Dialog(final String path, final String header, final int width, final int heigt, final boolean resizeable, final boolean draggable, final boolean modal) {
		this.path = path;
		this.width = width;	
		this.height = heigt;
		this.resizeable = resizeable;
		this.draggable = draggable;
		this.modal = modal;
		this.useOptions = true;
		this.header = header;
	}
	
	public String getPath() {
		return path;
	}

	public boolean isUseOptions() {
		return useOptions;
	}

	public int getWidth() {
		return width;
	}

	public int getHeight() {
		return height;
	}

	public boolean isResizeable() {
		return resizeable;
	}

	public boolean isDraggable() {
		return draggable;
	}

	public boolean isModal() {
		return modal;
	}
	
	public String getHeader(){
		return header;
	}
}
