package org.histo.action.dialog.slide;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.histo.action.dialog.AbstractDialog;
import org.histo.action.view.GlobalEditViewHandler;
import org.histo.action.view.ReceiptlogViewHandlerAction;
import org.histo.action.view.WorklistViewHandlerAction;
import org.histo.config.enums.Dialog;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.dao.TaskDAO;
import org.histo.dao.UtilDAO;
import org.histo.model.ListItem;
import org.histo.model.StainingPrototype;
import org.histo.model.StainingPrototype.StainingType;
import org.histo.model.patient.Block;
import org.histo.service.SampleService;
import org.histo.ui.task.TaskStatus;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Configurable;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Configurable
@Getter
@Setter
public class CreateSlidesDialog extends AbstractDialog {

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UtilDAO utilDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ReceiptlogViewHandlerAction receiptlogViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private TaskDAO taskDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	/**
	 * Current block for which the slides are created
	 */
	private Block block;

	/**
	 * Commentary for the slides
	 */
	private String commentary;

	/**
	 * True if block is null, so only stainings can be selected
	 */
	private boolean selectMode;

	/**
	 * True if the slides are restainings
	 */
	private boolean restaining;

	/**
	 * The slides will be marked as completed
	 */
	private boolean asCompleted;

	/**
	 * Tab container
	 */
	private List<StainingTypeContainer> container;

	/**
	 * Contains all available case histories
	 */
	private List<ListItem> slideCommentary;

	/**
	 * Initializes the dialog for selecting stainings.
	 */
	public void initAndPrepareBean() {
		if (initBean(null))
			prepareDialog();
	}

	/**
	 * Initializes the bean and shows the dialog for creating slides
	 * 
	 * @param patient
	 */
	public void initAndPrepareBean(Block block) {
		if (initBean(block))
			prepareDialog();
	}

	/**
	 * Initializes all field of the object
	 * 
	 * @param task
	 */
	public boolean initBean(Block block) {
		super.initBean(null, Dialog.SLIDE_CREATE);

		if (block != null) {
			try {
				setBlock(genericDAO.reattach(block));
			} catch (CustomDatabaseInconsistentVersionException e) {
				logger.debug("Version conflict, updating entity");
				task = taskDAO.getTaskAndPatientInitialized(block.getTask().getId());
				worklistViewHandlerAction.onVersionConflictTask(task, false);
				return false;
			}
			
			setCommentary("");

			setAsCompleted(false);

			setSlideCommentary(utilDAO.getAllStaticListItems(ListItem.StaticList.SLIDES));

			setRestaining(block.getTask().isListedInFavouriteList(PredefinedFavouriteList.DiagnosisList,
					PredefinedFavouriteList.ReDiagnosisList) || TaskStatus.checkIfReStainingFlag(block.getParent()));
		}
		
		setSelectMode(block == null);

		setContainer(new ArrayList<StainingTypeContainer>());

		// adding tabs dynamically
		for (StainingType type : StainingType.values()) {
			getContainer()
					.add(new StainingTypeContainer(type, utilDAO.getStainingPrototypes(new StainingType[] { type })));
		}

	

		return true;
	}

	/**
	 * True if at least one staining is selected
	 * 
	 * @return
	 */
	public boolean isStainingSelected() {
		return container.stream()
				.anyMatch(p -> p.getSelectedPrototypes() != null && !p.getSelectedPrototypes().isEmpty());
	}

	/**
	 * Hides the dialog and returns the selection
	 */
	public void hideDialogAndReturnSlides() {
		super.hideDialog(isStainingSelected() ? new SlideSelectResult() : null);
	}

	/**
	 * Class for displaying the according
	 * 
	 * @author andi
	 *
	 */
	@Getter
	@Setter
	public class StainingTypeContainer {
		private StainingType type;
		private List<StainingPrototype> prototpyes;
		private List<StainingPrototype> selectedPrototypes;

		public StainingTypeContainer(StainingType type, List<StainingPrototype> prototypes) {
			this.type = type;
			this.prototpyes = prototypes;
			this.selectedPrototypes = new ArrayList<StainingPrototype>();
		}
	}

	/**
	 * Return result, as a single object for passing via select event
	 * 
	 * @author andi
	 *
	 */
	@Getter
	@Setter
	public class SlideSelectResult {
		private List<StainingPrototype> prototpyes;
		private Block block;
		private String commentary;
		private boolean restaining;
		private boolean asCompleted;

		public SlideSelectResult() {
			this.block = CreateSlidesDialog.this.block;
			this.commentary = CreateSlidesDialog.this.commentary;
			this.restaining = CreateSlidesDialog.this.restaining;
			this.asCompleted = CreateSlidesDialog.this.asCompleted;

			this.prototpyes = new ArrayList<StainingPrototype>();
			// adding all selected prototypes to the result
			CreateSlidesDialog.this.container.stream().map(p -> p.getSelectedPrototypes()).collect(Collectors.toList())
					.forEach(p -> this.prototpyes.addAll(p));

		}
	}
}
