
class Util {
    constructor() { }

    create(jsConstructor, args) {
        print("start")
        try {
            print("call as constructor")
            return new (jsConstructor)(args);
        } catch(e) {
            try {
                print("call as function")
                return jsConstructor(args);
            } catch(e) {
                print("return given object")
                return jsConstructor;
            }
        }
    }
}


return new Util();