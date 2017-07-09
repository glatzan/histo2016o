package org.histo.action.dialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.CommonDataHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.action.handler.SettingsHandler;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.SettingsTab;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.dao.LogDAO;
import org.histo.dao.OrganizationDAO;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.UserDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.Contact;
import org.histo.model.DiagnosisPreset;
import org.histo.model.FavouriteList;
import org.histo.model.FavouriteListItem;
import org.histo.model.HistoUser;
import org.histo.model.ListItem;
import org.histo.model.Log;
import org.histo.model.MaterialPreset;
import org.histo.model.Organization;
import org.histo.model.StainingPrototype;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.patient.Patient;
import org.histo.ui.ListChooser;
import org.histo.ui.transformer.AssociatedRoleTransformer;
import org.histo.ui.transformer.DefaultTransformer;
import org.primefaces.event.ReorderEvent;
import org.primefaces.event.TabChangeEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@Scope(value = "session")
public class SettingsDialogHandler extends AbstractDialog {

	private static Logger logger = Logger.getLogger("org.histo");

	public static final int DIAGNOSIS_LIST = 0;
	public static final int DIAGNOSIS_EDIT = 1;
	public static final int DIAGNOSIS_TEXT_TEMPLATE = 2;

	@Autowired
	private UserDAO userDAO;

	@Autowired
	private GenericDAO genericDAO;

	@Autowired
	private UtilDAO utilDAO;

	@Autowired
	private PhysicianDAO physicianDAO;

	@Autowired
	private MainHandlerAction mainHandlerAction;

	@Autowired
	private ResourceBundle resourceBundle;

	@Autowired
	private UserHandlerAction userHandlerAction;

	@Autowired
	private CommonDataHandlerAction commonDataHandlerAction;

	@Autowired
	private SettingsHandler settingsHandler;

	@Autowired
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	private OrganizationDAO organizationDAO;

	@Autowired
	private LogDAO logDAO;

	/**
	 * Tabindex of settings dialog
	 */
	@Getter
	@Setter
	private int activeSettingsIndex = 0;

	@Getter
	@Setter
	public AbstractSettingsTab[] tabs = new AbstractSettingsTab[] { new HistoUserTab(), new DiagnosisTab(),
			new MaterialTab(), new StainingTab(), new StaticListTab(), new FavouriteListTab(), new OrganizationTab(),
			new LogTab() };

	public enum Tabs {
		HistUserTab(HistoUserTab.class), DiagnosisTab(DiagnosisTab.class), MaterialTab(MaterialTab.class), StainingTab(
				StainingTab.class), StaticListTab(StaticListTab.class), FavouriteListTab(
						FavouriteListTab.class), OrganizationTab(OrganizationTab.class), LogTab(LogTab.class);

		@Getter
		private final Class<? extends AbstractSettingsTab> tabClass;

		Tabs(final Class<? extends AbstractSettingsTab> tabClass) {
			this.tabClass = tabClass;
		}
	}

	/**
	 * Tabindex of the favouriteList
	 */
	private SettingsTab favouriteListTabIndex = SettingsTab.F_LIST;

	/********************************************************
	 * General
	 ********************************************************/

	public void initAndPrepareBean() {
		if (initBean(getActiveSettingsIndex()))
			prepareDialog();
	}

	public boolean initBean(int activeTab) {

		if (activeTab >= 0 && activeTab < getTabs().length) {
			setActiveSettingsIndex(activeTab);

			onSettingsTabChange(null);

			commonDataHandlerAction.setAssociatedRoles(Arrays.asList(ContactRole.values()));
			commonDataHandlerAction.setAssociatedRolesTransformer(
					new AssociatedRoleTransformer(commonDataHandlerAction.getAssociatedRoles()));
		} else {
			return false;
		}

		super.initBean(task, Dialog.SETTINGS);

		return true;
	}

	public void onSettingsTabChange(TabChangeEvent event) {

		logger.debug("Current Tab index is " + getActiveSettingsIndex());
		if (event != null)
			System.out.println("Active Tab: " + event.getTab().getTitle());

		if (getActiveSettingsIndex() >= 0 && getActiveSettingsIndex() < getTabs().length) {
			getTabs()[getActiveSettingsIndex()].updateData();
		}

		//
		// else if (getActiveSettingsIndex() ==
		// SettingsTab.PHYSICIAN.getTabNumber()) {
		// preparePhysicianList();
		// } else if (getActiveSettingsIndex() ==
		// SettingsTab.STATIC_LISTS.getTabNumber()) {
		// prepareStaticLists();
		// } else if (getActiveSettingsIndex() ==
		// SettingsTab.FAVOURITE_LIST.getTabNumber()) {
		// if (getFavouriteListTabIndex() == SettingsTab.F_EDIT) {
		// if (getTmpFavouriteList() != null && getTmpFavouriteList().getId() !=
		// 0)
		// // reload fav list
		// prepareEditFavouriteList(getTmpFavouriteList());
		// } else
		// prepareFavouriteLists();
		// }

	}

	public AbstractSettingsTab getTab(Tabs classTab) {
		for (AbstractSettingsTab abstractSettingsTab : tabs) {
			if (abstractSettingsTab.getClass() == classTab.getTabClass())
				return abstractSettingsTab;
		}

		return null;
	}

	/**
	 * Show the adminSettigns Dialog and inits the used values
	 */

	public void prepareSettingsDialog() {
		prepareSettingsDialog(getActiveSettingsIndex());
	}

	public void prepareSettingsDialog(SettingsTab settingsTab) {
		// SettingsTab parentTab = settingsTab.getParent() != null ?
		// settingsTab.getParent() : settingsTab;
		//
		// switch (parentTab) {
		// case PHYSICIAN:
		// setPhysicianTabIndex(settingsTab);
		// break;
		// default:
		// break;
		// }
		//
		// prepareSettingsDialog(settingsTab.getTabNumber());
	}

	public void prepareSettingsDialog(int activeTab) {
		super.initBean(null, Dialog.SETTINGS);

		mainHandlerAction.showDialog(Dialog.SETTINGS);
	}

	/********************************************************
	 * General
	 ********************************************************/

	/********************************************************
	 * History
	 ********************************************************/
	/**
	 * Loads the current history, for all events 100 entries. Shows the current
	 * history dialog.
	 */
	public void loadGeneralHistory() {
		// setCurrentHistory(utilDAO.getCurrentHistory(100));
	}

	/**
	 * Loads the current history for the given patient. Shows the current
	 * history dialog.
	 * 
	 * @param patient
	 */
	public void loadPatientHistory(Patient patient) {
		// setCurrentHistory(utilDAO.getCurrentHistoryForPatient(100,
		// patient));
	}

	/********************************************************
	 * History
	 ********************************************************/

	@Getter
	@Setter
	public abstract class AbstractSettingsTab {

		public abstract String getCenterView();

		public abstract void updateData();

		protected String name;

		protected String viewID;
	}

	public static enum HistoUserPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class HistoUserTab extends AbstractSettingsTab {

		private HistoUserPage page;

		private List<HistoUser> users;

		private HistoUser selectedUser;

		private List<ContactRole> allRoles;

		public HistoUserTab() {
			setName("dialog.settings.user");
			setViewID("histoUser");
			setPage(HistoUserPage.LIST);
			setAllRoles(Arrays.asList(ContactRole.values()));
		}

		public void updateData() {
			switch (page) {
			case LIST:
				setUsers(userDAO.loadAllUsers());
				break;
			case EDIT:
				setSelectedUser(userDAO.get(HistoUser.class, getSelectedUser().getId()));
			default:
				break;
			}
		}

		public void onChangeUserRole(HistoUser histoUser) {
			try {
				userHandlerAction.roleOfuserHasChanged(histoUser);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * prepares an edit dialog for editing user data, only avaliable for
		 * admins
		 * 
		 * @param physician
		 */
		public void prepareEditHistoUser(HistoUser histoUser) {
			setSelectedUser(histoUser);
			setPage(HistoUserPage.EDIT);

			updateData();
		}

		/**
		 * Saves an edited physician to the database
		 * 
		 * @param physician
		 */
		public void saveHistoUser() {
			try {
				if (getSelectedUser().getPhysician().hasNoAssociateRole())
					getSelectedUser().getPhysician().addAssociateRole(ContactRole.OTHER_PHYSICIAN);

				genericDAO.saveDataRollbackSave(getSelectedUser().getPhysician(),
						resourceBundle.get("log.settings.physician.physician.edit",
								getSelectedUser().getPhysician().getPerson().getFullName()));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Shows the userlist aganin
		 */
		public void discardHistoUser() {
			genericDAO.reset(getSelectedUser());
			setSelectedUser(null);
			setPage(HistoUserPage.LIST);

			updateData();
		}

		@Override
		public String getCenterView() {
			if (getPage() == HistoUserPage.EDIT)
				return "histoUser/userEdit.xhtml";
			else
				return "histoUser/userList.xhtml";
		}

	}

	public enum DiagnosisPage {
		LIST, EDIT, EDIT_TEXT_TEMPLATE;
	}

	@Getter
	@Setter
	public class DiagnosisTab extends AbstractSettingsTab {

		private DiagnosisPage page;

		private List<DiagnosisPreset> diagnosisPresets;

		private DefaultTransformer<DiagnosisPreset> diagnosisPresetsTransformer;

		private DiagnosisPreset selectedDiagnosisPreset;

		private boolean newDiagnosisPreset;

		public DiagnosisTab() {
			setName("dialog.settings.diagnosis");
			setViewID("diagnoses");
			setPage(DiagnosisPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
			case EDIT_TEXT_TEMPLATE:
				if (getSelectedDiagnosisPreset() != null && getSelectedDiagnosisPreset().getId() != 0)
					setSelectedDiagnosisPreset(
							genericDAO.get(DiagnosisPreset.class, getSelectedDiagnosisPreset().getId()));
				break;
			default:
				setDiagnosisPresets(utilDAO.getAllDiagnosisPrototypes());
				break;
			}

		}

		public void prepareNewDiagnosisPreset() {
			prepareEditDiagnosisPreset(new DiagnosisPreset());
		}

		public void prepareEditDiagnosisPreset(DiagnosisPreset diagnosisPreset) {
			setSelectedDiagnosisPreset(diagnosisPreset);
			setPage(DiagnosisPage.EDIT);

			setNewDiagnosisPreset(diagnosisPreset.getId() == 0 ? true : false);

			updateData();
		}

		public void saveDiagnosisPreset() {
			try {
				if (getSelectedDiagnosisPreset().getId() == 0) {

					// case new, save
					logger.debug("Creating new diagnosis " + getSelectedDiagnosisPreset().getCategory());

					getDiagnosisPresets().add(getSelectedDiagnosisPreset());

					genericDAO.saveDataRollbackSave(getSelectedDiagnosisPreset(), resourceBundle
							.get("log.settings.diagnosis.new", getSelectedDiagnosisPreset().getCategory()));

					ListOrder.reOrderList(getDiagnosisPresets());

					genericDAO.saveListRollbackSave(getDiagnosisPresets(),
							resourceBundle.get("log.settings.diagnosis.list.reoder"));

				} else {

					System.out.println(getSelectedDiagnosisPreset().getIndexInList());
					// case edit: update an save
					logger.debug("Updating diagnosis " + getSelectedDiagnosisPreset().getCategory());

					genericDAO.saveDataRollbackSave(getSelectedDiagnosisPreset(), resourceBundle
							.get("log.settings.diagnosis.update", getSelectedDiagnosisPreset().getCategory()));
				}

				discardDiagnosisPreset();

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Discards all changes of a diagnosisPrototype
		 */
		public void discardDiagnosisPreset() {
			if (getSelectedDiagnosisPreset().getId() != 0)
				genericDAO.reset(getSelectedDiagnosisPreset());
			setPage(DiagnosisPage.LIST);
			setSelectedDiagnosisPreset(null);

			updateData();
		}

		public void prepareEditDiagnosisPresetTemplate() {
			setPage(DiagnosisPage.EDIT_TEXT_TEMPLATE);
		}

		public void discardEditDiagnosisPresetTemplate() {
			setPage(DiagnosisPage.EDIT);
		}

		/**
		 * Is fired if the list is reordered by the user via drag and drop
		 *
		 * @param event
		 */
		public void onReorderList(ReorderEvent event) {

			try {
				logger.debug("List order changed, moved material from " + event.getFromIndex() + " to "
						+ event.getToIndex());

				ListOrder.reOrderList(getDiagnosisPresets());

				genericDAO.saveListRollbackSave(getDiagnosisPresets(),
						resourceBundle.get("log.settings.diagnosis.list.reoder"));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "diagnosis/diagnosisEdit.xhtml";
			case EDIT_TEXT_TEMPLATE:
				return "diagnosis/diagnosisEditTemplate.xhtml";
			default:
				return "diagnosis/diagnosisList.xhtml";
			}
		}
	}

	public enum MaterialPage {
		LIST, EDIT, ADD_STAINING;
	}

	@Getter
	@Setter
	public class MaterialTab extends AbstractSettingsTab {

		public MaterialPage page;

		private List<MaterialPreset> allMaterialList;

		/**
		 * StainingPrototype for creating and editing
		 */
		private MaterialPreset editMaterial;

		/**
		 * List for selecting staining, this list contains all stainings. They
		 * can be choosen and added to the material
		 */
		private List<ListChooser<StainingPrototype>> stainingListChooserForMaterial;

		private boolean newMaterial;

		public MaterialTab() {
			setName("dialog.settings.materials");
			setViewID("material");
			setPage(MaterialPage.LIST);
		}

		@Override
		public void updateData() {

			switch (getPage()) {
			case EDIT:
			case ADD_STAINING:
				setStainingListChooserForMaterial(utilDAO.getAllStainingPrototypes().stream()
						.map(p -> new ListChooser<StainingPrototype>(p)).collect(Collectors.toList()));
				break;
			default:
				setAllMaterialList(utilDAO.getAllMaterialPresets(true));
				break;
			}
		}

		/**
		 * Prepares a new StainingListChooser for editing
		 */
		public void prepareNewMaterial() {
			prepareEditMaterial(new MaterialPreset());
		}

		/**
		 * Shows the edit material form
		 *
		 * @param stainingPrototype
		 */
		public void prepareEditMaterial(MaterialPreset material) {
			setPage(MaterialPage.EDIT);
			setEditMaterial(material);

			setNewMaterial(material.getId() == 0 ? true : false);

			updateData();
		}

		/**
		 * Saves a material or creates a new one
		 *
		 * @param newStainingPrototypeList
		 * @param origStainingPrototypeList
		 */
		public void saveMaterial() {
			try {
				if (getEditMaterial().getId() == 0) {
					logger.debug("Creating new Material " + getEditMaterial().getName());
					// case new, save+
					getAllMaterialList().add(getEditMaterial());

					genericDAO.saveDataRollbackSave(getEditMaterial(),
							resourceBundle.get("log.settings.material.new", getEditMaterial().getName()));

					ListOrder.reOrderList(getAllMaterialList());

					genericDAO.saveListRollbackSave(getAllMaterialList(),
							resourceBundle.get("log.settings.material.list.reoder"));

				} else {
					logger.debug("Updating Material " + getEditMaterial().getName());
					// case edit: update an save
					genericDAO.saveDataRollbackSave(getEditMaterial(),
							resourceBundle.get("log.settings.material.update", getEditMaterial().getName()));
				}
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * discards all changes for the stainingList
		 */
		public void discardMaterial() {
			if (getEditMaterial().getId() != 0)
				genericDAO.reset(getEditMaterial());
			setPage(MaterialPage.LIST);
			setEditMaterial(null);

			updateData();
		}

		/**
		 * show a list with all stanings for adding them to a material
		 */
		public void prepareAddStainingToMaterial() {
			setPage(MaterialPage.ADD_STAINING);
		}

		/**
		 * Adds all selected staining prototypes to the material
		 *
		 * @param stainingListChoosers
		 * @param stainingPrototypeList
		 */
		public void addStainingToMaterial() {
			getStainingListChooserForMaterial().forEach(p -> {
				if (p.isChoosen()) {
					getEditMaterial().getStainingPrototypes().add(p.getListItem());
				}
			});
		}

		/**
		 * Removes a staining from a material
		 *
		 * @param toRemove
		 * @param stainingPrototypeList
		 */
		public void removeStainingFromStainingList(StainingPrototype toRemove) {
			getEditMaterial().getStainingPrototypes().remove(toRemove);
		}

		public void discardAddStainingToMaterial() {
			setPage(MaterialPage.EDIT);
		}

		/**
		 * Is fired if the list is reordered by the user via drag and drop
		 *
		 * @param event
		 */
		public void onReorderList(ReorderEvent event) {
			try {
				logger.debug("List order changed, moved material from " + event.getFromIndex() + " to "
						+ event.getToIndex());
				ListOrder.reOrderList(getAllMaterialList());

				genericDAO.saveListRollbackSave(getAllMaterialList(),
						resourceBundle.get("log.settings.staining.list.reoder"));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "material/materialEdit.xhtml";
			case ADD_STAINING:
				return "material/materialAddStaining.xhtml";
			default:
				return "material/materialList.xhtml";
			}
		}
	}

	public enum StainingPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class StainingTab extends AbstractSettingsTab {

		private StainingPage page;

		/**
		 * A List with all staings
		 */
		private List<StainingPrototype> allStainingsList;

		/**
		 * StainingPrototype for creating and editing
		 */
		private StainingPrototype editStaining;

		private boolean newStaining;

		public StainingTab() {
			setName("dialog.settings.stainings");
			setViewID("staining");
			setPage(StainingPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				setAllStainingsList(utilDAO.getAllStainingPrototypes());
				break;
			}

		}

		/**
		 * Prepares a new Staining for editing
		 */
		public void prepareNewStaining() {
			prepareEditStaining(new StainingPrototype());
		}

		/**
		 * Shows the edit staining form
		 *
		 * @param stainingPrototype
		 */
		public void prepareEditStaining(StainingPrototype stainingPrototype) {
			setEditStaining(stainingPrototype);
			setNewStaining(stainingPrototype.getId() == 0 ? true : false);
			setPage(StainingPage.EDIT);
		}

		/**
		 * Saves or creats a new stainingprototype
		 *
		 * @param newStainingPrototype
		 * @param origStainingPrototype
		 */
		public void saveStainig() {
			try {
				if (getEditStaining().getId() == 0) {
					logger.debug("Creating new staining " + getEditStaining().getName());
					// case new, save
					getAllStainingsList().add(getEditStaining());

					genericDAO.saveDataRollbackSave(getEditStaining(),
							resourceBundle.get("log.settings.staining.new", getEditStaining().getName()));

					ListOrder.reOrderList(getAllStainingsList());

					genericDAO.saveListRollbackSave(getAllStainingsList(),
							resourceBundle.get("log.settings.staining.list.reoder"));
				} else {
					genericDAO.saveDataRollbackSave(getEditStaining(),
							resourceBundle.get("log.settings.material.update", getEditStaining().getName()));
				}

			} catch (

			CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Is fired if the list is reordered by the user via drag and drop
		 *
		 * @param event
		 */
		public void onReorderList(ReorderEvent event) {
			try {
				logger.debug("List order changed, moved staining from " + event.getFromIndex() + " to "
						+ event.getToIndex());
				ListOrder.reOrderList(getAllStainingsList());
				genericDAO.saveListRollbackSave(getAllStainingsList(),
						resourceBundle.get("log.settings.staining.list.reoder"));

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * discards changes
		 */
		public void discardStainig() {
			if (getEditStaining().getId() != 0)
				genericDAO.reset(getEditStaining());
			setPage(StainingPage.LIST);
			setEditStaining(null);

			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "staining/stainingsEdit.xhtml";
			default:
				return "staining/stainingsList.xhtml";
			}
		}

	}

	public enum StaticListPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class StaticListTab extends AbstractSettingsTab {

		private StaticListPage page;

		/**
		 * Current static list to edit
		 */
		private ListItem.StaticList selectedStaticList = ListItem.StaticList.WARDS;

		/**
		 * Content of the current static list
		 */
		private List<ListItem> staticListContent;

		/**
		 * Is used for creating and editing static lists items
		 */
		private ListItem editListItem;

		/**
		 * If true archived object will be shown.
		 */
		private boolean showArchivedListItems;

		private boolean newListItem;

		public StaticListTab() {
			setName("dialog.settings.staticLists");
			setViewID("staticLists");
			setPage(StaticListPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				loadStaticList();
				break;
			}
		}

		public void loadStaticList() {
			setStaticListContent(utilDAO.getAllStaticListItems(getSelectedStaticList(), isShowArchivedListItems()));
		}

		public void prepareNewListItem() {
			prepareEditListItem(new ListItem());
		}

		public void prepareEditListItem(ListItem listItem) {
			setEditListItem(listItem);
			setPage(StaticListPage.EDIT);
			setNewListItem(listItem.getId() == 0 ? true : false);
		}

		public void saveListItem() {
			try {

				if (getEditListItem().getId() == 0) {

					getEditListItem().setListType(getSelectedStaticList());

					logger.debug("Creating new ListItem " + getEditListItem().getValue() + " for "
							+ getSelectedStaticList().toString());
					// case new, save
					getStaticListContent().add(getEditListItem());

					genericDAO.saveDataRollbackSave(getEditListItem(), resourceBundle.get("log.settings.staticList.new",
							getEditListItem().getValue(), getSelectedStaticList().toString()));

					ListOrder.reOrderList(getStaticListContent());

					genericDAO.saveListRollbackSave(getStaticListContent(), resourceBundle
							.get("log.settings.staticList.list.reoder", getSelectedStaticList().toString()));
				} else {
					logger.debug("Updating ListItem " + getEditListItem().getValue());
					// case edit: update an save

					genericDAO.saveDataRollbackSave(getEditListItem(),
							resourceBundle.get("log.settings.staticList.update", getEditListItem().getValue(),
									getSelectedStaticList().toString()));
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardListItem() {
			if (getEditListItem().getId() != 0)
				genericDAO.reset(getEditListItem());

			setEditListItem(null);
			setPage(StaticListPage.LIST);

			updateData();
		}

		public void archiveListItem(ListItem item, boolean archive) {
			try {
				item.setArchived(archive);
				if (archive) {
					genericDAO.saveDataRollbackSave(item, resourceBundle.get("log.settings.staticList.archive",
							item.getValue(), getSelectedStaticList().toString()));
				} else {
					genericDAO.saveDataRollbackSave(item, resourceBundle.get("log.settings.staticList.dearchive",
							item.getValue(), getSelectedStaticList().toString()));
				}

				// removing item from current list
				getStaticListContent().remove(item);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void onReorderList(ReorderEvent event) {
			try {
				ListOrder.reOrderList(getStaticListContent());

				genericDAO.saveListRollbackSave(getStaticListContent(),
						resourceBundle.get("log.settings.staticList.list.reoder", getSelectedStaticList().toString()));
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "staticLists/staticListsEdit.xhtml";
			default:
				return "staticLists/staticLists.xhtml";
			}
		}

	}

	// @Getter
	// @Setter
	// public class PhysicianSettingsTab extends AbstractSettingsTab {
	//
	// /**
	// * Tabindex of the settings tab
	// */
	// private Page page;
	// // private SettingsTab physicianTabIndex = SettingsTab.P_LIST;
	//
	// /**
	// * True if archived physicians should be display
	// */
	// private boolean showArchivedPhysicians = false;
	//
	// /**
	// * Array of roles for that physicians should be shown.
	// */
	// private ContactRole[] showPhysicianRoles;
	//
	// /**
	// * List containing all physicians known in the histo database
	// */
	// private List<Physician> physicianList;
	//
	// /**
	// * Used for creating new or for editing existing physicians
	// */
	// private Physician tmpPhysician;
	//
	// /**
	// * List containing all physicians available from ldap
	// */
	// private List<Physician> ldapPhysicianList;
	//
	// /**
	// * Used for selecting a physician from the ldap list
	// */
	// private Physician tmpLdapPhysician;
	//
	// /**
	// * String is used for searching for internal physicians
	// */
	// private String ldapPhysicianSearchString;
	//
	// public void prepare() {
	// switch (getPage()) {
	// case LIST:
	//
	// if (getShowPhysicianRoles() == null || getShowPhysicianRoles().length ==
	// 0)
	// setShowPhysicianRoles(new ContactRole[] { ContactRole.PRIVATE_PHYSICIAN,
	// ContactRole.SURGEON,
	// ContactRole.OTHER_PHYSICIAN, ContactRole.SIGNATURE });
	//
	// setPhysicianList(physicianDAO.getPhysicians(getShowPhysicianRoles(),
	// isShowArchivedPhysicians()));
	// break;
	//
	// default:
	// break;
	// }
	// }
	//
	// /**
	// * Shows the add external or ldap screen per default the ldap select
	// * screnn is used.
	// */
	// public void prepareNewPhysician() {
	// setTmpPhysician(new Physician());
	// getTmpPhysician().setPerson(new Person());
	// setPage(Page.ADD_LDAP);
	//
	// setLdapPhysicianSearchString("");
	// setLdapPhysicianList(new ArrayList<Physician>());
	// }
	//
	// /**
	// * Shows the gui for editing an existing physician
	// *
	// * @param physician
	// */
	// public void prepareEditPhysician(Physician physician) {
	// setTmpPhysician(physician);
	// setPage(Page.EDIT);
	// }
	//
	// /**
	// * Opens the passed physician in the settingsDialog in order to edit the
	// * phone number, email or faxnumber.
	// *
	// * @param associatedContact
	// */
	// public void prepareEditPhysicianFromExtern(Person person) {
	// Physician result = physicianDAO.getPhysicianByPerson(person);
	// if (result != null) {
	// setTmpPhysician(result);
	// setPhysicianTabIndex(SettingsTab.P_EDIT_EXTERN);
	// setActiveSettingsIndex(SettingsTab.PHYSICIAN.getTabNumber());
	// prepareSettingsDialog();
	// }
	// }
	//
	// /**
	// * Generates an ldap search filter (?(xxx)....) and offers the result
	// * list. The result list is a physician list with minimal details.
	// * Before adding an clinic physician a ldap fetch for more details has
	// * to be done
	// *
	// * @param name
	// */
	// public void searchForPhysician(String name) {
	// // removing multiple spaces an commas and replacing them with one
	// // space,
	// // splitting the whole thing into an array
	// String[] arr = name.replaceAll("[ ,]+", " ").split(" ");
	// StringBuffer request = new StringBuffer("(&");
	// for (int i = 0; i < arr.length; i++) {
	// request.append("(cn=*" + arr[i] + "*)");
	// }
	// request.append(")");
	//
	// try {
	// logger.debug("Search for " + request.toString());
	//
	// LdapHandler connection = settingsHandler.getLdapHandler();
	//
	// // searching for physicians
	// connection.openConnection();
	// setLdapPhysicianList(connection.getListOfPhysicians(request.toString()));
	// connection.closeConnection();
	//
	// setTmpLdapPhysician(null);
	//
	// } catch (NamingException | IOException e) {
	// setLdapPhysicianList(null);
	// // TODO to many results
	// }
	// }
	//
	// /**
	// * Saves an edited physician to the database
	// *
	// * @param physician
	// */
	// public void saveEditPhysician(Physician physician) {
	// try {
	// if (physician.hasNoAssociateRole())
	// physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);
	//
	// physicianDAO.save(physician, "log.settings.physician.physician.edit",
	// new Object[] { physician.getPerson().getFullName() });
	//
	// discardTmpPhysician();
	// } catch (CustomDatabaseInconsistentVersionException e) {
	// settingsDialogHandler.onDatabaseVersionConflict();
	// }
	// }
	//
	// /**
	// * Saves a physician to the database, if no role was selected
	// * ContactRole.Other will be set per default.
	// *
	// * @param physician
	// */
	// public void saveNewPrivatePhysician(Physician physician) {
	// try {
	// // always set role to miscellaneous if no other role was
	// // selected
	// if (physician.hasNoAssociateRole())
	// physician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);
	//
	// genericDAO.saveDataRollbackSave(physician, resourceBundle
	// .get("log.settings.physician.privatePhysician.save",
	// physician.getPerson().getFullName()));
	//
	// discardTmpPhysician();
	// } catch (CustomDatabaseInconsistentVersionException e) {
	// settingsDialogHandler.onDatabaseVersionConflict();
	// }
	// }
	//
	// /**
	// *
	// * @param ldapPhysician
	// * @param editPhysician
	// */
	// public void savePhysicianFromLdap(Physician ldapPhysician,
	// HashSet<ContactRole> roles) {
	// try {
	// if (ldapPhysician == null) {
	// discardTmpPhysician();
	// return;
	// }
	//
	// // removing id from the list
	// ldapPhysician.setId(0);
	//
	// if (roles == null || roles.size() == 0)
	// ldapPhysician.addAssociateRole(ContactRole.OTHER_PHYSICIAN);
	// else
	// ldapPhysician.setAssociatedRoles(roles);
	//
	// // tje internal physician from ldap it might have been added
	// // before
	// // (if
	// // the the physician is a user of this programm),
	// // search fur unique uid
	// Physician physicianFromDatabase =
	// physicianDAO.loadPhysicianByUID(ldapPhysician.getUid());
	//
	// // undating the foud physician
	// if (physicianFromDatabase != null) {
	// physicianFromDatabase.copyIntoObject(ldapPhysician);
	//
	// physicianFromDatabase.setArchived(false);
	//
	// // overwriting roles
	// physicianFromDatabase.setAssociatedRoles(roles);
	//
	// genericDAO.saveDataRollbackSave(physicianFromDatabase, resourceBundle
	// .get("log.settings.physician.ldap.update",
	// ldapPhysician.getPerson().getFullName()));
	//
	// setTmpPhysician(physicianFromDatabase);
	// discardTmpPhysician();
	// return;
	// }
	//
	// genericDAO.saveDataRollbackSave(ldapPhysician,
	// resourceBundle.get("log.settings.physician.ldap.save",
	// ldapPhysician.getPerson().getFullName()));
	//
	// discardTmpPhysician();
	// } catch (CustomDatabaseInconsistentVersionException e) {
	// settingsDialogHandler.onDatabaseVersionConflict();
	// }
	// }
	//
	// /**
	// * Archvies or dearchvies physicians depending on the given parameters.
	// *
	// * @param physician
	// * @param archive
	// */
	// public void archivePhysician(Physician physician, boolean archive) {
	// try {
	// physician.setArchived(archive);
	// genericDAO.saveDataRollbackSave(physician,
	// resourceBundle.get(
	// archive ? "log.settings.physician.archived" :
	// "log.settings.physician.archived.undo",
	// physician.getPerson().getFullName()));
	// preparePhysicianList();
	// } catch (CustomDatabaseInconsistentVersionException e) {
	// settingsDialogHandler.onDatabaseVersionConflict();
	// }
	// }
	//
	// /**
	// * Clears the temporary variables and the the physician list to display
	// */
	// public void discardTmpPhysician() {
	// // if a physician is in database and changes should be discarded, so
	// // refresh from database
	// if ((getPhysicianTabIndex() == SettingsTab.P_EDIT ||
	// getPhysicianTabIndex() == SettingsTab.P_EDIT_EXTERN)
	// && getTmpPhysician().getId() != 0)
	// genericDAO.reset(getTmpPhysician());
	//
	// setTmpPhysician(null);
	// setTmpLdapPhysician(null);
	//
	// if (getPhysicianTabIndex() != SettingsTab.P_EDIT_EXTERN) {
	// // update physician list
	// preparePhysicianList();
	// } else {
	// // if the edit was called externally close the dialog
	// hideDialog();
	// }
	//
	// setPhysicianTabIndex(SettingsTab.P_LIST);
	// }
	//
	// public void removeOrganizationFromPerson(Person person, Organization
	// organization) {
	// try {
	// logger.debug("Removing Person from Organization");
	// organizationDAO.removeOrganization(person, organization);
	// } catch (CustomDatabaseInconsistentVersionException e) {
	// onDatabaseVersionConflict();
	// }
	// }
	//
	// public enum Page {
	// LIST, EDIT, EDIT_EXTERN, ADD_EXTERN, ADD_LDAP;
	// }
	// }
	//

	// }

	public enum FavouriteListPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class FavouriteListTab extends AbstractSettingsTab {

		private FavouriteListPage page;

		/**
		 * Array containing all favourite listis
		 */
		private List<FavouriteList> favouriteLists;

		/**
		 * Temporaray faourite list
		 */
		private FavouriteList tmpFavouriteList;

		/**
		 * True if new favouriteList should be created
		 */
		private boolean newFavouriteList;

		public FavouriteListTab() {
			setName("dialog.settings.favouriteList");
			setViewID("favouriteLists");
			setPage(FavouriteListPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				if (getTmpFavouriteList().getId() != 0)
					setTmpFavouriteList(favouriteListDAO.getFavouriteList(getTmpFavouriteList().getId(), true));
				break;
			default:
				setFavouriteLists(favouriteListDAO.getAllFavouriteLists());
				break;
			}

		}

		public void prepareNewFavouriteList() {
			FavouriteList newList = new FavouriteList();
			newList.setItems(new ArrayList<FavouriteListItem>());
			newList.setDefaultList(false);
			newList.setEditAble(true);
			newList.setGlobal(true);
			prepareEditFavouriteList(newList);
		}

		public void prepareEditFavouriteList(FavouriteList favouriteList) {
			setTmpFavouriteList(favouriteList);
			setPage(FavouriteListPage.EDIT);
			setNewFavouriteList(favouriteList.getId() == 0 ? true : false);
			updateData();
		}

		public void saveFavouriteList() {
			try {
				// saving new list
				if (getTmpFavouriteList().getId() == 0) {
					genericDAO.saveDataRollbackSave(getTmpFavouriteList(), "log.settings.favouriteList.new",
							new Object[] { getTmpFavouriteList().toString() });
				} else {
					// updating old list
					genericDAO.saveDataRollbackSave(getTmpFavouriteList(), "log.settings.favouriteList.edit",
							new Object[] { getTmpFavouriteList().toString() });
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardFavouriteList() {
			setTmpFavouriteList(null);
			setPage(FavouriteListPage.LIST);
			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "favouriteLists/favouriteListEdit.xhtml";
			default:
				return "favouriteLists/favouriteList.xhtml";
			}
		}

	}

	public enum OrganizationTabPage {
		LIST, EDIT;
	}

	@Getter
	@Setter
	public class OrganizationTab extends AbstractSettingsTab {

		private OrganizationTabPage page;

		private List<Organization> organizations;

		private Organization selectedOrganization;

		private boolean newOrganization;

		public OrganizationTab() {
			setName("dialog.settings.organization");
			setViewID("organizations");
			setPage(OrganizationTabPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				break;
			default:
				setOrganizations(organizationDAO.getOrganizations());
				break;
			}

		}

		public void prepareNewOrganization() {
			Organization organization = new Organization(new Contact());
			prepareEditOrganization(organization);
		}

		public void prepareEditOrganization(Organization organization) {
			setSelectedOrganization(organization);
			setPage(OrganizationTabPage.EDIT);
			setNewOrganization(organization.getId() == 0 ? true : false);
			updateData();
		}

		public void saveOrganization() {
			try {
				organizationDAO.save(getSelectedOrganization(),
						getSelectedOrganization().getId() == 0 ? "log.organization.save" : "log.organization.created",
						new Object[] { getSelectedOrganization().getName() });
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardOrganization() {
			setSelectedOrganization(null);
			setPage(OrganizationTabPage.LIST);

			updateData();
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "organization/organizationEdit.xhtml";
			default:
				return "organization/organizationLists.xhtml";
			}
		}

	}

	@Getter
	@Setter
	public class LogTab extends AbstractSettingsTab {

		private int logsPerPull;

		private int selectedLogPage;

		private List<Log> logs;
		
		private int maxLogPages;
		
		public LogTab() {
			setName("dialog.settings.log");
			setViewID("logs");

			setLogsPerPull(50);
			setSelectedLogPage(1);

		}

		@Override
		public void updateData() {
			int maxPages = logDAO.countTotalLogs();
			int pagesCount = (int) Math.ceil((double) maxPages / logsPerPull);

			setMaxLogPages(pagesCount);
			
			setLogs(logDAO.getLogs(getLogsPerPull(), getSelectedLogPage()-1));
		}

		@Override
		public String getCenterView() {
			// TODO Auto-generated method stub
			return "log/log.xhtml";
		}
	}
}
