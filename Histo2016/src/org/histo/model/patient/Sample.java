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

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
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

@Entity
@Audited
@Cache(usage = CacheConcurrencyStrategy.TRANSACTIONAL)
@SelectBeforeUpdate(true)
@DynamicUpdate(true)
@SequenceGenerator(name = "sample_sequencegenerator", sequenceName = "sample_sequence")
public class Sample implements Parent<Task>, LogAble, DeleteAble, PatientRollbackAble, IdManuallyAltered, HasID {

	private long id;

	private long version;

	/**
	 * Parent of this sample.
	 */
	private Task parent;

	/**
	 * Sample ID as string
	 */
	private String sampleID = "";

	/**
	 * True if the user has manually altered the sample ID
	 */
	private boolean idManuallyAltered;

	/**
	 * Date of sample creation
	 */
	private long creationDate = 0;

	/**
	 * If true the not completed stainings are restainings.
	 */
	private boolean reStainingPhase = false;

	/**
	 * blocks array
	 */
	private List<Block> blocks;

	/**
	 * Material name is first initialized with the name of the typeOfMaterial.
	 * Can be later changed.
	 */
	private String material = "";

	/**
	 * Material object, containing preset for staining
	 */
	private MaterialPreset materilaPreset;

	public Sample() {
	}

	/**
	 * Constructor for adding this sample to a task.
	 * 
	 * @param task
	 */
	public Sample(Task task) {
		this(task, null);
	}

	/**
	 * Constructor for adding this sample to a task and set the material as
	 * well.
	 * 
	 * @param task
	 */
	public Sample(Task task, MaterialPreset material) {
		setCreationDate(System.currentTimeMillis());
		setParent(task);
		setMaterilaPreset(material);
		setMaterial(material == null ? "" : material.getName());
		task.getSamples().add(this);

		updateNameOfSample(task.isUseAutoNomenclature(), false);
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
		return "ID: " + getId() + ", Sample ID: " + getSampleID();
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/
	@Id
	@GeneratedValue(generator = "sample_sequencegenerator")
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

	@OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", fetch = FetchType.EAGER)
	@Fetch(value = FetchMode.SUBSELECT)
	@OrderBy("blockID ASC")
	public List<Block> getBlocks() {
		if (blocks == null)
			blocks = new ArrayList<Block>();
		return blocks;
	}

	public void setBlocks(List<Block> blocks) {
		this.blocks = blocks;
	}

	@Basic
	public String getSampleID() {
		return sampleID;
	}

	public void setSampleID(String sampleID) {
		this.sampleID = sampleID;
	}

	public long getCreationDate() {
		return creationDate;
	}

	public void setCreationDate(long creationDate) {
		this.creationDate = creationDate;
	}

	@Basic
	public boolean isReStainingPhase() {
		return reStainingPhase;
	}

	public void setReStainingPhase(boolean reStainingPhase) {
		this.reStainingPhase = reStainingPhase;
	}

	@Basic
	public String getMaterial() {
		return material;
	}

	public void setMaterial(String material) {
		this.material = material;
	}

	@OneToOne
	@NotAudited
	public MaterialPreset getMaterilaPreset() {
		return materilaPreset;
	}

	public void setMaterilaPreset(MaterialPreset materilaPreset) {
		this.materilaPreset = materilaPreset;
	}

	public boolean isIdManuallyAltered() {
		return idManuallyAltered;
	}

	public void setIdManuallyAltered(boolean idManuallyAltered) {
		this.idManuallyAltered = idManuallyAltered;
	}

	/********************************************************
	 * Getter/Setter
	 ********************************************************/

	/******************************************************** Transient ********************************************************/

	/******************************************************** Transient ********************************************************/

	/********************************************************
	 * Interface Parent
	 ********************************************************/
	@ManyToOne(targetEntity = Task.class)
	public Task getParent() {
		return parent;
	}

	public void setParent(Task parent) {
		this.parent = parent;
	}

	/**
	 * Überschreibt Methode aus dem Interface StainingTreeParent
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
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt die SampleID als
	 * identifier zurück
	 */
	@Transient
	@Override
	public String getTextIdentifier() {
		return "Probe " + getSampleID();
	}

	/**
	 * Überschreibt Methode aus dem Interface ArchiveAble Gibt den Dialog zum
	 * archivieren zurück
	 */
	@Transient
	@Override
	public Dialog getArchiveDialog() {
		return Dialog.DELETE_TREE_ENTITY;
	}

	/********************************************************
	 * Interface Delete Able
	 ********************************************************/

	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
	@Override
	@Transient
	public String getLogPath() {
		return getParent().getLogPath() + ", Sample-ID: " + getSampleID() + " (" + getId() + ")";
	}
	/********************************************************
	 * Interface PatientRollbackAble
	 ********************************************************/
}
