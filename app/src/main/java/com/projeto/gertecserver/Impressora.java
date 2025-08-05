package com.projeto.gertecserver;

import static android.widget.Toast.makeText;
import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;
import android.widget.Toast;
import org.json.JSONObject;
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

    public Impressora(Context context){
        this.context = context.getApplicationContext(); // Use ApplicationContext
        this.printer = Printer.getInstance(this.context, this);
        printer.setPrinterOrientation(OrientationType.INVERTED);
    }

    public void imprimirComprovante(Bitmap image){
        // Receipt cupom = new Receipt();

        try {
            // Impressão texto
            // if(MyWebSocketServer.cupom.trim().isEmpty()){
            Log.e("Impressora", MyWebSocketServer.cupom);
                MyWebSocketServer.cupom = "Texto vazio";
            // }

            TextFormat textFormat = new TextFormat();
            textFormat.setBold(true);
            textFormat.setUnderscore(false);
            textFormat.setFontSize(30);
            textFormat.setLineSpacing(6);
            textFormat.setAlignment(Alignment.CENTER);

            printer.printText(textFormat,MyWebSocketServer.cupom);
            printer.scrollPaper(1);

            PrinterUtils printerUtils = printer.getPrinterUtils();
            Bitmap monochromaticBitmap = printerUtils.toMonochromatic(image, 0.5);

            //Impressão imagem
            printer.printImageAutoResize(monochromaticBitmap);
            printer.scrollPaper(1);

            printer.cutPaper(CutType.PAPER_FULL_CUT);

            //Toast de impressão
            makeText(context, "Imprimindo", Toast.LENGTH_SHORT).show();
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
