package org.histo.model.patient;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;
import javax.persistence.OrderBy;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;
import javax.persistence.Version;

import org.hibernate.annotations.DynamicUpdate;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.SelectBeforeUpdate;
import org.hibernate.envers.Audited;
import org.hibernate.envers.NotAudited;
import org.histo.config.enums.Dialog;
import org.histo.model.MaterialPreset;
import org.histo.model.interfaces.DeleteAble;
import org.histo.model.interfaces.HasID;
import org.histo.model.interfaces.IdManuallyAltered;
import org.histo.model.interfaces.LogAble;
import org.histo.model.interfaces.Parent;
import org.histo.model.interfaces.PatientRollbackAble;
import org.histo.util.TaskUtil;

import lombok.Getter;
import lombok.Setter;

@Entity
@Audited
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "sample_sequencegenerator", sequenceName = "sample_sequence")
@Getter
@Setter
public class Sample implements Parent<Task>, LogAble, DeleteAble, PatientRollbackAble<Task>, IdManuallyAltered, HasID {

	@Id
	@GeneratedValue(generator = "sample_sequencegenerator")
	@Column(unique = true, nullable = false)
	private long id;

	@Version
	private long version;

	/**
	 * Parent of this sample.
	 */
	@ManyToOne(targetEntity = Task.class)
	private Task parent;

	/**
	 * Sample ID as string
	 */
	@Column
	private String sampleID = "";

	/**
	 * True if the user has manually altered the sample ID
	 */
	@Column
	private boolean idManuallyAltered;

	/**
	 * Date of sample creation
	 */
	@Column
	private long creationDate = 0;

	/**
	 * If true the not completed stainings are restainings.
	 */
	@Column
	private boolean reStainingPhase = false;

	/**
	 * blocks array
	 */
	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("blockID ASC")
	private List<Block> blocks = new ArrayList<Block>();

	/**
	 * Material name is first initialized with the name of the typeOfMaterial.
	 * Can be later changed.
	 */
	private String material = "";

	/**
	 * Material object, containing preset for staining
	 */
	@OneToOne
	@NotAudited
	private MaterialPreset materilaPreset;

	public Sample() {
	}

	/**
	 * Generates a sample name, if useAutoNomenclature is true an name will be
	 * auto generated
	 * 
	 * @param useAutoNomenclature
	 */
	@Transient
	public boolean updateNameOfSample(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		if (!isIdManuallyAltered() || (ignoreManuallyNamedItems && isIdManuallyAltered())) {

			if (useAutoNomenclature && getParent().getSamples().size() > 1) {
				String name = TaskUtil.getRomanNumber(getParent().getSamples().indexOf(this) + 1);

				if (getSampleID() == null || !getSampleID().equals(name)) {
					setSampleID(name);
					setIdManuallyAltered(false);
					return true;
				}
			}
		}

		return false;
	}
	
	@Transient
	public void updateAllNames() {
		updateAllNames(getTask().isUseAutoNomenclature(), false);
	}

	/**
	 * Updates the name of all block children
	 * 
	 * @param useAutoNomenclature
	 */
	@Transient
	public void updateAllNames(boolean useAutoNomenclature, boolean ignoreManuallyNamedItems) {
		updateNameOfSample(useAutoNomenclature, ignoreManuallyNamedItems);
		getBlocks().stream().forEach(p -> p.updateAllNames(useAutoNomenclature, ignoreManuallyNamedItems));
	}

	@Override
	@Transient
	public String toString() {
		return "Sample: " + getSampleID() + (getId() != 0 ? ", ID: " + getId() : "");
	}


	/********************************************************
	 * Interface Parent
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface StainingTreeParent
	 */
	@Transient
	@Override
	public Patient getPatient() {
		return getParent().getPatient();
	}

	/**
	 * Returns the parent task
	 */
	@Override
	@Transient
	public Task getTask() {
		return getParent();
	}

	/********************************************************
	 * Interface Parent
	 ********************************************************/

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Gibt die SampleID als
	 * identifier zur�ck
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Probe " + getSampleID();
	}

	/**
	 * �berschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zur�ck
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.DELETE_TREE_ENTITY;
	}

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/
}
