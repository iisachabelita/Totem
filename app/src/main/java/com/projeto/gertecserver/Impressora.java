package com.projeto.gertecserver;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import br.com.gertec.easylayer.printer.Alignment;
import br.com.gertec.easylayer.printer.CutType;
import br.com.gertec.easylayer.printer.OrientationType;
import br.com.gertec.easylayer.printer.Printer;
import br.com.gertec.easylayer.printer.PrinterError;
import br.com.gertec.easylayer.printer.PrinterException;
import br.com.gertec.easylayer.printer.PrinterUtils;
import br.com.gertec.easylayer.printer.TextFormat;

public class Impressora implements Printer.Listener {
    private final Context context;
    private final Printer printer;
    private final PrinterUtils printerUtils;

    public Impressora(Context context){
        this.context = context.getApplicationContext();
        this.printer = Printer.getInstance(this.context, this);

        //Inicialização da classe PrinterUtils
        this.printerUtils = printer.getPrinterUtils();

        printer.setPrinterOrientation(OrientationType.INVERTED);
    }

    public void imprimirComprovante(Bitmap image){
        try {
             if(MyWebSocketServer.cupom.trim().isEmpty()){
                MyWebSocketServer.cupom = "Lorem Ipsum is simply dummy text of th...";
             }

             // Divide por linha
            String[] linhas = MyWebSocketServer.cupom.split("\n");
            List<String> linhasList = Arrays.asList(linhas);
            // Inverte
            Collections.reverse(linhasList);

            // Verifica o tamanho da maior linha
            int maxLen = linhasList.stream().mapToInt(String::length).max().orElse(0);

            // Define tamanho da fonte baseado na maior linha
            int fontSize = maxLen <= 32 ? 30 :
                            maxLen <= 36 ? 26 :
                            maxLen <= 42 ? 22 :
                            maxLen <= 48 ? 18 : 16;

            // Configuração base de formatação
            TextFormat format = new TextFormat();
            format.setBold(true);
            format.setUnderscore(false);
            format.setFontSize(fontSize);
            format.setAlignment(Alignment.LEFT);
            format.setLineSpacing(4);

            for(String linha : linhasList){
                printer.printText(format,linha);
            }

            //Imprimir cupom
            // Receipt cupom = new Receipt();
            // printer.printXml(cupom);

            //Impressão imagem
            printer.scrollPaper(2);
            Bitmap monochromaticBitmap = printerUtils.toMonochromatic(image,0.5);
            // printer.printImageAutoResize(monochromaticBitmap);

            printer.cutPaper(CutType.PAPER_PARTIAL_CUT);
            MyWebSocketServer.cupom = "";
        } catch(PrinterException e){}
    }

    @Override
    public void onPrinterError(PrinterError printerError){
        Log.e("Impressora", "Erro de impressora: " + printerError.getCause());
    }

    @Override
    public void onPrinterSuccessful(int printerRequestId){
        Log.i("Impressora", "Impressão realizada com sucesso. ID: " + printerRequestId);
    }
}
