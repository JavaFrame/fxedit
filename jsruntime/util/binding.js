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

/**
 * Represents a change to an Array
 */
class Change {
	/**
	 * the change added the values found on the newArray in the
	 * range returned by Change.to and Change.from
	 */
	static get ADD_TYPE() { return "add"; }
	/**
	 * the change permuted the values found on the newArray in the
	 * range returned by Change.to and Change.from. More information
	 * is not transmitted to js
	 */
	static get PERMUTATION_TYPE() { return "permutation"; }
	/**
	 * the change removed the values found on the newArray in the
	 * range returned by Change.to and Change.from. 	 
	 */
	static get REMOVE_TYPE() { return "remove"; }
	/**
	 * the change updated the values found on the newArray in the
	 * range returned by Change.to and Change.from. 
	 */
	static get UPDATE_TYPE() { return "update"; }
	/**
	 * The ArrayBinding is reseted to the value of the java ObservableList.
	 * The cause can be either a diffrence or an exception while applying changes to the java observable list or
	 * the js ArrayBinding. It also happens when the js and java values are binded together, because the js value
	 * is synced to the java.<br><br>
	 * 
	 * After this change is received, you should throw away all you knowledge about the array. It might be diffrent,
	 * but it could be the same.
	 */
	static get RESET_TYPE() { return "reset"; }

	/**
	 *	constructor 
	 * @param {string} type the type of the change
	 * @param {number} from where the change started (inclusive)
	 * @param {number} to  where the change ended (exclusive)
	 */
	constructor(type, from, to) {
		this.type = type;
		this.from = from;
		this.to = to;
	}
}

/**
 * An array which CAN be bound to a java ObservableList
 */
class ArrayBinding {
	/**
	 * constructor
	 * @param {array} initValue  the initial value. If this ArrayBinding is bound
	 * to an observable list, the value is rested to the value of the java list
	 */
	constructor(initValue) {
		this._value = initValue;
		this._listeners = [];
	}

	get(index) {
		return this._value[index];
	}

	/**
	 * Pushes an value on to the array. If it is bound to an java instance,
	 * the value has to be serializeable 
	 * @param {*} value the value to add to the array
	 */
	push(value) {
		let oldValue = this._value;
		let changes = [new Change(Change.ADD_TYPE, oldValue.length, oldValue.length + 1)];
		this._value.push(value)
		this._fireListeners(oldValue, this._value, changes)
	}

	/**
	 * sets the value at the given index. If it is bound to an java instance,
	 * the new value has to be serializable 
	 * @param {number} index which value should be replaced
	 * @param {*} value the value to replace the old value
	 */
	set(index, value) {
		let oldValue = this._value;
		let changes = [new Change(Change.UPDATE_TYPE, index, index + 1)];
		this._value[index] = value;
		this._fireListeners(oldValue, this._value, changes)
	}

	/**
	 * Splices the array, like Array.prototype.splice(start, [deleteCount, [item... ]])
	 * @param {number} start the start index
	 * @param {number} deleteCount the number of value after the start which should be removed
	 * @param {...*} newValues new values which are inserted after the start index. If it is bound to an 
	 * java instance, the new values have to be serializable
	 */
	splice(start=0, deleteCount=0, newValues=null) {
		let oldValue = this._value;
		if(deleteCount == null)
			deleteCount = this._value.length;
		let startI = Math.min(start, this._value.length);
		if(startI < 0) startI = this._value.length - startI;
		let deleteCountI = Math.min(deleteCount, this._value.length-start);
		if(deleteCountI < 0) deleteCountI = 0;
		let changes = [new Change(Change.REMOVE_TYPE, startI, startI+deleteCountI)];
		this._value.splice.bind(this._value, arguments);
		this._fireListeners(oldValue, this._value, changes);
	}

	/**
	 * adds a new listener to the array 
	 * 
	 * @callback
	 * @param {array} oldArray the old array before the change
	 * @param {array} newArray the new Array after the change
	 * @param {Change[]} changes the changes which happend to the oldArray which
	 * transformed it the newArray
	 */
	addListener(listener) {
		this._listeners.push(listener);
	}

	/**
	 * fires all registered listeners 
	 * @param {array} oldArray the oldArray
	 * @param {array} newArray  the new array
	 * @param {Change[]} changes the changes which where applied to the oldArray
	 */
	_fireListeners(oldArray, newArray, changes) {
		this._listeners.forEach(l => l(oldArray, newArray, changes));
	}
}

return {
	Binding: Binding,
	Change: Change,
	ArrayBinding: ArrayBinding,
};
