let Binding = require("util.binding");
let Window = require("window.window");
let Tab = require("window.tab");
let TextEditor = require("editor.texteditor");
let FileSource = require("source.filesource");

let Mode = require("util.shortcutmode");


let root = require("window.root");

root.windows.addListener((oldArray, newArray, changes) => print("windows changed"))

let window = root.windows.get(0);
window.title.value = "test title";

window.centerTabs.value.tabs.addListener((oldArray, newArray, changes) => {
    print("new tabs:")
    newArray.forEach(tab => print("tab: ", tab.title.value))
});

let tab = new Tab();
tab.title.value = "new"
window.centerTabs.value.tabs.push(tab);

let tab2 = new Tab();
tab2.title.value = "test tab 2"
window.centerTabs.value.tabs.push(tab2);

let textEditor = new TextEditor();
tab.editor.value = textEditor;

root.getShortcutManager().currentMode.register("o", Mode.NONE, ctx => {
    print("o pressed")
    let source = new FileSource("./rc.js");
    textEditor.source.value = source;
})

