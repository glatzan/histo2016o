package org.histo.ui.menu;

import java.util.List;

import javax.faces.component.html.HtmlPanelGroup;

import org.apache.log4j.Logger;
import org.histo.action.UserHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.dao.FavouriteListDAO;
import org.histo.model.dto.FavouriteListMenuItem;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.model.user.HistoPermissions;
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

	public MenuModel generateEditMenu(Patient patient, Task task, HtmlPanelGroup taskMenuCommandButtons) {
		logger.debug("Generating new MenuModel");

		MenuModel model = new DefaultMenuModel();

		if (taskMenuCommandButtons == null) {
			logger.error("No button container connected!");
			return model;
		} else
			// clearing command button array
			taskMenuCommandButtons.getChildren().clear();

		// patient menu
		{

			boolean PATIENT_EDIT = userHandlerAction.currentUserHasPermission(HistoPermissions.PATIENT_EDIT);

			// patient menu
			DefaultSubMenu patientSubMenu = new DefaultSubMenu(resourceBundle.get("header.menu.patient"));
			model.addElement(patientSubMenu);

			// add patient
			DefaultMenuItem item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.new"));
			item.setOnclick(getOnClickCommand("addPatientButton"));
			item.setIcon("fa fa-user");
			item.setRendered(PATIENT_EDIT);
			patientSubMenu.addElement(item);

			// separator
			DefaultSeparator seperator = new DefaultSeparator();
			seperator.setRendered(PATIENT_EDIT);
			patientSubMenu.addElement(seperator);

			// patient overview
			item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.overview"));
			item.setOnclick(getOnClickCommand("showPatientOverview"));
			item.setIcon("fa fa-tablet");
			item.setDisabled(patient == null);
			patientSubMenu.addElement(item);

			if (patient != null && PATIENT_EDIT) {

				DefaultSubMenu administerSubMenu = new DefaultSubMenu(
						resourceBundle.get("header.menu.patient.administer"));
				administerSubMenu.setIcon("fa fa-male");
				patientSubMenu.addElement(administerSubMenu);

				// patient edit data, disabled if not external patient
				item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.edit"));
				item.setOnclick(
						"$('#headerForm\\\\:editPatientData').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-pencil-square-o");
				item.setDisabled(!patient.isExternalPatient());
				// TODO comment that patient is not edtiable
				administerSubMenu.addElement(item);

				// remove patient if empty tasks
				item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.remove"));
				item.setOnclick(
						"$('#headerForm\\\\:removePatient').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-trash");
				item.setDisabled(!patient.getTasks().isEmpty());
				administerSubMenu.addElement(item);

				// patient merge
				// TODO introduce right for merging
				item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.merge"));
				item.setOnclick(
						"$('#headerForm\\\\:mergePatientData').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-medkit");
				administerSubMenu.addElement(item);

				// patient upload pdf
				item = new DefaultMenuItem(resourceBundle.get("header.menu.patient.upload"));
				item.setOnclick(
						"$('#headerForm\\\\:uploadBtnToPatient').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-cloud-upload");
				patientSubMenu.addElement(item);

			}
		}

		// task menu
		if (patient != null && userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT)) {

			boolean taskIsNull = task == null;

			if (!taskIsNull && task.getTaskStatus() == null) {
				task.generateTaskStatus();
			}

			boolean taskIsEditable = taskIsNull ? false : task.getTaskStatus().isEditable();

			DefaultSubMenu taskSubMenu = new DefaultSubMenu(resourceBundle.get("header.menu.task"));
			model.addElement(taskSubMenu);

			// new task
			DefaultMenuItem item = new DefaultMenuItem(resourceBundle.get("header.menu.task.create"));
			item.setOnclick(
					"$('#headerForm\\\\:newTaskBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-file");
			item.setRendered(userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT_NEW));
			taskSubMenu.addElement(item);

			// new sample, if task is not null
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.create"));
			item.setOnclick(
					"$('#headerForm\\\\:newSampleBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-eyedropper");
			item.setRendered(!taskIsNull);
			item.setDisabled(!taskIsEditable);
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
				item.setDisabled(!taskIsEditable);
				stainingSubMenu.addElement(item);

				// ***************************************************
				DefaultSeparator seperator = new DefaultSeparator();
				seperator.setRendered(task.getTaskStatus().isEditable());
				stainingSubMenu.addElement(seperator);

				// leave staining phase regularly
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.staining.exit"));
				item.setOnclick(
						"$('#headerForm\\\\:stainingPhaseExit').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-image");
				item.setRendered(task.getTaskStatus().isStainingNeeded() || task.getTaskStatus().isReStainingNeeded());
				item.setDisabled(!taskIsEditable);
				stainingSubMenu.addElement(item);

				// Remove from staining phase
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.staining.exitStayInPhase"));
				item.setCommand(
						"#{globalEditViewHandler.removeTaskFromFavouriteList(globalEditViewHandler.selectedTask, "
								+ PredefinedFavouriteList.StayInStainingList.getId() + ")}");
				item.setIcon("fa fa-image");
				item.setRendered(task.getTaskStatus().isStayInStainingList());
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
				stainingSubMenu.addElement(item);

				// Add to stay in staining phase
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.staining.enter"));
				item.setOnclick(
						"$('#headerForm\\\\:stainingPhaseEnter').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-image");
				item.setRendered(!(task.getTaskStatus().isStainingNeeded() || task.getTaskStatus().isReStainingNeeded()
						|| task.getTaskStatus().isStayInStainingList()) && task.getTaskStatus().isEditable());
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
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
				item.setDisabled(!taskIsEditable);
				diagnosisSubMenu.addElement(item);

				// council
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.council"));
				item.setOnclick(
						"$('#headerForm\\\\:councilBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-comment-o");
				diagnosisSubMenu.addElement(item);

				// ***************************************************
				DefaultSeparator seperator = new DefaultSeparator();
				seperator.setRendered(task.getTaskStatus().isEditable());
				diagnosisSubMenu.addElement(seperator);

				// Leave diagnosis phase if in phase an not complete
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.diagnosisPhase.exit"));
				item.setOnclick(
						"$('#headerForm\\\\:diagnosisPhaseExit').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-eye-slash");
				item.setRendered(
						task.getTaskStatus().isDiagnosisNeeded() || task.getTaskStatus().isReDiagnosisNeeded());
				item.setDisabled(!taskIsEditable);
				diagnosisSubMenu.addElement(item);

				// Leave phase if stay in phase
				item = new DefaultMenuItem(
						resourceBundle.get("header.menu.task.sample.diagnosisPhase.exitStayInPhase"));
				item.setCommand(
						"#{globalEditViewHandler.removeTaskFromFavouriteList(globalEditViewHandler.selectedTask, "
								+ PredefinedFavouriteList.StayInDiagnosisList.getId() + ")}");
				item.setRendered(task.getTaskStatus().isStayInDiagnosisList());
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
				item.setIcon("fa fa-eye-slash");
				diagnosisSubMenu.addElement(item);

				// enter diagnosis pahse
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.diagnosisPhase.enter"));
				item.setOnclick(
						"$('#headerForm\\\\:diagnosisPhaseEnter').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setRendered(
						!(task.getTaskStatus().isDiagnosisNeeded() || task.getTaskStatus().isReDiagnosisNeeded()
								|| task.getTaskStatus().isStayInDiagnosisList()) && task.getTaskStatus().isEditable());
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
				item.setIcon("fa fa-eye");
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
				notificationSubMenu.addElement(item);

				// print
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.print"));
				item.setOnclick(
						"$('#headerForm\\\\:printBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-print");
				notificationSubMenu.addElement(item);

				// ***************************************************
				DefaultSeparator seperator = new DefaultSeparator();
				seperator.setRendered(task.getTaskStatus().isEditable());
				notificationSubMenu.addElement(seperator);

				// notification perform
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.perform"));
				item.setOnclick(
						"$('#headerForm\\\\:notificationPerformBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-volume-up");
				item.setRendered(!task.getTaskStatus().isFinalized());
				notificationSubMenu.addElement(item);

				// exit notification phase, without performing notification
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.exit"));
				item.setOnclick(
						"$('#headerForm\\\\:notificationPhaseExit').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
				item.setIcon("fa fa-volume-off");
				item.setRendered(
						task.getTaskStatus().isNotificationNeeded() || task.getTaskStatus().isStayInNotificationList());
				notificationSubMenu.addElement(item);

				// exit stay in notification phase
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.enter"));
				item.setCommand(
						"#{globalEditViewHandler.removeTaskFromFavouriteList(globalEditViewHandler.selectedTask, "
								+ PredefinedFavouriteList.StayInNotificationList.getId() + ")}");
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
				item.setIcon("fa fa-volume-off");
				item.setRendered(task.getTaskStatus().isStayInNotificationList());
				notificationSubMenu.addElement(item);

				// add to notification phase
				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.notification.enter"));
				item.setCommand("#{globalEditViewHandler.addTaskToFavouriteList(globalEditViewHandler.selectedTask, "
						+ PredefinedFavouriteList.NotificationList.getId() + ")}");
				item.setOncomplete("updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
				item.setUpdate("navigationForm:patientList contentForm headerForm");
				item.setIcon("fa fa-volume-off");
				item.setRendered(!(task.getTaskStatus().isNotificationNeeded()
						|| task.getTaskStatus().isStayInNotificationList()) && task.getTaskStatus().isEditable());
				notificationSubMenu.addElement(item);

			}

			// finalized
			if (!taskIsNull) {
				DefaultSeparator seperator = new DefaultSeparator();
				seperator.setRendered(userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT_ARCHIVE,
						HistoPermissions.TASK_EDIT_RESTORE));
				taskSubMenu.addElement(seperator);

				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.archive"));
				item.setOnclick(
						"$('#headerForm\\\\:archiveTaskBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-archive");
				item.setRendered(!task.isFinalized()
						&& userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT_ARCHIVE));
				taskSubMenu.addElement(item);

				item = new DefaultMenuItem(resourceBundle.get("header.menu.task.restore"));
				item.setOnclick(
						"$('#headerForm\\\\:restoreTaskBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
				item.setIcon("fa fa-dropbox");
				item.setRendered(task.isFinalized()
						&& userHandlerAction.currentUserHasPermission(HistoPermissions.TASK_EDIT_RESTORE));
				taskSubMenu.addElement(item);
			}

			DefaultSeparator seperator = new DefaultSeparator();
			seperator.setRendered(!taskIsNull);
			taskSubMenu.addElement(seperator);

			// Favorite lists
			if (!taskIsNull) {

				List<FavouriteListMenuItem> items = favouriteListDAO.getMenuItems(userHandlerAction.getCurrentUser(),
						task);

				// only render of size > 0
				if (items.size() > 0) {
					DefaultSubMenu favouriteSubMenu = new DefaultSubMenu("F. lists");
					favouriteSubMenu.setIcon("fa fa-list-alt");
					taskSubMenu.addElement(favouriteSubMenu);

					DefaultSubMenu hiddenFavouriteSubMenu = new DefaultSubMenu("Versteckte Listen");
					favouriteSubMenu.setIcon("fa fa-list-alt");

					for (FavouriteListMenuItem favouriteListItem : items) {
						item = new DefaultMenuItem(favouriteListItem.getName());

						// if the favourite lists contains the task, option to remove ist
						if (favouriteListItem.isContainsTask()) {
							item.setIcon("fa fa-check-circle icon-green");

							// if favourite has a dumplist, open the dialog for moving the task to the
							// dumplist
							if (favouriteListItem.isDumpList()) {

								MethodSignature signature = new MethodSignature(
										"favouriteListItemRemoveDialog.initAndPrepareBean",
										new VaribaleHolder<Task>(task, "task"),
										new VaribaleHolder<Long>(favouriteListItem.getId(), "favListId"));

								signature.generateButton().addAjaxBehaviorToButton("dialogReturn", new MethodSignature(
										"globalEditViewHandler.updateDataOfTask",
										"navigationForm:patientList contentForm headerForm", null,
										"updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll');",
										new VaribaleHolder<Boolean>(Boolean.TRUE, "valu1"),
										new VaribaleHolder<Boolean>(Boolean.TRUE, "valu1"),
										new VaribaleHolder<Boolean>(Boolean.FALSE, "valu2"),
										new VaribaleHolder<Boolean>(Boolean.FALSE, "valu3")))
										.addToParent(taskMenuCommandButtons);

								// onlick active the command button
								item.setOnclick("$('#headerForm\\\\:" + signature.getButtonId()
										+ "').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");

							} else {
								item.setCommand(
										"#{globalEditViewHandler.removeTaskFromFavouriteList(globalEditViewHandler.selectedTask, "
												+ favouriteListItem.getId() + ")}");
								item.setOncomplete(
										"updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
								item.setUpdate("navigationForm:patientList contentForm headerForm");
							}
						} else {
							item.setIcon("fa fa-circle-o");
							item.setCommand(
									"#{globalEditViewHandler.addTaskToFavouriteList(globalEditViewHandler.selectedTask, "
											+ favouriteListItem.getId() + ")}");
							item.setOncomplete(
									"updateAndAutoScrollToSelectedElement('navigationForm:patientNavigationScroll')");
							item.setUpdate("navigationForm:patientList contentForm headerForm");
						}

						if (favouriteListItem.isHidden())
							hiddenFavouriteSubMenu.addElement(item);
						else
							favouriteSubMenu.addElement(item);
					}
					
					// only adding hidden lists if one list is present
					if(hiddenFavouriteSubMenu.getElements().size() > 0) {
						favouriteSubMenu.addElement(new DefaultSeparator());
						favouriteSubMenu.addElement(hiddenFavouriteSubMenu);
					}

				} else {
					item = new DefaultMenuItem("Keine F. Listen");
					item.setIcon("fa fa-list-alt");
					item.setDisabled(true);
					taskSubMenu.addElement(item);
				}

			}

			// accounting
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.sample.accounting"));
			item.setIcon("fa fa-dollar");

			item.setDisabled(true);
			taskSubMenu.addElement(item);

			// biobank
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.biobank"));
			item.setOnclick(
					"$('#headerForm\\\\:bioBankBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-leaf");

			item.setDisabled(!taskIsEditable);
			taskSubMenu.addElement(item);

			// upload
			item = new DefaultMenuItem(resourceBundle.get("header.menu.task.upload"));
			item.setOnclick(
					"$('#headerForm\\\\:uploadBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");
			item.setIcon("fa fa-cloud-upload");

			taskSubMenu.addElement(item);

			// log
			item = new DefaultMenuItem(resourceBundle.get("header.menu.log"));
			item.setOnclick(
					"$('#headerForm\\\\:logBtn').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;");

			model.addElement(item);
		}

		return model;

	}

	private String getOnClickCommand(String id) {
		return "$('#headerForm\\\\:" + id + "').click();$('#headerForm\\\\:taskTieredMenuButton').hide();return false;";
	}
}
