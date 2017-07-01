package org.histo.action.dialog;

import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;

import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.OrganizationDAO;
import org.histo.dao.PatientDao;
import org.histo.model.Organization;
import org.histo.model.patient.Patient;
import org.primefaces.util.Constants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.Getter;
import lombok.Setter;

@Configurable
public class OrganizationListDialog extends AbstractDialog {

	@Autowired
	private OrganizationDAO organizationDAO;

	@Getter
	@Setter
	private List<Organization> organizations;

	@Getter
	@Setter
	private boolean selectMode;

	@Getter
	@Setter
	private Organization selectedOrganization;

	public void initAndPrepareBean(boolean selectMode) {
		initBean(selectMode);
		prepareDialog();
	}

	public void initBean(boolean selectMode) {
		super.initBean(null, Dialog.ORGANIZATION_LIST);
		this.selectMode = selectMode;
		setOrganizations(organizationDAO.getOrganizations());
	}

	public void test() {
		System.out.println(isDialogContext() + " " + FacesContext.getCurrentInstance() + " " + mainHandlerAction.test);
		FacesContext context = FacesContext.getCurrentInstance();
		context.addMessage("globalgrowl", new FacesMessage("", "testset"));
		mainHandlerAction.test.addMessage("globalgrowl", new FacesMessage("", "testset"));
	}

	public static boolean isDialogContext() {

		return FacesContext.getCurrentInstance().getExternalContext().getRequestParameterMap()
				.containsKey(Constants.DIALOG_FRAMEWORK.CONVERSATION_PARAM);
	}

}