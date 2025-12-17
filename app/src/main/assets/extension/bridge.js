function initBridge(){
    try {
        const bridgeObject = {
            postMessage: function(message){
                // Envia para background.js
                browser.runtime.sendMessage({
                    action: "JSBridge",
                    data: message
                });
            }
        };

        // Exporta o objeto para a janela da página web (window)
        window.wrappedJSObject.JSBridge = cloneInto(
            bridgeObject,
            window,
            { cloneFunctions: true }
        );
    } catch (e) {
        console.error("JSBridge: Erro na injeção:", e);
    }
}

initBridge();

// listener das mensagens vindas do background.js (Java -> Background -> Content)
browser.runtime.onMessage.addListener((request) => {
    if(request.action === 'evalJavascript'){
        try {
            const result = window.eval(request.data);
            return Promise.resolve({ action: "evalJavascript", data: result || "success", id: request.id });
        } catch (e) {
            return Promise.resolve({ action: "evalJavascript", data: e.toString(), id: request.id });
        }
    }
});