class FileSource {
    constructor(path) {
        _initObj("source.filesource", this, path);
    }
}
_initClass("source.filesource", FileSource);

return FileSource;