package org.histo.config.enums;

/**
 * mainHandlerAction.showDialog(enumProvider.getDialog('WORKLIST_ORDER'))
 * @author andi
 *
 */
public enum Dialog {
	
	WORKLIST_SEARCH("/pages/dialog/worklist/worklistSearch", null, 858, 484, false, false, true), // 16:9
	WORKLIST_ADD_PATIENT("/pages/dialog/patient/addPatient", null, 1024, 600, false, false, true), // 16:9
	WORKLIST_ACCOUNTING("/pages/dialog/task/accounting", null,430, 270, false, false, true), 
	WORKLIST_ORDER("/pages/dialog/worklist/worklistOrder", null,290, 230, false, false, true), 
	WORKLIST_SETTINGS("/pages/dialog/worklist/worklistSettings", null, 290, 120, false, false, true), 
	PATIENT_EDIT("/pages/dialog/patient/editPatient", null, 1024, 600, false, false, true), // 16:9
	PATIENT_LOG("/pages/dialog/history/patientLog", null,  1024, 600, false, false, true),// 16:9
	TASK_CREATE("/pages/dialog/task/createTask", null, 858, 484, false, false, true), // 16:9
	TASK_ARCHIV("/pages/dialog/task/archivTask", null, 0, 0, false, false, true), 
	SAMPLE_CREATE("/pages/dialog/task/createSample", null, 480, 272, false, false, true), // 16:9
	SAMPLE_ARCHIV("/pages/dialog/task/archivSample", null, 0, 0, false, false, true), 
	BLOCK_ARCHIV("/pages/dialog/task/archivBlock", null, 0, 0, false, false, true), 
	SLIDE_CREATE("/pages/dialog/task/addSlide", null, 0, 0, false, false, true),
	SLIDE_ARCHIV("/pages/dialog/task/archivSlide", null, 0, 0, false, false, true),
	DIAGNOSIS_FINALIZE("/pages/dialog/diagnosis/finalizeDiagnosis", null, 480, 272, false, false, true),// 16:9
	DIAGNOSIS_UNFINALIZE("/pages/dialog/diagnosis/unfinalizeDiagnosis", null, 0, 0, false, false, true),
	DIAGNOSIS_RECORD_OVERWRITE("/pages/dialog/diagnosis/histologicalRecordOverwrite", null, 0, 0, false, false, true),
	STAINING_PERFORMED("/pages/dialog/task/staingingPerformed", null, 480, 272, false, false, true), // 16:9
	CONTACTS("/pages/dialog/contact/contact", null, 1024, 600, false, false, true), // 16:9
	SETTINGS("/pages/dialog/settings/settings", null, 1024, 600, false, false, true), // 16:9
	PRINT("/pages/dialog/print/print", null, 1024, 600, false, false, true), // 16:9
	COUNCIL("/pages/dialog/task/council", null,  640, 360, false, false, true), // 16:9
	NOTIFICATION_ALREADY_PERFORMED("/pages/dialog/notification/notification_already_performed", null,  480, 272, false, false, true), //  16:9
	NOTIFICATION("/pages/dialog/notification/notification", null,  1024, 600, false, false, true),  // 16:9
	NOTIFICATION_PREVIEW("/pages/dialog/notification/notification_preview", null,  1024, 600, false, false, true), //  16:9
	INFO("/pages/dialog/info/info", null,  1024, 600, false, false, true), //  16:9
	UPLOAD_TASK("/pages/dialog/upload/uploadPdfToTask", null,  640, 360, false, false, true);
	
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
