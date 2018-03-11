package org.histo.action.dialog.worklist;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.UserHandlerAction;
import org.histo.action.dialog.AbstractTabDialog;
import org.histo.action.dialog.export.ExportTasksDialog;
import org.histo.action.dialog.slide.CreateSlidesDialog.SlideSelectResult;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.ContactRole;
import org.histo.config.enums.Dialog;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.PatientDao;
import org.histo.dao.PhysicianDAO;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.DiagnosisPreset;
import org.histo.model.ListItem;
import org.histo.model.MaterialPreset;
import org.histo.model.Physician;
import org.histo.model.StainingPrototype;
import org.histo.model.favouriteList.FavouriteList;
import org.histo.model.patient.Patient;
import org.histo.model.patient.Task;
import org.histo.ui.FavouriteListContainer;
import org.histo.ui.transformer.DefaultTransformer;
import org.histo.worklist.Worklist;
import org.histo.worklist.search.WorklistFavouriteSearch;
import org.histo.worklist.search.WorklistSearchExtended;
import org.histo.worklist.search.WorklistSimpleSearch;
import org.histo.worklist.search.WorklistSimpleSearch.SimpleSearchOption;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.annotation.Lazy;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class WorklistSearchDialog extends AbstractTabDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PatientDao patientDao;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private PhysicianDAO physicianDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ExportTasksDialog exportTasksDialog;

	@Autowired
	@Lazy
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	private SimpleSearchTab simpleSearchTab;
	private FavouriteSearchTab favouriteSearchTab;
	private ExtendedSearchTab extendedSearchTab;

	public WorklistSearchDialog() {
		setSimpleSearchTab(new SimpleSearchTab());
		setFavouriteSearchTab(new FavouriteSearchTab());
		setExtendedSearchTab(new ExtendedSearchTab());

		tabs = new AbstractTab[] { simpleSearchTab, favouriteSearchTab, extendedSearchTab };
	}

	public void initAndPrepareBean() {
		initBean();
		prepareDialog();
	}

	public boolean initBean() {
		return super.initBean(Dialog.WORKLIST_SEARCH);
	}

	@Getter
	@Setter
	public class SimpleSearchTab extends AbstractTab {

		private WorklistSimpleSearch worklistSearch;

		public SimpleSearchTab() {
			setTabName("SimpleSearchTab");
			setName("dialog.worklistsearch.simple");
			setViewID("simpleSearch");
			setCenterInclude("include/simpleSearch.xhtml");
		}

		public boolean initTab() {
			setWorklistSearch(new WorklistSimpleSearch());
			return true;
		}

		public void onChangeWorklistSelection() {
			worklistSearch.setSearchIndex(SimpleSearchOption.CUSTOM_LIST);
		}

		public void selectAsWorklist() {
			Worklist worklist = new Worklist("Default", worklistSearch,
					userHandlerAction.getCurrentUser().getSettings().isWorklistHideNoneActiveTasks(),
					userHandlerAction.getCurrentUser().getSettings().getWorklistSortOrder(),
					userHandlerAction.getCurrentUser().getSettings().isWorklistAutoUpdate(), false,
					userHandlerAction.getCurrentUser().getSettings().isWorklistSortOrderAsc());

			worklistViewHandlerAction.addWorklist(worklist, true, true);
		}
	}

	@Getter
	@Setter
	public class FavouriteSearchTab extends AbstractTab {

		private WorklistFavouriteSearch worklistSearch;

		private List<FavouriteListContainer> containers;

		private FavouriteListContainer selectedContainer;

		public FavouriteSearchTab() {
			setTabName("FavouriteSearchTab");
			setName("dialog.worklistsearch.favouriteList");
			setViewID("favouriteListSearch");
			setCenterInclude("include/favouriteSearch.xhtml");
		}

		public boolean initTab() {
			setWorklistSearch(new WorklistFavouriteSearch());
			setSelectedContainer(null);
			return true;
		}

		@Override
		public void updateData() {

			List<FavouriteList> list = favouriteListDAO.getFavouriteListsForUser(userHandlerAction.getCurrentUser(),
					false, true, true, true, true);

			containers = list.stream().map(p -> new FavouriteListContainer(p, userHandlerAction.getCurrentUser()))
					.collect(Collectors.toList());

			if (selectedContainer != null) {
				List<Patient> patient = favouriteListDAO
						.getPatientFromFavouriteList(selectedContainer.getFavouriteList().getId(), false);
			}
		}

		public void selectAsWorklist() {

			worklistSearch.setFavouriteList(selectedContainer.getFavouriteList());

			Worklist worklist = new Worklist("Default", worklistSearch, false,
					userHandlerAction.getCurrentUser().getSettings().getWorklistSortOrder(),
					userHandlerAction.getCurrentUser().getSettings().isWorklistAutoUpdate(), true,
					userHandlerAction.getCurrentUser().getSettings().isWorklistSortOrderAsc());

			worklistViewHandlerAction.addWorklist(worklist, true, true);
		}
	}

	@Getter
	@Setter
	public class ExtendedSearchTab extends AbstractTab {

		private WorklistSearchExtended worklistSearch;

		/**
		 * List of available materials
		 */
		private List<MaterialPreset> materialList;

		/**
		 * Physician list
		 */
		private Physician[] allPhysicians;

		/**
		 * Transformer for physicians
		 */
		private DefaultTransformer<Physician> allPhysicianTransformer;

		/**
		 * Contains all available case histories
		 */
		private List<ListItem> caseHistoryList;

		/**
		 * List of all diagnosis presets
		 */
		private List<DiagnosisPreset> diagnosisPresets;

		/**
		 * Contains all available wards
		 */
		private List<ListItem> wardList;

		public ExtendedSearchTab() {
			setTabName("ExtendedSearchTab");
			setName("dialog.worklistsearch.scifi");
			setViewID("extendedSearch");
			setCenterInclude("include/extendedSearch.xhtml");
		}

		public boolean initTab() {
			setWorklistSearch(new WorklistSearchExtended());

			// setting material list
			setMaterialList(utilDAO.getAllMaterialPresets(true));

			// setting physician list
			List<Physician> allPhysicians = physicianDAO.getPhysicians(ContactRole.values(), false);
			setAllPhysicians(allPhysicians.toArray(new Physician[allPhysicians.size()]));
			setAllPhysicianTransformer(new DefaultTransformer<>(getAllPhysicians()));

			// case history
			setCaseHistoryList(utilDAO.getAllStaticListItems(ListItem.StaticList.CASE_HISTORY));

			// Diagnosis presets
			setDiagnosisPresets(utilDAO.getAllDiagnosisPrototypes());

			// wardlist
			setWardList(utilDAO.getAllStaticListItems(ListItem.StaticList.WARDS));

			return true;
		}

		public void selectAsWorklist() {
			Worklist worklist = new Worklist("Default", worklistSearch, true,
					userHandlerAction.getCurrentUser().getSettings().getWorklistSortOrder(),
					userHandlerAction.getCurrentUser().getSettings().isWorklistAutoUpdate(), true,
					userHandlerAction.getCurrentUser().getSettings().isWorklistSortOrderAsc());

			worklistViewHandlerAction.addWorklist(worklist, true, true);
		}

		public void exportWorklist() {
			List<Task> tasks = taskDAO.getTaskByCriteria(getWorklistSearch(), true);
			exportTasksDialog.initAndPrepareBean(tasks);
		}

		public void onSelectStainingDialogReturn(SelectEvent event) {
			logger.debug("On select staining dialog return " + event.getObject());

			if (event.getObject() != null && event.getObject() instanceof SlideSelectResult) {

				if (worklistSearch.getStainings() == null)
					worklistSearch.setStainings(new ArrayList<StainingPrototype>());

				worklistSearch.getStainings().addAll(((SlideSelectResult) event.getObject()).getPrototpyes());
			}
		}
	}

	public Worklist extendedSearch() {

		logger.debug("Calling extended search");

		return null;
	}
}
