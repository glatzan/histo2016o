package org.histo.action.view;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.DocumentType;
import org.histo.model.PDFContainer;
import org.histo.util.dataList.HasDataList;
import org.histo.util.event.PatientMergeEvent;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

@Controller
@Scope("session")
public class PatientViewHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");
	
	@Autowired
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private WorklistViewHandlerAction worklistViewHandlerAction;

	public void showPatientMediaDialog() {
		showPatientMediaDialog(null);
	}

	public void showPatientMediaDialog(PDFContainer container) {
		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(globalEditViewHandler.getSelectedPatient(),
				new HasDataList[] { globalEditViewHandler.getSelectedPatient() }, container, false);

		// enabeling upload to task
		dialogHandlerAction.getMediaDialog().enableUpload(
				new HasDataList[] { globalEditViewHandler.getSelectedPatient() },
				new DocumentType[] { DocumentType.U_REPORT, DocumentType.COUNCIL_REPLY,
						DocumentType.BIOBANK_INFORMED_CONSENT, DocumentType.OTHER });

		// setting info text
		dialogHandlerAction.getMediaDialog()
				.setActionDescription(resourceBundle.get("dialog.media.headline.info.patient",
						globalEditViewHandler.getSelectedPatient().getPerson().getFullName()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}

	public void showTaskMediaDialog() {
		// init dialog for patient and task
		dialogHandlerAction.getMediaDialog().initBean(globalEditViewHandler.getSelectedPatient(),
				new HasDataList[] { globalEditViewHandler.getSelectedTask() }, false);

		// enabeling upload to task
		dialogHandlerAction.getMediaDialog().enableUpload(new HasDataList[] { globalEditViewHandler.getSelectedTask() },
				new DocumentType[] { DocumentType.U_REPORT, DocumentType.COUNCIL_REPLY,
						DocumentType.BIOBANK_INFORMED_CONSENT, DocumentType.OTHER });

		// setting info text
		dialogHandlerAction.getMediaDialog().setActionDescription(resourceBundle.get("dialog.media.headline.info.task",
				globalEditViewHandler.getSelectedTask().getTaskID()));

		// show dialog
		dialogHandlerAction.getMediaDialog().prepareDialog();
	}

	/**
	 * Is called on return of the patient data edit dialog, if a merge event had
	 * happened the worklist is updated.
	 * 
	 * @param event
	 */
	public void onEditPatientDataReturn(SelectEvent event) {
		logger.debug("On EditPatient-Dialog return");
		if (event.getObject() != null && event.getObject() instanceof PatientMergeEvent) {
			PatientMergeEvent p = (PatientMergeEvent) event.getObject();
			
			// if merge source was archived, remove it from worklist
			if(p.getMergeFrom().isArchived())
				worklistViewHandlerAction.removePatientFromCurrentWorklist(p.getMergeFrom());
			else
				worklistViewHandlerAction.replacePatientInCurrentWorklist(p.getMergeFrom());
			
			worklistViewHandlerAction.replacePatientInCurrentWorklist(p.getMergeTo());
		}
	}
}
