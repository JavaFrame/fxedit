let Mode = require("util.shortcutmode");
class Shortcut {
    constructor(keys, mode, listener) {
        this.keys = keys;
        this.mode = mode;
        this.listener = listener;
    }

    /**
     * Attempts to match the given input sequence fully. 
     * A shortcut which receivs a selection cannot be fully matched, 
     * because it always needs the selection.<br>
     * For this function to return true, the input sequence has to match the
     * key sequence of this object exactly (length and values).
     * 
     * @param {array} input an array with the inputs as string
     * @return {boolean} if the input sequence matches the key sequence of the shortcut
     * @see #partMatch(input) see partMatch to match shortcut which receives a selection
     */
    fullMatch(input) {
        if(input.length != this.keys.length) return false;
        //cannot full match a shortcut which receivs a selection
        if(this.mode != Mode.NONE) return false;
        for(let i = 0; i < input.length && i < this.keys.length; i++) {
            if(input[i] != this.keys[i]) return false;
        }
        return true;
    }

    /**
     * If the shortcut receives a selection and the given input
     * sequence starts with the sequence of this shortcut object
     * than this function returns the remaining inputs to process.
     * If the given input doesn't match the sequence of the shortcut or
     * there are no remaining input to process than this function returns
     * false
     * @param {array} input the input sequence
     * @return {array|false} returns either the remaining inputs or false
     * if the inputs don't match or there is no remaining inputs to processe
     */
    partMatch(input) {
        if(input.length <= this.keys.length) return false;
        for(let i = 0; i < this.keys.length; i++) {
            if(input[i] != this.keys[i]) return false;
        }
        return input.slice(this.keys.length);
    }
}
class ShortcutMode {
    constructor() {
        this._shortcuts = [];
    }

    register(keys, mode, listener) {
        this._shortcuts.push(new Shortcut(keys, mode, listener));
    }

    /**
     * Tries to match a shortcut to the given key sequence. If one is matched, then it
     * is executed (except if try is set to true) and the return result is returned, else
     * false is returned.
     * 
     * @param {array} keys the pressed keys in string form
     * @param {boolean} dry if the command shortcut should be executed
     * @param {object} context the context object given to the shortcut
     * @return  {*|false|true} returns false if no shortcut could be matched, else
     * it returns the return value of the shortcut listener function. If
     * dry is set to true, then the function will return true, if a 
     * shortcut was matched and found and else false.
     */
    execute(keys, context={}, dry=false) {
        let shortcut = this._shortcuts.find(e => e.fullMatch(keys));
        let remaining = [];
        let selection = null;
        //no full match found -> attempt to find partly matched
        if(shortcut == null) {
            for(let i = 0; i < this._shortcuts.length; i++) {
                let returnsValue = this._shortcuts[i].partMatch(keys); 
                if(returnsValue !== false) {
                    remaining = returnsValue;
                    break; 
                }
            }
        }
        //no partly match found
        if(shortcut == null) {
            return false;
        }
        //the shortcut expects receiving a selection, but no input is left
        if(shortcut.mode == Mode.RECEIVE_SELECTION && remaining.length == 0) 
            return false;
        //the shortcut doesn't expect receiving a selection, but there is still input left to process
        if(shortcut.mode != Mode.RECEIVE_SELECTION && remaining.length != 0) 
            return false;

        if(shortcut.mode == Mode.RECEIVE_SELECTION) {
            selection = this._executeSelection(remaining, dry, context);
            //if no selection was found, return
            if(selection === false) return false;
            //sets the context selection
            context.selection = selection;
        }

        // calls shortcut and returns result
        shortcut.listener(context);
        return true;
    }


    /**
     * finds a selection shortcut with the given key sequence and if one is found
     * it will execute it (except if dry is set to true) and returns the selection, else
     * false is returned
     * 
     * @param {array} keys the pressed keys in string form
     * @param {boolean} dry if the command shortcut should be executed
     * @param {object} context the context object given to the shortcut
     * @return  {*|false|true} returns false if no shortcut could be matched, else
     * it returns the selection returned by the shortcut listener function. If
     * dry is set to true, then the function will return true, if a 
     * shortcut was matched and found and else false.
     */
    _executeSelection(keys, dry=false, context={}) {
        let selection = false;
        //attempt to find selection shortcut for remaing input which returns a selection
        let remainingShortcut = this._shortcuts.find(e => e.mode == Mode.SELECTION && e.fullMatch(keys));
        if(remainingShortcut == null) return false; 
        if(!dry) {
            // executes shortcut
            selection = remainingShortcut.listener(context);
        } else {
            return true;
        }
        return selection;
    }
}

class ShortcutManager {
    constructor() {
        this._modes = {};
        this._currentModeName = "normal";
        this._currentMode = this.createMode(this._currentModeName);

        _initObj("util.shortcut", this);
    }

    createMode(name) {
        return this._modes[name] = new ShortcutMode();
    }

    get(name) {
        return this._modes[name];
    }

    exists(name) {
        return this._modes[name] === undefined;
    }

    switchMode(name) {
        if(!exists(name)) return false;
        this._currentModeName = name;
        this._currentMode = this.get(name);
        return true;
    }

    get currentMode() {
        return this._currentMode;
    }

    get currentModeName() {
        return this._currentModeName;
    }

    execute(keys, context={}, dry=false) {
        if(this.currentMode == null) throw new Error("No current mode is set");
        return this.currentMode.execute(keys, context, dry);
    }
}

_initClass("util.shortcut", ShortcutManager);
return ShortcutManager;