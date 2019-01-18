const javaLibInit = "test.init";
class Test {
    constructor() {
        _initObj("test", this)
    }
}
_initClass("test", Test)
return Test;