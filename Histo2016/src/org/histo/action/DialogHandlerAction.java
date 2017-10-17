package org.histo.action;

import org.histo.action.dialog.OrganizationListDialog;
import org.histo.action.dialog.WorklistSearchDialog;
import org.histo.action.dialog.diagnosis.DiagnosisExitStayInPhaseDialog;
import org.histo.action.dialog.diagnosis.DiagnosisPhaseExitDialog;
import org.histo.action.dialog.diagnosis.DiagnosisUnFinalizeDialog;
import org.histo.action.dialog.favouriteLists.AddTaskToFavouriteListDialog;
import org.histo.action.dialog.favouriteLists.FavouriteListsDialog;
import org.histo.action.dialog.media.MediaDialog;
import org.histo.action.dialog.notification.ContactDialog;
import org.histo.action.dialog.notification.ContactNotificationDialog;
import org.histo.action.dialog.notification.ContactSelectDialog;
import org.histo.action.dialog.notification.NotificationDialog;
import org.histo.action.dialog.patient.AddPatientDialogHandler;
import org.histo.action.dialog.patient.CreateTaskDialog;
import org.histo.action.dialog.patient.DeleteTaskDialog;
import org.histo.action.dialog.patient.EditPatientDialog;
import org.histo.action.dialog.print.CustomAddressDialog;
import org.histo.action.dialog.print.FaxPrintDocumentDialog;
import org.histo.action.dialog.print.PrintDialog;
import org.histo.action.dialog.settings.PhysicianEditDialog;
import org.histo.action.dialog.settings.PhysicianSearchDialog;
import org.histo.action.dialog.slide.SlideOverviewDialog;
import org.histo.action.dialog.slide.StainingExitStayInPhaseDialog;
import org.histo.action.dialog.slide.StainingPhaseExitDialog;
import org.histo.action.dialog.task.ChangeMaterialDialog;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Setter;

@Component
@Scope(value = "session")
@Setter
public class DialogHandlerAction {

	private OrganizationListDialog organizationListDialog;

	private WorklistSearchDialog worklistSearchDialog;

	private PrintDialog printDialog;

	private CustomAddressDialog customAddressDialog;

	private ContactSelectDialog contactSelectDialog;

	private ContactDialog contactDialog;

	private ContactNotificationDialog contactNotificationDialog;

	private ChangeMaterialDialog changeMaterialDialog;

	private NotificationDialog notificationDialog;

	private MediaDialog mediaDialog;

	private FaxPrintDocumentDialog faxPrintDocumentDialog;

	private DeleteTaskDialog deleteTaskDialog;

	private CreateTaskDialog createTaskDialog;

	private EditPatientDialog editPatientDialog;

	private AddPatientDialogHandler addPatientDialogHandler;

	private FavouriteListsDialog favouriteListsDialog;

	private AddTaskToFavouriteListDialog addTaskToFavouriteListDialog;

	private SlideOverviewDialog slideOverviewDialog;

	private DiagnosisPhaseExitDialog diagnosisPhaseExitDialog;

	private DiagnosisExitStayInPhaseDialog diagnosisExitStayInPhaseDialog;

	private DiagnosisUnFinalizeDialog diagnosisUnFinalizeDialog;

	private StainingPhaseExitDialog stainingPhaseExitDialog;

	private StainingExitStayInPhaseDialog stainingExitStayInPhaseDialog;

	private PhysicianSearchDialog physicianSearchDialog;

	private PhysicianEditDialog physicianEditDialog;

	public OrganizationListDialog getOrganizationListDialog() {
		if (organizationListDialog == null)
			organizationListDialog = new OrganizationListDialog();

		return organizationListDialog;
	}

	public WorklistSearchDialog getWorklistSearchDialog() {
		if (worklistSearchDialog == null)
			worklistSearchDialog = new WorklistSearchDialog();

		return worklistSearchDialog;
	}

	public PrintDialog getPrintDialog() {
		if (printDialog == null)
			printDialog = new PrintDialog();

		return printDialog;
	}

	public CustomAddressDialog getCustomAddressDialog() {
		if (customAddressDialog == null)
			customAddressDialog = new CustomAddressDialog();

		return customAddressDialog;
	}

	public ContactSelectDialog getContactSelectDialog() {
		if (contactSelectDialog == null)
			contactSelectDialog = new ContactSelectDialog();

		return contactSelectDialog;
	}

	public ContactDialog getContactDialog() {
		if (contactDialog == null)
			contactDialog = new ContactDialog();

		return contactDialog;
	}

	public ContactNotificationDialog getContactNotificationDialog() {
		if (contactNotificationDialog == null)
			contactNotificationDialog = new ContactNotificationDialog();

		return contactNotificationDialog;
	}

	public ChangeMaterialDialog getChangeMaterialDialog() {
		if (changeMaterialDialog == null)
			changeMaterialDialog = new ChangeMaterialDialog();

		return changeMaterialDialog;
	}

	public NotificationDialog getNotificationDialog() {
		if (notificationDialog == null)
			notificationDialog = new NotificationDialog();

		return notificationDialog;
	}

	public MediaDialog getMediaDialog() {
		if (mediaDialog == null)
			mediaDialog = new MediaDialog();

		return mediaDialog;
	}

	public FaxPrintDocumentDialog getFaxPrintDocumentDialog() {
		if (faxPrintDocumentDialog == null)
			faxPrintDocumentDialog = new FaxPrintDocumentDialog();

		return faxPrintDocumentDialog;
	}

	public DeleteTaskDialog getDeleteTaskDialog() {
		if (deleteTaskDialog == null) {
			deleteTaskDialog = new DeleteTaskDialog();
		}

		return deleteTaskDialog;
	}

	public CreateTaskDialog getCreateTaskDialog() {
		if (createTaskDialog == null) {
			createTaskDialog = new CreateTaskDialog();
		}

		return createTaskDialog;
	}

	public EditPatientDialog getEditPatientDialog() {
		if (editPatientDialog == null) {
			editPatientDialog = new EditPatientDialog();
		}

		return editPatientDialog;
	}

	public AddPatientDialogHandler getAddPatientDialogHandler() {
		if (addPatientDialogHandler == null) {
			addPatientDialogHandler = new AddPatientDialogHandler();
		}

		return addPatientDialogHandler;
	}

	public FavouriteListsDialog getFavouriteListsDialog() {
		if (favouriteListsDialog == null) {
			favouriteListsDialog = new FavouriteListsDialog();
		}

		return favouriteListsDialog;
	}

	public SlideOverviewDialog getSlideOverviewDialog() {
		if (slideOverviewDialog == null) {
			slideOverviewDialog = new SlideOverviewDialog();
		}

		return slideOverviewDialog;
	}

	public DiagnosisPhaseExitDialog getDiagnosisPhaseExitDialog() {
		if (diagnosisPhaseExitDialog == null) {
			diagnosisPhaseExitDialog = new DiagnosisPhaseExitDialog();
		}

		return diagnosisPhaseExitDialog;
	}

	public DiagnosisExitStayInPhaseDialog getDiagnosisExitStayInPhaseDialog() {
		if (diagnosisExitStayInPhaseDialog == null) {
			diagnosisExitStayInPhaseDialog = new DiagnosisExitStayInPhaseDialog();
		}

		return diagnosisExitStayInPhaseDialog;
	}

	public StainingPhaseExitDialog getStainingPhaseExitDialog() {
		if (stainingPhaseExitDialog == null) {
			stainingPhaseExitDialog = new StainingPhaseExitDialog();
		}

		return stainingPhaseExitDialog;
	}

	public StainingExitStayInPhaseDialog getStainingExitStayInPhaseDialog() {
		if (stainingExitStayInPhaseDialog == null) {
			stainingExitStayInPhaseDialog = new StainingExitStayInPhaseDialog();
		}

		return stainingExitStayInPhaseDialog;
	}

	public DiagnosisUnFinalizeDialog getDiagnosisUnFinalizeDialog() {
		if (diagnosisUnFinalizeDialog == null) {
			diagnosisUnFinalizeDialog = new DiagnosisUnFinalizeDialog();
		}

		return diagnosisUnFinalizeDialog;
	}

	public AddTaskToFavouriteListDialog getAddTaskToFavouriteListDialog() {
		if (addTaskToFavouriteListDialog == null) {
			addTaskToFavouriteListDialog = new AddTaskToFavouriteListDialog();
		}

		return addTaskToFavouriteListDialog;
	}

	public PhysicianSearchDialog getPhysicianSearchDialog() {
		if (physicianSearchDialog == null) {
			physicianSearchDialog = new PhysicianSearchDialog();
		}

		return physicianSearchDialog;
	}

	public PhysicianEditDialog getPhysicianEditDialog() {
		if (physicianEditDialog == null) {
			physicianEditDialog = new PhysicianEditDialog();
		}

		return physicianEditDialog;
	}

}
