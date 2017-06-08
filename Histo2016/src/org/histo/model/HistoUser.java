package org.histo.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.config.enums.WorklistView;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.springframework.security.core.userdetails.UserDetails;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "user_sequencegenerator", sequenceName = "user_sequence")
public class HistoUser implements UserDetails, Serializable, LogAble, HasID {

	private static final long serialVersionUID = 8292898827966568346L;

	private long id;

	private long version;

	private String username;

	private long lastLogin;

	private Role role;

	private boolean accountNonExpired = true;
	private boolean accountNonLocked = true;
	private boolean credentialsNonExpired = true;
	private boolean enabled = true;

	private Physician physician;

	/**
	 * True if the printer should be autoselected
	 */
	private boolean autoSelectedPreferedPrinter;
	/**
	 * Name of the preferred cups printer
	 */
	private String preferedPrinter;

	/**
	 * True if the label printer should be autoselected
	 */
	private boolean autoSelectedPreferedLabelPrinter;
	/**
	 * The uuid of the preferred labelprinter
	 */
	private String preferedLabelPritner;

	/**
	 * Page which should be shown as default page
	 */
	private View defaultView;

	/**
	 * TODO implement, task oder patient orientated worklist
	 */
	private WorklistView defaultWorklistView;

	/**
	 * Default worklist to load, staining, diagnosis, notification, none
	 */
	private WorklistSearchOption defaultWorklistToLoad;

	/**
	 * Default sortorder of worklist
	 */
	private WorklistSortOrder defaultWorklistSortOrder;
	
	/**
	 * True the sort order is ascending, false the sortorder is descending
	 */
	private boolean defaultWorklistSortAsc;
	
	/**
	 * If true none active tasks in worklist will be hidden per default
	 */
	private boolean defaultHideNonActiveTasksInWorklist;
	
	/**
	 * Constructor for Hibernate
	 */
	public HistoUser() {
	}

	/**
	 * Consturctor for creating new Histouser
	 * 
	 * @param name
	 */
	public HistoUser(String name) {
		this(name, Role.GUEST);
	}

	public HistoUser(String name, Role role) {
		setUsername(name);
		setRole(role);
		setPhysician(new Physician());
		getPhysician().setPerson(new Person());

		// set role clinicalDoctor or clical personnel
		getPhysician().setClinicEmployee(true);
	}

	/**
	 * Updates the user settings with predefined settings on role change
	 * @param predefinedRoleSettings
	 */
	public void updateUserSettings(PredefinedRoleSettings predefinedRoleSettings) {
		setDefaultView(predefinedRoleSettings.getDefaultView());
		setAutoSelectedPreferedLabelPrinter(predefinedRoleSettings.isAutoSelectedPreferedLabelPrinter());
		setAutoSelectedPreferedPrinter(predefinedRoleSettings.isAutoSelectedPreferedPrinter());
		setDefaultWorklistToLoad(predefinedRoleSettings.getDefaultWorklistToLoad());
		setDefaultWorklistView(predefinedRoleSettings.getDefaultWorklistView());
		setDefaultWorklistSortOrder(predefinedRoleSettings.getWorklistSortOrder());
		setDefaultWorklistSortAsc(predefinedRoleSettings.isWorklistSortOrderAscending());
		setDefaultHideNonActiveTasksInWorklist(predefinedRoleSettings.isHideNoneActiveTasks());
	}

	@Id
	@GeneratedValue(generator = "user_sequencegenerator")
	@Column(unique = true, nullable = false)
	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	@Version
	public long getVersion() {
		return version;
	}

	public void setVersion(long version) {
		this.version = version;
	}
	
	@Column
	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	public Physician getPhysician() {
		return physician;
	}

	public void setPhysician(Physician physician) {
		this.physician = physician;
	}

	@Column
	public boolean isEnabled() {
		return enabled;
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Column
	public long getLastLogin() {
		return lastLogin;
	}

	public void setLastLogin(long lastLogin) {
		this.lastLogin = lastLogin;
	}

	@Enumerated(EnumType.ORDINAL)
	public WorklistSortOrder getDefaultWorklistSortOrder() {
		return defaultWorklistSortOrder;
	}

	public void setDefaultWorklistSortOrder(WorklistSortOrder defaultWorklistSortOrder) {
		this.defaultWorklistSortOrder = defaultWorklistSortOrder;
	}

	@Column
	public boolean isDefaultWorklistSortAsc() {
		return defaultWorklistSortAsc;
	}

	public void setDefaultWorklistSortAsc(boolean defaultWorklistSortAsc) {
		this.defaultWorklistSortAsc = defaultWorklistSortAsc;
	}

	@Column
	public boolean isDefaultHideNonActiveTasksInWorklist() {
		return defaultHideNonActiveTasksInWorklist;
	}

	public void setDefaultHideNonActiveTasksInWorklist(boolean defaultHideNonActiveTasksInWorklist) {
		this.defaultHideNonActiveTasksInWorklist = defaultHideNonActiveTasksInWorklist;
	}

	@Enumerated(EnumType.STRING)
	public Role getRole() {
		return role;
	}

	public void setRole(Role role) {
		this.role = role;
	}

	@Enumerated(EnumType.ORDINAL)
	public View getDefaultView() {
		return defaultView;
	}

	public void setDefaultView(View defaultView) {
		this.defaultView = defaultView;
	}

	@Column
	public String getPreferedPrinter() {
		return preferedPrinter;
	}

	public void setPreferedPrinter(String preferedPrinter) {
		this.preferedPrinter = preferedPrinter;
	}

	@Column
	public String getPreferedLabelPritner() {
		return preferedLabelPritner;
	}

	public void setPreferedLabelPritner(String preferedLabelPritner) {
		this.preferedLabelPritner = preferedLabelPritner;
	}

	@Column
	public boolean isAutoSelectedPreferedPrinter() {
		return autoSelectedPreferedPrinter;
	}

	public void setAutoSelectedPreferedPrinter(boolean autoSelectedPreferedPrinter) {
		this.autoSelectedPreferedPrinter = autoSelectedPreferedPrinter;
	}

	@Column
	public boolean isAutoSelectedPreferedLabelPrinter() {
		return autoSelectedPreferedLabelPrinter;
	}

	public void setAutoSelectedPreferedLabelPrinter(boolean autoSelectedPreferedLabelPrinter) {
		this.autoSelectedPreferedLabelPrinter = autoSelectedPreferedLabelPrinter;
	}

	@Enumerated(EnumType.ORDINAL)
	public WorklistView getDefaultWorklistView() {
		return defaultWorklistView;
	}

	public void setDefaultWorklistView(WorklistView defaultWorklistView) {
		this.defaultWorklistView = defaultWorklistView;
	}

	@Enumerated(EnumType.ORDINAL)
	public WorklistSearchOption getDefaultWorklistToLoad() {
		return defaultWorklistToLoad;
	}

	public void setDefaultWorklistToLoad(WorklistSearchOption defaultWorklistToLoad) {
		this.defaultWorklistToLoad = defaultWorklistToLoad;
	}

	@Transient
	public List<Role> getAuthorities() {
		List<Role> result = new ArrayList<Role>();
		result.add(role);
		return result;
	}
	
	/**
	 * Not used, LDAP Auth
	 */
	@Override
	@Transient
	public String getPassword() {
		return null;
	}

	@Transient
	public boolean isAccountNonExpired() {
		return accountNonExpired;
	}

	public void setAccountNonExpired(boolean accountNonExpired) {
		this.accountNonExpired = accountNonExpired;
	}

	@Transient
	public boolean isAccountNonLocked() {
		return accountNonLocked;
	}

	public void setAccountNonLocked(boolean accountNonLocked) {
		this.accountNonLocked = accountNonLocked;
	}

	@Transient
	public boolean isCredentialsNonExpired() {
		return credentialsNonExpired;
	}

	public void setCredentialsNonExpired(boolean credentialsNonExpired) {
		this.credentialsNonExpired = credentialsNonExpired;
	}
}
