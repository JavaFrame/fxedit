let Binding = require("util.binding");
let Window = require("window.window");
let Tab = require("window.tab");
let TextEditor = require("editor.texteditor");
let FileSource = require("source.filesource");

let filechooser = require("util.filechooser");

let root = require("window.root");

let window = root.windows.get(0);
window.title.value = "FXEdit";

let shortcuts = root.getShortcutManager();
let normalMode = shortcuts.currentMode;

normalMode.register("C-t", ctx => {
    let selectedWindow = root.currentWindow.value;
    let tab = new Tab();
    tab.title.value = "New File";

    let textEditor = new TextEditor();
    tab.editor.value = textEditor;
    selectedWindow.centerTabs.value.tabs.push(tab);
})

normalMode.register("C-w", ctx => {
    let selectedWindow = root.currentWindow.value;
    let tabs = selectedWindow.centerTabs.value.tabs;
    let tabArray = tabs._value;
    
    let currentTab = root.currentTab.value;
    let tabIndex = tabArray.indexOf(currentTab);
    if(tabIndex < 0) return;
    print("index:", tabIndex)
    tabs.splice(tabIndex, 1);
    
})

normalMode.register("C-o", ctx => {
    let currentTab = root.currentTab.value;
    if(currentTab == null) return;
    let editor = currentTab.editor.value;
    if(editor == null) {
        editor = new TextEditor();
        currentTab.editor.value = editor;
    }

    filechooser.openFileChooser(false, "Open File", path => {
        if(path == null) return;
        let source = new FileSource(path);
        editor.source.value = source;
    });
})

normalMode.register("C-s", ctx => {
    let currentTab = root.currentTab.value;
    if(currentTab == null) return;
    let editor = currentTab.editor.value;
    if(editor == null) return;
    let source = editor.source.value;
    source.save();
});
