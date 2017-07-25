package org.histo.action;

import java.util.List;

import org.histo.config.enums.ContactRole;
import org.histo.config.enums.View;
import org.histo.model.AssociatedContact;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

/**
 * Object containing commonly used variables (mostly for dialogs)!
 * 
 * @author andi
 *
 */
@Component
@Scope(value = "session")
@Getter
@Setter
public class CommonDataHandlerAction {

	// ************************ Navigation ************************
	/**
	 * View options, dynamically generated depending on the users role
	 */
	private List<View> navigationPages;

	// ************************ Patient ************************
	/**
	 * Currently selectedTask
	 */
	private Patient selectedPatient;

	/**
	 * Currently selectedTask
	 */
	private Task selectedTask;

}
