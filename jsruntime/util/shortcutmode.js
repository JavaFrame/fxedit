class Key {
    constructor(code, listener=null) {
        this.code = code;
        this.listener = listener;
        this.children = {};
    }
}

class ShortcutMode {
    static get FOUND_MATCH() { return "found match" }
    static get NO_MATCH() { return "no match" }
    static get POSSIBLE_MATCH() { return "possible match" }

    constructor(name, consumesEvents = true) {
        this.name = name;
        this.consumesEvents = consumesEvents;
        this.root = new Key("<root>");
    }

    register(keys, listener) {
        let keySplitted = keys.split(" ").reverse();
        function _register(currentKey, keys, listener) {
            if(keys.length == 0) {
                throw new Error("Invalid key code: cannot be empty")
            }
            let code = keys.pop();
            let key = currentKey.children[code];
            if(key == null) {
                key = new Key(code);
                print("create new key ", key)
                currentKey.children[code] = key;
            }
            if(keys.length > 0) {
                //calls _register with the (new) key
                _register(key, keys, listener);
            } else {
                //no codes left -> set listener to last key object
                key.listener = listener;
                print("set listener to ", key)
            }
        }

        _register(this.root, keySplitted, listener);
        print("root: ", this.root);
    }

    execute(keys, context) {
        function _execute(currentKey, keys)  {
            let code = keys.pop();
            let key = currentKey.children[code];
            if(key == null) {
                //couldn't resolve keys
                return ShortcutMode.NO_MATCH;
            }
            let childrenLength = Object.keys(key.children).length;
            if(keys.length == 0 && childrenLength != 0)  {
                print("possible match: " + key.code + ", children: ", key)
                return ShortcutMode.POSSIBLE_MATCH;  
            } else if(keys.length == 0 && childrenLength == 0) {
                let listener = key.listener;
                if(listener == null) {
                    throw new Error("Found edge key with no listener");
                }
                listener(context);
                return ShortcutMode.FOUND_MATCH;
            } else if(keys.length > 0) {
                return _execute(key, keys);
            } else {
                throw new Error("Illegal state");
            }
        }
       return _execute(this.root, keys.reverse());
    }
}

return ShortcutMode;