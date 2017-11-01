package org.histo.model.user;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

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
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Role;
import org.histo.config.enums.View;
import org.histo.config.enums.WorklistSearchOption;
import org.histo.config.enums.WorklistSortOrder;
import org.histo.model.Person;
import org.histo.model.Physician;
import org.histo.model.interfaces.ArchivAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.LogAble;
import org.histo.model.patient.Patient;
import org.histo.model.transitory.PredefinedRoleSettings;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "user_sequencegenerator", sequenceName = "user_sequence")
@Getter
@Setter
public class HistoUser implements UserDetails, Serializable, LogAble, HasID, ArchivAble {

	private static final long serialVersionUID = 8292898827966568346L;

	@Id
	@GeneratedValue(generator = "user_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	@Column(columnDefinition = "VARCHAR")
	private String username;

	private long lastLogin;

	@Column(columnDefinition = "boolean default false")
	private boolean archived;

	@Enumerated(EnumType.STRING)
	private Role role;

	// @ManyToOne(fetch = FetchType.EAGER)
	// @Fetch(value = FetchMode.SUBSELECT)
	// private HistoGroup role;

	@Transient
	private boolean accountNonExpired = true;
	@Transient
	private boolean accountNonLocked = true;
	@Transient
	private boolean credentialsNonExpired = true;
	@Transient
	private boolean enabled = true;

	@OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private Physician physician;

	@OneToOne(cascade = CascadeType.ALL)
	@NotAudited
	private HistoSettings settings;

	/**
	 * True if the printer should be autoselected
	 */
	@Column
	private boolean autoSelectedPreferedPrinter;

	/**
	 * Name of the preferred cups printer
	 */
	@Column(columnDefinition = "VARCHAR")
	private String preferedPrinter;

	/**
	 * True if the label printer should be autoselected
	 */
	@Column
	private boolean autoSelectedPreferedLabelPrinter;

	/**
	 * The uuid of the preferred labelprinter
	 */
	@Column(columnDefinition = "VARCHAR")
	private String preferedLabelPritner;

	/**
	 * Page which should be shown as default page
	 */
	@Enumerated(EnumType.ORDINAL)
	private View defaultView;

	/**
	 * Default worklist to load, staining, diagnosis, notification, none
	 */
	@Enumerated(EnumType.ORDINAL)
	private WorklistSearchOption worklistToLoad;

	/**
	 * Default sortorder of worklist
	 */
	@Enumerated(EnumType.ORDINAL)
	private WorklistSortOrder worklistSortOrder;

	/**
	 * True the sort order is ascending, false the sortorder is descending
	 */
	@Column
	private boolean worklistSortOrderAsc;

	/**
	 * If true none active tasks in worklist will be hidden per default
	 */
	@Column
	private boolean worklistHideNoneActiveTasks;

	/**
	 * True if autoupdate of the current worklist should be happening
	 */
	@Column
	private boolean worklistAutoUpdate;

	/**
	 * If true, a patient added viea quciksearch will be added to the worklist
	 * an the create task dialog will be opend.
	 */
	@Column
	private boolean alternatePatientAddMode;

	/**
	 * Background-Color of all inputfields
	 */
	@Column(columnDefinition = "VARCHAR", length = 7)
	private String inputFieldColor;

	/**
	 * Font-Color of the inputfields
	 */
	@Column(columnDefinition = "VARCHAR", length = 7)
	private String inputFieldFontColor;

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
	}

	/**
	 * Updates the user settings with predefined settings on role change
	 * 
	 * @param predefinedRoleSettings
	 */
	public void updateUserSettings(PredefinedRoleSettings predefinedRoleSettings) {
		setDefaultView(predefinedRoleSettings.getDefaultView());
		setAutoSelectedPreferedLabelPrinter(predefinedRoleSettings.isAutoSelectedPreferedLabelPrinter());
		setAutoSelectedPreferedPrinter(predefinedRoleSettings.isAutoSelectedPreferedPrinter());
		setWorklistToLoad(predefinedRoleSettings.getWorklistToLoad());
		setWorklistSortOrder(predefinedRoleSettings.getWorklistSortOrder());
		setWorklistSortOrderAsc(predefinedRoleSettings.isWorklistSortOrderAscending());
		setWorklistHideNoneActiveTasks(predefinedRoleSettings.isHideNoneActiveTasks());
	}

	@Transient
	public List<Role> getAuthorities() {
		List<Role> result = new ArrayList<Role>();
		result.add(role);
		return result;
	}

	@Transient
	@Override
	public String getPassword() {
		return null;
	}

	@Override
	public String toString() {
		return "HistoUser [id=" + id + ", username=" + username + "]";
	}

	@Override
	public boolean equals(Object obj) {

		if (obj instanceof HistoUser && ((HistoUser) obj).getId() == getId())
			return true;

		return super.equals(obj);
	}
}