package org.histo.action.view;

import org.histo.action.CommonDataHandlerAction;
import org.histo.action.DialogHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.model.interfaces.HasDataList;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class PatientViewHandlerAction {

	@Autowired
	private CommonDataHandlerAction commonDataHandlerAction;

	@Autowired
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	public void showPatientMediaDialog() {
		showPatientMediaDialog(null);
	}

	public void showPatientMediaDialog(PDFContainer container) {
		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(commonDataHandlerAction.getSelectedPatient(),
				new HasDataList[] { commonDataHandlerAction.getSelectedPatient() }, container, false);

		// enabeling upload to task
		dialogHandlerAction.getMediaDialog().enableUpload(new HasDataList[] { commonDataHandlerAction.getSelectedPatient() },
				new DocumentType[] { DocumentType.U_REPORT, DocumentType.COUNCIL_REPLY,
						DocumentType.BIOBANK_INFORMED_CONSENT, DocumentType.OTHER });

		// setting info text
		dialogHandlerAction.getMediaDialog().setActionDescription(resourceBundle.get("dialog.media.headline.info.patient",
				commonDataHandlerAction.getSelectedPatient().getPerson().getFullName()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}

	public void showTaskMediaDialog() {
		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(commonDataHandlerAction.getSelectedPatient(),
				new HasDataList[] { commonDataHandlerAction.getSelectedTask() }, false);

		// enabeling upload to task
		dialogHandlerAction.getMediaDialog().enableUpload(new HasDataList[] { commonDataHandlerAction.getSelectedTask() },
				new DocumentType[] { DocumentType.U_REPORT, DocumentType.COUNCIL_REPLY,
						DocumentType.BIOBANK_INFORMED_CONSENT, DocumentType.OTHER });

		// setting info text
		dialogHandlerAction.getMediaDialog().setActionDescription(resourceBundle.get("dialog.media.headline.info.task",
				commonDataHandlerAction.getSelectedTask().getTaskID()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}
}
