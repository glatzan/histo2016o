package org.histo.search;

import java.util.ArrayList;
import java.util.List;

import org.primefaces.model.OrganigramNode;

public class SearchCondition implements OrganigramNode {

	public static final String DEFAULT_TYPE = "default";

	protected String type;
	protected Object data;
	protected List<OrganigramNode> children;
	protected OrganigramNode parent;
	protected String rowKey;

	protected boolean expanded = true;

	protected boolean selectable;
	protected boolean draggable;
	protected boolean droppable;
	protected boolean collapsible = true;

	public SearchCondition() {
		this.type = DEFAULT_TYPE;
		this.children = new ArrayList<OrganigramNode>();
	}

	@Override
	public void clearParent() {
		this.parent = null;
	}

	@Override
	public int getChildCount() {
		return children.size();
	}

	@Override
	public List<OrganigramNode> getChildren() {
		return children;
	}

	@Override
	public Object getData() {
		return data;
	}

	@Override
	public OrganigramNode getParent() {
		return parent;
	}

	@Override
	public String getRowKey() {
		return rowKey;
	}

	@Override
	public String getType() {
		return type;
	}

	@Override
	public boolean isCollapsible() {
		return collapsible;
	}

	@Override
	public boolean isDraggable() {
		return draggable;
	}

	@Override
	public boolean isDroppable() {
		return droppable;
	}

	@Override
	public boolean isExpanded() {
		return expanded;
	}

	@Override
	public boolean isLeaf() {
		if (children == null) {
			return true;
		}

		return children.isEmpty();
	}

	@Override
	public boolean isSelectable() {
		return selectable;
	}

	@Override
	public void setChildren(List<OrganigramNode> children) {
		this.children = children;
	}

	@Override
	public void setCollapsible(boolean collapsible) {
		this.collapsible = collapsible;
	}

	@Override
	public void setData(Object data) {
		this.data = data;
	}

	@Override
	public void setDraggable(boolean draggable) {
		this.draggable = draggable;

	}

	@Override
	public void setDroppable(boolean droppable) {
		this.droppable = droppable;
	}

	@Override
	public void setExpanded(boolean expanded) {
		this.expanded = expanded;
	}

	@Override
	public void setParent(OrganigramNode parent) {
		if (parent != null) {
			parent.getChildren().add(this);
		}
		this.parent = parent;
	}

	@Override
	public void setRowKey(String rowKey) {
		this.rowKey = rowKey;
	}

	@Override
	public void setSelectable(boolean selectable) {
		this.selectable = selectable;
	}

	@Override
	public void setType(String type) {
		this.type = type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((data == null) ? 0 : data.hashCode());
		return result;
	}

//	@Override
//	public boolean equals(Object obj) {
//		if (this == obj) {
//			return true;
//		}
//		if (obj == null) {
//			return false;
//		}
//		if (getClass() != obj.getClass()) {
//			return false;
//		}
//
//		DefaultOrganigramNode other = (DefaultOrganigramNode) obj;
//		if (data == null) {
//			if (other.data != null) {
//				return false;
//			}
//		} else if (!data.equals(other.data)) {
//			return false;
//		}
//
//		return true;
//	}

}
