package com.projeto.gertecserver;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import br.com.gertec.easylayer.printer.Alignment;
import br.com.gertec.easylayer.printer.CutType;
import br.com.gertec.easylayer.printer.OrientationType;
import br.com.gertec.easylayer.printer.Printer;
import br.com.gertec.easylayer.printer.PrinterError;
import br.com.gertec.easylayer.printer.TextFormat;

public class Impressora implements Printer.Listener {

    private final Context context;
    private final Printer printer;

    public Impressora(Context context) {
        this.context = context.getApplicationContext(); // Use ApplicationContext
        this.printer = Printer.getInstance(this.context, this);
    }

    public String imprimir(JSONObject json) {
        try {
            JSONObject params = json.optJSONObject("parameters");
            String text = params != null ? params.optString("text", "Texto vazio") : "Texto vazio";

            if (text.trim().isEmpty()) {
                text = "Texto vazio";
            }

            TextFormat textFormat = new TextFormat();
            textFormat.setBold(true);
            textFormat.setUnderscore(false);
            textFormat.setFontSize(30);
            textFormat.setLineSpacing(6);
            textFormat.setAlignment(Alignment.CENTER);

            printer.setPrinterOrientation(OrientationType.DEFAULT);
            printer.printText(textFormat, text);
            printer.scrollPaper(3);
            printer.cutPaper(CutType.PAPER_FULL_CUT);

            Log.i("Impressora", "Impressão realizada com sucesso.");
            return "{\"status\":\"ok\",\"mensagem\":\"Impressão realizada\"}";

        } catch (Exception e) {
            Log.e("Impressora", "Erro ao imprimir: " + e.getMessage(), e);
            return "{\"status\":\"erro\",\"mensagem\":\"Erro ao imprimir: " + e.getMessage() + "\"}";
        }
    }

    @Override
    public void onPrinterError(PrinterError printerError) {
        Log.e("Impressora", "Erro de impressora: " + printerError.getCause());
    }

    @Override
    public void onPrinterSuccessful(int printerRequestId) {
        Log.i("Impressora", "Impressão realizada com sucesso. ID: " + printerRequestId);
    }
}
