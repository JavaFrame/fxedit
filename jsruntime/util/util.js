
class Util {
    constructor() { }

    create(jsConstructor, args) {
        try {
            return new (jsConstructor)(args);
        } catch(e) {
            try {
                return jsConstructor(args);
            } catch(e) {
                return jsConstructor;
            }
        }
    }
}


return new Util();