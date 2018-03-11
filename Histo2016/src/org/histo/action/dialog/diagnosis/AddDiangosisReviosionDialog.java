package org.histo.action.dialog.diagnosis;

import org.histo.action.dialog.AbstractDialog;
import org.histo.config.enums.DiagnosisRevisionType;
import org.histo.config.enums.Dialog;
import org.histo.model.patient.Task;
import org.histo.service.DiagnosisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * Dialog for adding a diagnosis revision on creating a restaining
 * 
 * @author andi
 *
 */
@Component
@Scope(value = "session")
@Setter
@Getter
public class AddDiangosisReviosionDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DiagnosisService diagnosisService;

	public void initAndPrepareBean(Task task) {
		if (initBean(task))
			prepareDialog();
	}

	public boolean initBean(Task task) {
		super.initBean(task, Dialog.DIAGNOSIS_REVISION_ADD);
		return true;
	}

	public void createDiagosisRevision() {
		diagnosisService.createDiagnosisRevision(getTask(), DiagnosisRevisionType.DIAGNOSIS_REVISION);
		mainHandlerAction.sendGrowlMessagesAsResource("growl.diagnosis.create.rediagnosis");
	}
}
