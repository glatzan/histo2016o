package org.histo.action.view;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.log4j.Logger;
import org.histo.action.DialogHandlerAction;
import org.histo.action.MainHandlerAction;
import org.histo.action.UserHandlerAction;
import org.histo.config.enums.DocumentType;
import org.histo.config.enums.PredefinedFavouriteList;
import org.histo.config.enums.StainingListAction;
import org.histo.config.exception.CustomDatabaseInconsistentVersionException;
import org.histo.config.exception.CustomUserNotificationExcepetion;
import org.histo.dao.FavouriteListDAO;
import org.histo.dao.GenericDAO;
import org.histo.model.patient.Sample;
import org.histo.model.patient.Slide;
import org.histo.model.patient.Task;
import org.histo.service.SampleService;
import org.histo.template.DocumentTemplate;
import org.histo.template.documents.TemplateSlideLable;
import org.histo.ui.StainingTableChooser;
import org.histo.ui.task.TaskStatus;
import org.primefaces.event.SelectEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Controller;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Controller
@Scope("session")
@Getter
@Setter
public class TaskHandlerAction {

	private static Logger logger = Logger.getLogger("org.histo");

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private WorklistViewHandlerAction worklistViewHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private SampleService sampleService;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private DialogHandlerAction dialogHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GlobalEditViewHandler globalEditViewHandler;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private FavouriteListDAO favouriteListDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private GenericDAO genericDAO;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private UserHandlerAction userHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private MainHandlerAction mainHandlerAction;

	@Autowired
	@Getter(AccessLevel.NONE)
	@Setter(AccessLevel.NONE)
	private ReceiptlogViewHandlerAction receiptlogiewHandlerAction;

	/**
	 * Creates a block by using the gui
	 * 
	 * @param sample
	 */
	public void createNewBlock(Sample sample) {
		try {
			sampleService.createBlockForSample(sample);
			// generating gui list
			globalEditViewHandler.updateDataOfTask(true, false, true, true);

		} catch (CustomDatabaseInconsistentVersionException e) {
			// catching database version inconsistencies
			worklistViewHandlerAction.onVersionConflictTask();
		}
	}

	/**
	 * Starts the staining phase
	 * 
	 * @param task
	 */
	public void startStainingPhase(Task task) {
		sampleService.startStainingPhase(task);
		// generating gui list
		globalEditViewHandler.updateDataOfTask(true, false, true, true);
	}

	public void testMemo() {
		mainHandlerAction.sendGrowlMessages("test", "test");
	}

}
