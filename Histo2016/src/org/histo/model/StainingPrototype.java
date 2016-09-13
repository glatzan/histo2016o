package org.histo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.SequenceGenerator;
import javax.persistence.Transient;

import org.histo.model.util.EditAbleEntity;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

@Entity
@SequenceGenerator(name = "stainingPrototype_sequencegenerator", sequenceName = "stainingPrototype_sequence")
public class StainingPrototype implements EditAbleEntity<StainingPrototype>{

    public static final int TYPE_NORMAL = 0;
    public static final int TYPE_IMMUN = 1;
    
    @Expose
    private long id;
    
    @Expose
    private String name;
    
    @Expose
    private String commentary;
    
    @Expose
    private int type;
    
    @Expose
    private boolean archived;
    
    public StainingPrototype() {
    }
    
    public StainingPrototype(StainingPrototype stainingPrototype) {
	this.id = stainingPrototype.getId();
	update(stainingPrototype);
    }
    
    /******************************************************** Getter/Setter ********************************************************/
    @Id
    @GeneratedValue(generator = "stainingPrototype_sequencegenerator")
    @Column(unique = true, nullable = false)
    public long getId() {
        return id;
    }
    public void setId(long id) {
        this.id = id;
    }
    
    @Column
    public String getName() {
	return name;
    }
    public void setName(String name) {
	this.name = name;
    }
    
    @Column(columnDefinition = "text")
    public String getCommentary() {
        return commentary;
    }
    public void setCommentary(String commentary) {
        this.commentary = commentary;
    }

    @Column
    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    @Column
    public boolean isArchived() {
        return archived;
    }

    public void setArchived(boolean archived) {
        this.archived = archived;
    }
    /******************************************************** Getter/Setter ********************************************************/
    
    @Transient
    public void update(StainingPrototype stainingPrototype){
	this.name = stainingPrototype.getName();
	this.commentary = stainingPrototype.getCommentary();
	this.type = stainingPrototype.getType();
    }
    
    @Transient
    public String asGson() {
	final GsonBuilder builder = new GsonBuilder();
	builder.excludeFieldsWithoutExposeAnnotation();
	final Gson gson = builder.create();
	return gson.toJson(this);
    }
    
    @Transient
    public String getTypeAsString(){
	switch (getType()) {
	case TYPE_NORMAL:
	    return "Standard";
	case TYPE_IMMUN:
	    return "Immun";
	default:
	    return "";
	}
    }
}
