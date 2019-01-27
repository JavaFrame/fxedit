class Root {
    constructor() {
        _initObj("window.root", this);
    }
}

_initClass("window.root", Root);
return new Root();