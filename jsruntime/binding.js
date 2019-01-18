class Binding {
	/**
	 * @constructor
	 * Creates a new binding.
	 * It can be used as a Function:
	 * <pre>
	 * let b = new Binding()
	 * b(10) //sets the value to 10 and returns the new value
	 * b() // just returns the value 
	 * </pre>
	 * There are also the Binding.value property:
	 * <pre>
	 * let b = new Binding()
	 * b.value = 10 //sets the value to 10 and returns the new value
	 * b.value // just returns the value 
	 * </pre>
	 * 
	 * and the Binding.listeners property:
	 * <pre>
	 * let b = new Binding()
	 * b.listeners // returns all listeners
	 * b.addListener(listener) // registers a new listener
	 * </pre>
	 * @param {*} initValue 
	 */
	constructor(initValue) {
		this._value = initValue;
		this.listeners = [];
	}

	get value() {
		return this._value
	}

	set value(newValue) {
		let oldValue = this._value;
		this._value = newValue;
		this.fireListener(oldValue, newValue);
	}

	setValue(newValue) {
		this.value = newValue;
	}

	getValue() {
		return this.value;
	}
	/**
	 * Adds a new listener
	 * @callback
	 * @param {*} oldValue the old value before the change
	 * @param {*} newValue the new value after the change
	 */
	addListener(listener) {
		this.listeners.push(listener);
	}

	/**
	 * Fires all listeners
	 * @param {*} oldValue the oldValue
	 * @param {*} newValue the newValue
	 */
	fireListener(oldValue, newValue) {
		this.listeners.forEach(l => {
			l(oldValue, newValue)
		});
	}

}

class Change {
	static ADD_TYPE = "add";
	static PERMUTATION_TYPE = "permutation";
	static REMOVE_TYPE = "remove";
	static UPDATE_TYPE = "update";

	constructor(type, from, to) {
		this.type = type;
		this.from = from;
		this.to = to;
	}

}

class ArrayBinding {
	constructor(initValue) {
		this._value = initValue;
		this._listeners = [];
	}

	push(value) {
		let oldValue = this._value;
		let changes = [new Change(Change.ADD_TYPE, oldValue.length, oldValue.length + 1)];
		this._value.push(value)
		this._fireListeners(oldValue, this._value, changes)
	}

	set(index, value) {
		let oldValue = this._value;
		let changes = [new Change(Cahnge.UPDATE_TYPE, index, index + 1)];
		this._value[index] = value;
		this._fireListeners(oldValue, this._value, changes)
	}

	slice(start=0, end=null) {
		let oldValue = this._value;
		if(end == null)
			end = this._value.length;
		let startI = start;
		if(startI < 0) startI = this._value.length - startI;
		let endI = Math.min(end, this._value.length);
		if(endI < 0) endI = this._value.length - endI;
		let changes = [new Change(Cahnge.REMOVE_TYPE, startI, endI)];
		this._value.slice(start, end);
		this._fireListenersl(oldValue, this._value, changes);
	}

	addListener(listener) {
		this._listeners.push(listener);
	}

	_fireListeners(oldArray, newArray, changes) {
		this._listeners.forEach(l => l(oldArray, newArray, changes));
	}
}

return {
	Binding: Binding,
	Change: Change,
	ArrayBinding: ArrayBinding,
};
