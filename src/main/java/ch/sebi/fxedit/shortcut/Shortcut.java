package ch.sebi.fxedit.shortcut;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class Shortcut {
	private BooleanProperty ctrlModifier = new SimpleBooleanProperty();
	private BooleanProperty altModifier = new SimpleBooleanProperty();
	private BooleanProperty shiftModifier = new SimpleBooleanProperty();
	private StringProperty keys = new SimpleStringProperty();

	
	public Shortcut(String keys, boolean ctrlModifier, boolean altModifier, boolean shifModifier) {
		this.keys.set(keys);
		this.ctrlModifier.set(ctrlModifier);
		this.altModifier.set(altModifier);
		this.shiftModifier.set(shifModifier);
	}
	
	public String getKeys() {
		return keys.get();
	}
	
	public void setKeys(String keys) {
		this.keys.set(keys);
	}
	
	public StringProperty keysProperty() {
		return keys;
	}
	
	public boolean isCtrlModifier() {
		return ctrlModifier.get();
	}
	
	public void setCtrlModifier(boolean modifier) {
		ctrlModifier.set(modifier);
	}
	
	public BooleanProperty ctrlModifierProperty() {
		return ctrlModifier;
	}


	public boolean isAltModifier() {
		return altModifier.get();
	}
	
	public void setAltModifier(boolean modifier) {
		altModifier.set(modifier);
	}
	
	public BooleanProperty altModifierProperty() {
		return altModifier;
	}
	
	
	public boolean isShiftModifier() {
		return shiftModifier.get();
	}
	
	public void setShiftModifier(boolean modifier) {
		shiftModifier.set(modifier);
	}
	
	public BooleanProperty shiftModifierProperty() {
		return shiftModifier;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((altModifier == null) ? 0 : altModifier.hashCode());
		result = prime * result + ((ctrlModifier == null) ? 0 : ctrlModifier.hashCode());
		result = prime * result + ((keys == null) ? 0 : keys.hashCode());
		result = prime * result + ((shiftModifier == null) ? 0 : shiftModifier.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Shortcut other = (Shortcut) obj;
		if(other.isAltModifier() != isAltModifier()) return false;
		if(other.isCtrlModifier() != isCtrlModifier()) return false;
		if(other.isShiftModifier() != isShiftModifier()) return false;
		if(!other.getKeys().equals(getKeys())) return false;
		return true;
	}
	
}
