package org.histo.model.transitory;

import java.util.List;

import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistView;

/**
 * Standard settings for a specific role
 * @author glatza
 *
 */
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
	 * TODO implement, task oder patient orientated worklist
	 */
	private WorklistView defaultWorklistView;
	
	/**
	 * Default worklist to load, staining, diagnosis, notification, none
	 */
	private WorklistSearchOption defaultWorklistToLoad;

	
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	public boolean isAutoSelectedPreferedPrinter() {
		return autoSelectedPreferedPrinter;
	}

	public void setAutoSelectedPreferedPrinter(boolean autoSelectedPreferedPrinter) {
		this.autoSelectedPreferedPrinter = autoSelectedPreferedPrinter;
	}

	public boolean isAutoSelectedPreferedLabelPrinter() {
		return autoSelectedPreferedLabelPrinter;
	}

	public void setAutoSelectedPreferedLabelPrinter(boolean autoSelectedPreferedLabelPrinter) {
		this.autoSelectedPreferedLabelPrinter = autoSelectedPreferedLabelPrinter;
	}

	public View getDefaultView() {
		return defaultView;
	}

	public void setDefaultView(View defaultView) {
		this.defaultView = defaultView;
	}

	public WorklistView getDefaultWorklistView() {
		return defaultWorklistView;
	}

	public void setDefaultWorklistView(WorklistView defaultWorklistView) {
		this.defaultWorklistView = defaultWorklistView;
	}

	public WorklistSearchOption getDefaultWorklistToLoad() {
		return defaultWorklistToLoad;
	}

	public void setDefaultWorklistToLoad(WorklistSearchOption defaultWorklistToLoad) {
		this.defaultWorklistToLoad = defaultWorklistToLoad;
	}

	public List<View> getAvailableViews() {
		return availableViews;
	}

	public void setAvailableViews(List<View> availableViews) {
		this.availableViews = availableViews;
	}
	
}
