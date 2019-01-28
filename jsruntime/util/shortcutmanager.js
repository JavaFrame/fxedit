let ShortcutMode = require("util.shortcutmode");
class ShortcutManager {
    constructor() {
        _initObj("util.shortcutmanager", this);
        this.modes = {}

        let normalMode = new ShortcutMode("normal");
        normalMode.consumesEvents = false;
        this.addMode(normalMode);

        this.currentMode = normalMode;
    }    

    addMode(mode) {
        this.modes[mode.name] = mode;
    }

    execute(keys, context) {
        if(this.currentMode == null) throw new Error("No mode selected");
        return this.currentMode.execute(keys, context);
    }

    set currentMode(newMode) {
        this._currentMode = newMode;
        print(newMode);
        print("set consumes events to " + newMode.consumesEvents);
        this.consumesEvents.value = newMode.consumesEvents;
        this.modeName.value = newMode.name;
    }

    get currentMode() {
        return this._currentMode;
    }
}

_initClass("util.shortcutmanager", ShortcutManager);

return ShortcutManager;