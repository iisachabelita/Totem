'use strict';

const port = browser.runtime.connectNative("browser");

// Recebe mensagens do MainActivity.java e envia para bridge.js
port.onMessage.addListener(async (request) => {
    const tabs = await browser.tabs.query({ active: true, currentWindow: true });
    if(tabs.length > 0){
        const targetTabId = tabs[0].id;

        if(request.action === "evalJavascript"){
            const response = await browser.tabs.sendMessage(targetTabId, request);
            port.postMessage(response); // Devolve
        }
    }
});

// Recebe mensagens do bridge.js e envia para o MainActivity.java
browser.runtime.onMessage.addListener((data, sender) => {
    if(data.action === 'JSBridge'){
        port.postMessage(data); // Envia para mPortDelegate
    }
    return Promise.resolve('');
});