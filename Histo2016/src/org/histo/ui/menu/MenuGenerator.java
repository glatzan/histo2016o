package org.histo.ui.menu;

import java.util.List;

import org.apache.log4j.Logger;
import org.histo.action.UserHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.primefaces.model.menu.DefaultMenuItem;
import org.primefaces.model.menu.DefaultMenuModel;
import org.primefaces.model.menu.DefaultSeparator;
import org.primefaces.model.menu.DefaultSubMenu;
import org.primefaces.model.menu.MenuModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
public class MenuGenerator {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ResourceBundle resourceBundle;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	private static Logger logger = Logger.getLogger("org.histo");

	public MenuModel generateEditMenu(Patient patient, Task task) {
		logger.debug("Generating new MenuModel");

		MenuModel model = new DefaultMenuModel();

		// patient menu
		{
			// patient menu
			DefaultSubMenu patientSubMenu = new DefaultSubMenu(resourceBundle.get("header.menu.patient"));
			model.addElement(patientSubMenu);

			// add patient
			DefaultMenuItem item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.new"));
			item.setOnclick(
					"$('#headerForm\\\\:addPatientButton').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-user");
			patientSubMenu.addElement(item);

			// separator
			DefaultSeparator seperator = new DefaultSeparator();
			seperator.setRendered(patient != null);
			patientSubMenu.addElement(seperator);

			// patient overview
			item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.overview"));
			item.setOnclick(
					"$('#headerForm\\\\:showPatientOverview').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-tablet");
			item.setRendered(patient != null);
			patientSubMenu.addElement(item);

			// patient edit data, disabled if not external patient
			item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.edit"));
			item.setOnclick(
					"$('#headerForm\\\\:editPatientData').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-pencil-square-o");
			item.setRendered(patient != null);
			item.setDisabled(!patient.isExternalPatient());
			// TODO comment that patient is not edtiable
			patientSubMenu.addElement(item);

			// patient upload pdf
			item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.upload"));
			item.setOnclick(
					"$('#headerForm\\\\:uploadBtnToPatient').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-cloud-upload");
			item.setRendered(patient != null);
			patientSubMenu.addElement(item);
		}

		// task menu
		if (patient != null) {

			boolean taskIsNull = task == null;
			boolean taskIsEditable = task.getTaskStatus().isEditable();

			DefaultSubMenu taskSubMenu = new DefaultSubMenu(resourceBundle.get("header.menu.task"));
			model.addElement(taskSubMenu);

			// new task
			DefaultMenuItem item = new DefaultMenuItem(resourceBundle.get("header.menu.task.create"));
			item.setOnclick(
					"$('#headerForm\\\\:newTaskBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-file");
			item.setStyleClass("headerSubMenuStyle");
			taskSubMenu.addElement(item);

			// new sample, if task is not null
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.create"));
			item.setOnclick(
					"$('#headerForm\\\\:newSampleBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-eyedropper");
			item.setRendered(!taskIsNull);
			item.setDisabled(!taskIsEditable);
			item.setStyleClass("headerSubMenuStyle");
			taskSubMenu.addElement(item);

			// staining submenu
			if (!taskIsNull) {
				DefaultSubMenu stainingSubMenu = new DefaultSubMenu(
						resourceBundle.get("header.menu.task.sample.staining"));
				stainingSubMenu.setIcon("fa fa-paint-brush");
				taskSubMenu.addElement(stainingSubMenu);

				// new slide
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.staining.newSlide"));
				item.setOnclick(
						"$('#headerForm\\\\:stainingOverview').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-paint-brush");
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable);
				stainingSubMenu.addElement(item);

				// leave staining phase and set all stainings to
				// completed
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.staining.leave"));
				item.setOnclick(
						"$('#headerForm\\\\:stainingPhaseExit').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-image");
				item.setStyleClass("headerSubMenuStyle");
				item.setRendered(task.getTaskStatus().isStainingNeeded() || task.getTaskStatus().isReStainingNeeded());
				item.setDisabled(!taskIsEditable);
				stainingSubMenu.addElement(item);

				// Staining, end staining phase if all staining tasks
				// have been completed
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.staining.stayInPhase.leave"));
				item.setOnclick(
						"$('#headerForm\\\\:stainingPhaseForceExitStayInPhase').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-image");
				item.setRendered(task.getTaskStatus().isReStainingNeeded());
				item.setStyleClass("headerSubMenuStyle");
				stainingSubMenu.addElement(item);
			}

			// diagnosis menu
			if (!taskIsNull) {
				DefaultSubMenu diagnosisSubMenu = new DefaultSubMenu(
						resourceBundle.get("header.menu.task.sample.diagnosis"));
				diagnosisSubMenu.setIcon("fa fa-search");
				taskSubMenu.addElement(diagnosisSubMenu);

				// new diagnosis
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.diagnosisRevion"));
				item.setOnclick(
						"$('#headerForm\\\\:newDiagnosisRevision').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-pencil-square-o");
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable
						&& !(task.getTaskStatus().isDiagnosisNeeded() || task.getTaskStatus().isReDiagnosisNeeded()));
				diagnosisSubMenu.addElement(item);

				// Leave diagnosis phase if in phase an not complete
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.diagnosisPhase.leave"));
				item.setOnclick(
						"$('#headerForm\\\\:diagnosisPhaseExit').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-eye-slash");
				item.setStyleClass("headerSubMenuStyle");
				item.setRendered(
						task.getTaskStatus().isDiagnosisNeeded() || task.getTaskStatus().isReDiagnosisNeeded());
				item.setDisabled(!taskIsEditable);
				diagnosisSubMenu.addElement(item);

				// Leave phase if stay in phase
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.diagnosisPhase.force.leave"));
				item.setOnclick(
						"$('#headerForm\\\\:diagnosisExitStayInPhase').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setRendered(task.getTaskStatus().isStayInDiagnosisList() && task.isFinalized());
				item.setIcon("fa fa-eye-slash");
				item.setStyleClass("headerSubMenuStyle");
				diagnosisSubMenu.addElement(item);

				// council
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.council"));
				item.setOnclick(
						"$('#headerForm\\\\:councilBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-comment-o");
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable);
				diagnosisSubMenu.addElement(item);
			}

			// notification submenu
			if (!taskIsNull) {
				DefaultSubMenu notificationSubMenu = new DefaultSubMenu(
						resourceBundle.get("header.menu.task.sample.notification"));
				notificationSubMenu.setIcon("fa fa-volume-up");
				taskSubMenu.addElement(notificationSubMenu);

				// contacts
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.contact"));
				item.setOnclick(
						"$('#headerForm\\\\:editContactBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-envelope-o");
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable);
				notificationSubMenu.addElement(item);

				// report
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.notification"));
				item.setOnclick(
						"$('#headerForm\\\\:medicalFindingsContactBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-volume-up");
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable);
				notificationSubMenu.addElement(item);

				// print
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.print"));
				item.setOnclick(
						"$('#headerForm\\\\:printBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-print");
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable);
				notificationSubMenu.addElement(item);
			}

			// finalized
			{
				DefaultSeparator seperator = new DefaultSeparator();
				seperator.setRendered(task.isFinalized());
				taskSubMenu.addElement(seperator);

				// unfinalize task
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.unFinalize"));
				item.setOnclick(
						"$('#headerForm\\\\:diagnosisPhaseUnFinalize').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-eye");
				item.setRendered(task.isFinalized());
				item.setStyleClass("headerSubMenuStyle");
				item.setDisabled(!taskIsEditable);
				taskSubMenu.addElement(item);
			}

			DefaultSeparator seperator = new DefaultSeparator();
			seperator.setRendered(!taskIsNull);
			taskSubMenu.addElement(seperator);

			// Favorite lists
			if (!taskIsNull) {
				DefaultSubMenu favouriteSubMenu = new DefaultSubMenu("F. lists");
				favouriteSubMenu.setIcon("fa fa-search");
				taskSubMenu.addElement(favouriteSubMenu);

				List<FavouriteList> lists = favouriteListDAO
						.getFavouriteListsWithTasksForUser(userHandlerAction.getCurrentUser(), true, false);

				for (FavouriteList favouriteList : lists) {
					item = new DefaultMenuItem(favouriteList.getName());
					item.setIcon("fa fa-pencil-square-o");
					item.setStyleClass("headerSubMenuStyle icon-red");
					favouriteSubMenu.addElement(item);
				}
			}

			// accounting
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.accounting"));
			item.setIcon("fa fa-dollar");
			item.setStyleClass("headerSubMenuStyle");
			item.setDisabled(true);
			taskSubMenu.addElement(item);

			// biobank
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.biobank"));
			item.setOnclick(
					"$('#headerForm\\\\:bioBankBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-leaf");
			item.setStyleClass("headerSubMenuStyle");
			item.setDisabled(!taskIsEditable);
			taskSubMenu.addElement(item);

			// upload
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.upload"));
			item.setOnclick(
					"$('#headerForm\\\\:uploadBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-cloud-upload");
			item.setStyleClass("headerSubMenuStyle");
			item.setDisabled(!taskIsEditable);
			taskSubMenu.addElement(item);

			// log
			item = new DefaultMenuItem(resourceBundle.get("header.menu.log"));
			item.setOnclick(
					"$('#headerForm\\\\:logBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setStyleClass("headerSubMenuStyle");
			model.addElement(item);

		}

		return model;

	}
}
