let Binding = require("binding");
let Test = require("test")

let testObj = []
let testProxy = new Proxy(testObj, {
    set(target, property, value, receiver) {
        print("set: ", target, property, value, receiver)
        return true;
    },
    deleteProperty(target, property) {
        print("delete: ", target, property)
        return true;
    }, 
    defineProperty(target, property, descriptor) {
        print("define: ", target, property, descriptor)
        return true;
    },
    apply(target, thisArg, argumentsList) {
        print("applay: ", target, thisArg, argumentsList)
    }
})

testProxy.testProp1 = "testvalue1 for testProp1";
testProxy.push("test push")
