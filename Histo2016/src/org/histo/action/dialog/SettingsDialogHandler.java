package org.histo.action.dialog;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.UserHandlerAction;
import org.histo.config.ResourceBundle;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
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
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Organization;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.interfaces.ListOrder;
import org.histo.model.log.Log;
import org.histo.model.user.HistoGroup;
import org.histo.model.user.HistoUser;
import org.histo.ui.ListChooser;
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
	private ResourceBundle resourceBundle;

	@Autowired
	private UserHandlerAction userHandlerAction;

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
			new MaterialTab(), new StainingTab(), new StaticListTab(), new FavouriteListTab(),
			new PhysicianSettingsTab(), new OrganizationTab(), new LogTab() };

	/********************************************************
	 * General
	 ********************************************************/

	public void initAndPrepareBean() {
		if (initBean(getActiveSettingsIndex()))
			prepareDialog();
	}

	public void initAndPrepareBean(String tabName) {
		if (initBean(tabName))
			prepareDialog();
	}

	public boolean initBean(String tabName) {
		int tabNumber = 0;

		for (int i = 0; i < tabs.length; i++)
			if (tabs[i].getTabName().equals(tabName)) {
				tabNumber = i;
				break;
			}

		return initBean(tabNumber);
	}

	public boolean initBean(int activeTab) {

		if (activeTab >= 0 && activeTab < getTabs().length) {
			setActiveSettingsIndex(activeTab);

			onSettingsTabChange(null);
		} else {
			return false;
		}

		super.initBean(task, Dialog.SETTINGS);

		return true;
	}

	public void onSettingsTabChange(TabChangeEvent event) {
		if (getActiveSettingsIndex() >= 0 && getActiveSettingsIndex() < getTabs().length) {
			logger.debug("Updating Tab with index " + getActiveSettingsIndex());
			getTabs()[getActiveSettingsIndex()].updateData();
		}
	}

	public AbstractSettingsTab getTab(String tabName) {
		for (AbstractSettingsTab abstractSettingsTab : tabs) {
			if (abstractSettingsTab.getTabName().equals(tabName))
				return abstractSettingsTab;
		}

		return null;
	}

	/********************************************************
	 * General
	 ********************************************************/

	@Getter
	@Setter
	public abstract class AbstractSettingsTab {

		public abstract String getCenterView();

		public abstract void updateData();

		protected String name;

		protected String viewID;

		protected String tabName;
	}

	@Getter
	@Setter
	public class HistoUserTab extends AbstractSettingsTab {

		private List<HistoUser> users;

		private List<HistoGroup> groups;
		
		private DefaultTransformer<HistoGroup> groupTransformer;
		
		public HistoUserTab() {
			setTabName("HistoUserTab");
			setName("dialog.settings.user");
			setViewID("histoUser");
		}

		public void updateData() {
			setUsers(userDAO.getUsers(false));
			setGroups(userDAO.getGroups(false));
			setGroupTransformer(new DefaultTransformer<HistoGroup>(getGroups()));
		}

		public void onChangeUserGroup(HistoUser histoUser) {
			try {
				userHandlerAction.groupOfUserHasChanged(histoUser);
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			return "globalSettings/userList.xhtml";
		}

		public void addHistoUser(Physician physician) {
			try {
				if (physician != null) {
					userDAO.addUser(physician);
					updateData();
				}
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
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

		private ContactRole[] allRoles;

		public DiagnosisTab() {
			setTabName("DiagnosisTab");
			setName("dialog.settings.diagnosis");
			setViewID("diagnoses");
			setPage(DiagnosisPage.LIST);

			setAllRoles(new ContactRole[] { ContactRole.FAMILY_PHYSICIAN, ContactRole.PATIENT, ContactRole.SURGEON,
					ContactRole.PRIVATE_PHYSICIAN, ContactRole.RELATIVES });
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

					genericDAO.save(getSelectedDiagnosisPreset(), resourceBundle.get("log.settings.diagnosis.new",
							getSelectedDiagnosisPreset().getCategory()));

					ListOrder.reOrderList(getDiagnosisPresets());

					genericDAO.saveCollection(getDiagnosisPresets(), "log.settings.diagnosis.list.reoder");

				} else {

					System.out.println(getSelectedDiagnosisPreset().getIndexInList());
					// case edit: update an save
					logger.debug("Updating diagnosis " + getSelectedDiagnosisPreset().getCategory());

					genericDAO.save(getSelectedDiagnosisPreset(), resourceBundle.get("log.settings.diagnosis.update",
							getSelectedDiagnosisPreset().getCategory()));
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
				genericDAO.refresh(getSelectedDiagnosisPreset());
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

				genericDAO.saveCollection(getDiagnosisPresets(), "log.settings.diagnosis.list.reoder");

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "globalSettings/diagnosisEdit.xhtml";
			case EDIT_TEXT_TEMPLATE:
				return "globalSettings/diagnosisEditTemplate.xhtml";
			default:
				return "globalSettings/diagnosisList.xhtml";
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
			setTabName("MaterialTab");
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

					genericDAO.save(getEditMaterial(),
							resourceBundle.get("log.settings.material.new", getEditMaterial().getName()));

					ListOrder.reOrderList(getAllMaterialList());

					genericDAO.saveCollection(getAllMaterialList(), "log.settings.material.list.reoder");

				} else {
					logger.debug("Updating Material " + getEditMaterial().getName());
					// case edit: update an save
					genericDAO.save(getEditMaterial(),
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
				genericDAO.refresh(getEditMaterial());
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

				genericDAO.saveCollection(getAllMaterialList(), "log.settings.staining.list.reoder");

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "globalSettings/materialEdit.xhtml";
			case ADD_STAINING:
				return "globalSettings/materialAddStaining.xhtml";
			default:
				return "globalSettings/materialList.xhtml";
			}
		}
	}

	@Getter
	@Setter
	public class StainingTab extends AbstractSettingsTab {

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
			setTabName("StainingTab");
			setName("dialog.settings.stainings");
			setViewID("staining");
		}

		@Override
		public void updateData() {
			setAllStainingsList(utilDAO.getAllStainingPrototypes());
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
				genericDAO.saveCollection(getAllStainingsList(), "log.settings.staining.list.reoder");

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			return "globalSettings/stainingsList.xhtml";
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
			setTabName("StaticListTab");
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

					genericDAO.save(getEditListItem(), resourceBundle.get("log.settings.staticList.new",
							getEditListItem().getValue(), getSelectedStaticList().toString()));

					ListOrder.reOrderList(getStaticListContent());

					genericDAO.saveCollection(getStaticListContent(), "log.settings.staticList.list.reoder",
							new Object[] { getSelectedStaticList().toString() });
				} else {
					logger.debug("Updating ListItem " + getEditListItem().getValue());
					// case edit: update an save

					genericDAO.save(getEditListItem(), resourceBundle.get("log.settings.staticList.update",
							getEditListItem().getValue(), getSelectedStaticList().toString()));
				}

			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		public void discardListItem() {
			if (getEditListItem().getId() != 0)
				genericDAO.refresh(getEditListItem());

			setEditListItem(null);
			setPage(StaticListPage.LIST);

			updateData();
		}

		public void archiveListItem(ListItem item, boolean archive) {
			try {
				item.setArchived(archive);
				if (archive) {
					genericDAO.save(item, resourceBundle.get("log.settings.staticList.archive", item.getValue(),
							getSelectedStaticList().toString()));
				} else {
					genericDAO.save(item, resourceBundle.get("log.settings.staticList.dearchive", item.getValue(),
							getSelectedStaticList().toString()));
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

				genericDAO.saveCollection(getStaticListContent(), "log.settings.staticList.list.reoder",
						new Object[] { getSelectedStaticList().toString() });
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			switch (getPage()) {
			case EDIT:
				return "globalSettings/staticListsEdit.xhtml";
			default:
				return "globalSettings/staticLists.xhtml";
			}
		}

	}

	@Getter
	@Setter
	public class PhysicianSettingsTab extends AbstractSettingsTab {

		/**
		 * True if archived physicians should be display
		 */
		private boolean showArchivedPhysicians;

		/**
		 * Array of roles for that physicians should be shown.
		 */
		private ContactRole[] showPhysicianRoles;

		/**
		 * List containing all physicians known in the histo database
		 */
		private List<Physician> physicianList;

		private List<ContactRole> allRoles;

		public PhysicianSettingsTab() {
			setTabName("PhysicianSettingsTab");
			setName("dialog.settings.persons");
			setViewID("persons");
			setShowArchivedPhysicians(false);
			setAllRoles(Arrays.asList(ContactRole.values()));

			setShowPhysicianRoles(new ContactRole[] { ContactRole.PRIVATE_PHYSICIAN, ContactRole.SURGEON,
					ContactRole.OTHER_PHYSICIAN, ContactRole.SIGNATURE });
		}

		@Override
		public void updateData() {
			setPhysicianList(physicianDAO.getPhysicians(getShowPhysicianRoles(), isShowArchivedPhysicians()));
		}

		public void addPhysician(Physician physician) {
			try {
				if (physician != null) {
					physicianDAO.synchronizePhysician(physician);
					updateData();
				}
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		/**
		 * Archvies or dearchvies physicians depending on the given parameters.
		 *
		 * @param physician
		 * @param archive
		 */
		public void archivePhysician(Physician physician, boolean archive) {
			try {
				physician.setArchived(archive);
				genericDAO.save(physician,
						resourceBundle.get(
								archive ? "log.settings.physician.archived" : "log.settings.physician.archived.undo",
								physician.getPerson().getFullName()));

				updateData();
			} catch (CustomDatabaseInconsistentVersionException e) {
				onDatabaseVersionConflict();
			}
		}

		@Override
		public String getCenterView() {
			return "globalSettings/physicianList.xhtml";
		}

	}

	@Getter
	@Setter
	public class FavouriteListTab extends AbstractSettingsTab {

		/**
		 * Array containing all favourite listis
		 */
		private List<FavouriteList> favouriteLists;

		public FavouriteListTab() {
			setTabName("FavouriteListTab");
			setName("dialog.settings.favouriteList");
			setViewID("favouriteLists");
		}

		@Override
		public void updateData() {
			setFavouriteLists(favouriteListDAO.getAllFavouriteLists());

		}

		@Override
		public String getCenterView() {
			return "globalSettings/favouriteList.xhtml";
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
			setTabName("OrganizationTab");
			setName("dialog.settings.organization");
			setViewID("organizations");
			setPage(OrganizationTabPage.LIST);
		}

		@Override
		public void updateData() {
			switch (getPage()) {
			case EDIT:
				setSelectedOrganization(organizationDAO.get(Organization.class, getSelectedOrganization().getId()));
				organizationDAO.initializeOrganization(getSelectedOrganization());
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
				return "globalSettings/organizationEdit.xhtml";
			default:
				return "globalSettings/organizationLists.xhtml";
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
			setTabName("LogTab");
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

			setLogs(logDAO.getLogs(getLogsPerPull(), getSelectedLogPage() - 1));
		}

		@Override
		public String getCenterView() {
			// TODO Auto-generated method stub
			return "globalSettings/log.xhtml";
		}
	}

	public class AdminTab extends AbstractSettingsTab {

		public AdminTab() {
			setTabName("AdminTab");
			setName("dialog.settings.admin");
			setViewID("admin");
		}

		@Override
		public String getCenterView() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void updateData() {
			// TODO Auto-generated method stub

		}

	}
}
