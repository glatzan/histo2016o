package org.histo.config.enums;

/**
 * mainHandlerAction.showDialog(enumProvider.getDialog('WORKLIST_ORDER'))
 * @author andi
 *
 */
public enum Dialog {
	
	WORKLIST_SEARCH("/pages/dialog/worklist/worklistSearch", 620, 440, false, false, true),
	WORKLIST_ADD_PATIENT("/pages/dialog/patient/addPatient", 1024, 600, false, false, true), //
	WORKLIST_ACCOUNTING("/pages/dialog/task/accounting", 430, 270, false, false, true), //
	WORKLIST_ORDER("/pages/dialog/worklist/worklistOrder", 290, 230, false, false, true), //
	WORKLIST_SETTINGS("/pages/dialog/worklist/worklistSettings", 290, 120, false, false, true), //
	PATIENT_EDIT("/pages/dialog/patient/editPatient", 600, 500, false, false, true),
	PATIENT_LOG("/pages/dialog/history/patientLog", 620, 500, false, false, true),
	TASK_CREATE("/pages/dialog/task/createTask", 0, 380, false, false, true),
	TASK_ARCHIV("/pages/dialog/task/archivTask", 0, 0, false, false, true), // <<
	SAMPLE_CREATE("/pages/dialog/task/createSample", 0, 0, false, false, true),
	SAMPLE_ARCHIV("/pages/dialog/task/archivSample", 0, 0, false, false, true), // <<
	BLOCK_ARCHIV("/pages/dialog/task/archivBlock", 0, 0, false, false, true), // <<
	SLIDE_CREATE("/pages/dialog/task/addSlide", 0, 0, false, false, true),
	SLIDE_ARCHIV("/pages/dialog/task/archivSlide", 0, 0, false, false, true),
	DIAGNOSIS_FINALIZE("/pages/dialog/diagnosis/finalizeDiagnosis", 0, 0, false, false, true),
	DIAGNOSIS_UNFINALIZE("/pages/dialog/diagnosis/unfinalizeDiagnosis", 0, 0, false, false, true),
	DIAGNOSIS_RECORD_OVERWRITE("/pages/dialog/diagnosis/histologicalRecordOverwrite", 0, 0, false, false, true),
	CONTACTS("/pages/dialog/contact/contact", 1024, 600, false, false, true),// <<
	STAINING_PERFORMED("/pages/dialog/task/staingingPerformed", 0, 0, false, false, true),
	SETTINGS("/pages/dialog/settings/settings", 1024, 600, false, false, true),
	PRINT("/pages/dialog/print/print", 1024, 600, false, false, true), // <<
	COUNCIL("/pages/dialog/task/council", 640, 360, false, false, true),
	; 
	
	
	private final String path;
	private final boolean useOptions;
	
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
	}

	Dialog(final String path, final int width, final int heigt) {
		this.path = path;
		this.width = width;	
		this.height = heigt;
		this.resizeable = true;
		this.draggable = true;
		this.modal = false;
		this.useOptions = true;
	}
	
	Dialog(final String path, final int width, final int heigt, final boolean resizeable, final boolean draggable, final boolean modal) {
		this.path = path;
		this.width = width;	
		this.height = heigt;
		this.resizeable = resizeable;
		this.draggable = draggable;
		this.modal = modal;
		this.useOptions = true;
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
}
