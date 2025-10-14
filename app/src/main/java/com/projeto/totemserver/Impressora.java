package com.projeto.totemserver;

import static br.com.gertec.easylayer.printer.Alignment.CENTER;
import static br.com.gertec.easylayer.printer.Alignment.LEFT;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.Base64;
import android.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import br.com.gertec.easylayer.printer.Alignment;
import br.com.gertec.easylayer.printer.CutType;
import br.com.gertec.easylayer.printer.OrientationType;
import br.com.gertec.easylayer.printer.PrintConfig;
import br.com.gertec.easylayer.printer.Printer;
import br.com.gertec.easylayer.printer.PrinterError;
import br.com.gertec.easylayer.printer.PrinterException;
import br.com.gertec.easylayer.printer.PrinterUtils;
import br.com.gertec.easylayer.printer.TextFormat;
import android.graphics.Color;

public class Impressora implements Printer.Listener {
    private static final int MAX_PRINT_WIDTH = 384;
    private final Context context;
    private final Printer printer;
    private final PrinterUtils printerUtils;

    public Impressora(Context context){
        this.context = context.getApplicationContext();
        this.printer = Printer.getInstance(this.context, this);

        //Inicialização da classe PrinterUtils
        this.printerUtils = printer.getPrinterUtils();

        printer.setPrinterOrientation(OrientationType.DEFAULT);

        loadConfig();
    }

    private String image;
    private int fontSize;
    private int lineSpacing;
    private int maxChars;
    private int topHeight;

    private void loadConfig(){
        // SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("Printer", Context.MODE_PRIVATE);
        image = prefs.getString("image",null);
        fontSize = prefs.getInt("fontSize", 20);
        lineSpacing = prefs.getInt("lineSpacing", 0);
        maxChars = prefs.getInt("maxChars", 32);
        topHeight = prefs.getInt("topHeight", 4);
    }

    public void configurarImpressora(JSONObject parameters){
        // SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("Printer", Context.MODE_PRIVATE);

        prefs.edit().putString("image",parameters.optString("image","")).apply();

        switch(parameters.optInt("fontSize",1)){
            case 1:
                prefs.edit().putInt("fontSize", 20).apply();
                prefs.edit().putInt("topHeight", 4).apply();
                break;
            case 2:
                prefs.edit().putInt("fontSize", 25).apply();
                prefs.edit().putInt("topHeight", 6).apply();
                // Ideal
                // prefs.edit().putInt("maxChars", 25).apply();
                // prefs.edit().putInt("lineSpacing", 3).apply();
                break;
            case 3:
                prefs.edit().putInt("fontSize", 30).apply();
                prefs.edit().putInt("topHeight", 8).apply();
                // Ideal
                // prefs.edit().putInt("maxChars", 21).apply();
                // prefs.edit().putInt("lineSpacing", 5).apply();
                break;
        }

        prefs.edit().putInt("lineSpacing", parameters.optInt("lineSpacing",0)).apply();
        prefs.edit().putInt("maxChars", parameters.optInt("maxChars",32)).apply();
    }

    public void imprimirComprovante(JSONArray items, JSONObject parameters) throws PrinterException, JSONException {
        // Logo do estabelecimento
        if(image != null){
            byte[] imageBytes = Base64.decode(image,Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length);

            PrintConfig printConfig = new PrintConfig();
            printConfig.setWidth(100);
            printConfig.setHeight(100);
            printConfig.setAlignment(CENTER);
            printer.printImage(printConfig,bitmap);
            printer.scrollPaper(1);
        }

        // Separador
        PrintConfig lineConfig = new PrintConfig();
        printer.printImage(lineConfig,createLineSeparator());

        printFormat(parameters.optString("orderConsume",""),LEFT,false);

        // Separador
        printer.printImage(lineConfig,createLineSeparator());

        printFormat("Senha: ",CENTER,false);

        TextFormat textFormat = new TextFormat();
        textFormat.setBold(true);
        textFormat.setFontSize(50);
        textFormat.setLineSpacing(15);
        textFormat.setAlignment(CENTER);
        printer.printText(textFormat,parameters.optString("orderRef",""));

        // Separador
        printer.printImage(lineConfig,createLineSeparator());

        printFormat(parameters.optString("nameFantasy",""),LEFT,false);
        printFormat(parameters.optString("reasonSocial",""),LEFT,false);

        // Espaçamento
        printer.printImage(lineConfig,createLineSpace());

        printFormat(parameters.optString("adressRef",""),LEFT,false);

        // Espaçamento
        printer.printImage(lineConfig,createLineSpace());

        printFormat(parameters.optString("orderHash",""),LEFT,false);

        // Itens
        if(items != null && items.length() > 0){
            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            printFormat("Itens", LEFT, false);

            // Espaçamento
            printer.printImage(lineConfig, createLineSpace());

            for (int i = 0; i < items.length(); i++) {
                JSONObject item = items.getJSONObject(i);

                String titulo = item.optString("ite_titulo","");
                String preco = item.optString("ite_preco","");

                printFormat(formatLine(titulo, preco), LEFT, false);

                if (item.has("perguntas")) {
                    JSONArray perguntas = item.getJSONArray("perguntas");

                    for (int j = 0; j < perguntas.length(); j++) {
                        JSONObject pergunta = perguntas.getJSONObject(j);

                        if (pergunta.has("respostas") && !pergunta.isNull("respostas")) {
                            JSONArray respostas = pergunta.getJSONArray("respostas");

                            for (int k = 0; k < respostas.length(); k++) {
                                JSONObject respostaObj = respostas.getJSONObject(k);
                                String title = respostaObj.optString("res_titulo","");
                                String price = respostaObj.optString("res_preco","");

                                printFormat(formatLine(title, price), LEFT, false);
                            }
                        }
                    }
                }
            }
        }

        String obs = parameters.optString("observText","");
        if(obs != null && !obs.isEmpty()){
            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            printFormat(obs, LEFT, false);
        }

        // Separador
        printer.printImage(lineConfig,createLineSeparator());

        printFormat(parameters.optString("nameUser",""),LEFT,false);
        printFormat(parameters.optString("phoneUser",""),LEFT,false);
        printFormat(parameters.optString("cpfCnpjUser",""),LEFT,false);

        // Separador
        printer.printImage(lineConfig,createLineSeparator());

        printFormat(parameters.optString("paymentType",""),LEFT,false);
        printFormat(formatLine("Sub-total",parameters.optString("valueSubtotal","")),LEFT,false);

        String discount = parameters.optString("valueDiscount","");
        if(discount != null && !discount.isEmpty()){
            printFormat(formatLine("Desconto",discount),LEFT,false);
        }

        String fee = parameters.optString("valueFee","");
        if(fee != null && !fee.isEmpty()){
            printFormat(formatLine("Taxa pagto.",fee),LEFT,false);
        }

        printFormat(formatLine("Total",parameters.optString("valueTotal","")),LEFT,true);

        // Separador
        printer.printImage(lineConfig,createLineSeparator());
        printFormat(new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(new Date()),CENTER,false);

        printer.scrollPaper(1);
        printer.cutPaper(CutType.PAPER_PARTIAL_CUT);
    }

    private void printFormat(String s, Alignment alignment, Boolean bold) throws PrinterException{
        if(s != null && !s.isEmpty()){
            List<String> linhas = new ArrayList<>();

            // Tratativa para quebra de linha
            if(s.length() > maxChars){
                // Evitando cortar palavras
                String[] palavras = s.split(" ");
                StringBuilder linhaAtual = new StringBuilder();

                for(String palavra : palavras){
                    // Se adicionar essa palavra passar do limite, salva linha e começa outra
                    if(linhaAtual.length() > 0 && (linhaAtual.length() + 1 + palavra.length()) > maxChars){
                        linhas.add(linhaAtual.toString());
                        linhaAtual = new StringBuilder();
                    }

                    if(linhaAtual.length() > 0){
                        linhaAtual.append(" ");
                    }
                    linhaAtual.append(palavra);
                }

                // Adiciona a última linha se sobrar algo
                if(linhaAtual.length() > 0){
                    linhas.add(linhaAtual.toString());
                }
            }else{
                linhas.add(s);
            }

            for(String linha : linhas){
                TextFormat textFormat = new TextFormat();
                textFormat.setBold(bold);
                textFormat.setUnderscore(false);
                textFormat.setFontSize(fontSize);
                textFormat.setLineSpacing(lineSpacing);
                textFormat.setAlignment(alignment);

                printer.printText(textFormat,linha);
            }
        }
    }

    private String formatLine(String var1,String var2){
        if(var1.length() + var2.length() >= maxChars){
            return var1 + " " + var2;
        }

        int spaces = maxChars - var1.length() - var2.length();
        StringBuilder sb = new StringBuilder();
        sb.append(var1);
        for(int i = 0; i < spaces; i++){
            sb.append(" ");
        }
        sb.append(var2);
        return sb.toString();
    }

    private Bitmap createLineSeparator(){
        int lineHeight = 2;
        int bottomHeight = 2;
        int totalHeight = topHeight + lineHeight + bottomHeight;

        Bitmap bitmap = Bitmap.createBitmap(MAX_PRINT_WIDTH,totalHeight,Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        bitmap.eraseColor(Color.TRANSPARENT);

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        // Retângulo: left, top, right, bottom, paint
        // canvas.drawRect(0f, 0f,MAX_PRINT_WIDTH,height,paint);
        canvas.drawRect(
                0f,
                topHeight,
                MAX_PRINT_WIDTH,
                topHeight + lineHeight,
                paint
        );

        return bitmap;
    }

    private Bitmap createLineSpace(){
        int height = 15;

        Bitmap bitmap = Bitmap.createBitmap(MAX_PRINT_WIDTH,height,Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);

        Paint paint = new Paint();
        paint.setColor(Color.TRANSPARENT);
        paint.setStyle(Paint.Style.FILL);

        // Retângulo: left, top, right, bottom, paint
        canvas.drawRect(0f, 0f,MAX_PRINT_WIDTH,height,paint);

        return bitmap;
    }

    @Override
    public void onPrinterError(PrinterError printerError){
        Log.e("Impressora", "Erro de impressora: " + printerError.getCause());
    }

    @Override
    public void onPrinterSuccessful(int printerRequestId){
        Log.d("Impressora", "Impressão realizada com sucesso. ID: " + printerRequestId);
    }
}

