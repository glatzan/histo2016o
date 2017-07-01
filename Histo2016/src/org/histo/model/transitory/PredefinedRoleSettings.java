package org.histo.model.transitory;

import java.util.List;

import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistSortOrder;

import lombok.Getter;
import lombok.Setter;

/**
 * Standard settings for a specific role
 * 
 * @author glatza
 *
 */
@Getter
@Setter
public class PredefinedRoleSettings {

	/**
	 * 
	 */
	private Role role;

	/**
	 * True if the printer should be autoselected
	 */
	private boolean autoSelectedPreferedPrinter;

	/**
	 * True if the label printer should be autoselected
	 */
	private boolean autoSelectedPreferedLabelPrinter;

	/**
	 * Page which should be shown as default page
	 */
	private View defaultView;

	/**
	 * List of available views
	 */
	private List<View> availableViews;

	/**
	 * List of selectable views
	 */
	private List<View> selectableViews;

	/**
	 * Default worklist to load, staining, diagnosis, notification, none
	 */
	private WorklistSearchOption worklistToLoad;

	/**
	 * Default sort order
	 */
	private WorklistSortOrder worklistSortOrder;

	/**
	 * True if sort order should be ascending
	 */
	private boolean worklistSortOrderAscending;

	/**
	 * True if none active tasks should be hidden
	 */
	private boolean hideNoneActiveTasks;
}
