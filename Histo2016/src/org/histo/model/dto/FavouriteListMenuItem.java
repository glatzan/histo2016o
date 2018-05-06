package org.histo.model.dto;

import javax.persistence.ColumnResult;
import javax.persistence.ConstructorResult;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.NamedNativeQuery;
import javax.persistence.SqlResultSetMapping;

import lombok.Getter;
import lombok.Setter;

@NamedNativeQuery(
	    name = "favouriteListMenuItemDTO",
	    query =
	        "select " +
	        "flist.id AS id, " +
	        "flist.name AS name, " + 
	        "flist.usedumplist AS dumplist," +
	        "coalesce(bool_or(fitem.task_id = :task_id),false) As containstask " +
	        "from favouritelist flist " +
	        "left join favouritepermissionsgroup fgroup on fgroup.favouritelist_id = flist.id "+
	        "left join favouritepermissionsuser fuser on fuser.favouritelist_id = flist.id "+
	        "left join favouritelistitem fitem on fitem.favouritelist_id = flist.id "+
	        "left join favouritelist_histouser hideList on hideList.favouritelist_id = flist.id "+
	        "where (owner_id = :user_id or fgroup.group_id = :group_id or fuser.user_id = :user_id) and (hideList.hidelistforuser_id IS NULL or hideList.hidelistforuser_id != :user_id) " +
	        "GROUP BY flist.id",
	    resultSetMapping = "FavouriteListMenuItem"
	)
	@SqlResultSetMapping(
	    name = "FavouriteListMenuItem",
	    classes = @ConstructorResult(
	        targetClass = FavouriteListMenuItem.class,
	        columns = {
	            @ColumnResult(name = "id", type=Long.class),
	            @ColumnResult(name = "name", type=String.class ),
	            @ColumnResult(name = "containstask", type=Boolean.class),
	            @ColumnResult(name = "dumplist", type=Boolean.class )
	        }
	    )
	)
@Getter
@Setter
@Entity
public class FavouriteListMenuItem {

	@Id
	private long tmp_id;
	
	private long id;
	private String name;
	private boolean containsTask;
	private boolean dumpList;
	
	public FavouriteListMenuItem() {
		
	}
	
	public FavouriteListMenuItem(long id, String name, boolean containsTask, boolean dumpList) {
		this.id = id;
		this.name = name;
		this.containsTask = containsTask;
		this.dumpList = dumpList;
	}
	
}
