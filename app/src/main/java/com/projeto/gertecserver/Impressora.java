package com.projeto.gertecserver;

import static br.com.gertec.easylayer.printer.Alignment.CENTER;
import static br.com.gertec.easylayer.printer.Alignment.LEFT;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
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
import br.com.gertec.easylayer.printer.Receipt;
import br.com.gertec.easylayer.printer.TextFormat;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
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
    }

    // Apenas para testes
    public void imprimirComprovante(Bitmap image, JSONArray items, JSONObject parameters){}

    public void imprimirComprovante_(Bitmap image, JSONArray items, JSONObject parameters) throws PrinterException {
        try {
            // Logo do estabelecimento
            PrintConfig printConfig = new PrintConfig();
            printConfig.setWidth(100);
            printConfig.setHeight(100);
            printConfig.setAlignment(CENTER);
            printer.printImage(printConfig,image);
            printer.scrollPaper(1);

            // Separador
            PrintConfig lineConfig = new PrintConfig();
            lineConfig.setAlignment(Alignment.CENTER);
            printer.printImage(lineConfig,createLineSeparator());

            printFormat(parameters.optString("orderConsume"),LEFT,false);

            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            printFormat("Senha: ",CENTER,false);
            TextFormat textFormat = new TextFormat();
            textFormat.setBold(true);
            textFormat.setFontSize(50);
            textFormat.setLineSpacing(-5);
            textFormat.setAlignment(CENTER);
            printer.printText(textFormat,parameters.optString("orderRef"));

            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            printFormat(parameters.optString("nameFantasy"),LEFT,false);
            printFormat(parameters.optString("reasonSocial"),LEFT,false);
            printer.printImage(lineConfig,createLineSpace());
            printFormat(parameters.optString("adressRef"),LEFT,false);
            printer.printImage(lineConfig,createLineSpace());
            printFormat(parameters.optString("orderHash"),LEFT,false);

            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            // Itens
            try {
                printFormat("Itens", LEFT, false);
                printer.printImage(lineConfig,createLineSpace());

                for(int i = 0; i < items.length(); i++){
                    JSONObject item = items.getJSONObject(i);

                    String titulo = item.getString("ite_titulo");
                    Double preco = item.getDouble("ite_preco");
                    String quantidade = item.getString("ite_quantidade");

                    printFormat(formatLine(
                        (quantidade != null && !quantidade.isEmpty()) ? quantidade + "x " + titulo : titulo,
                        (preco != null && preco > 0) ? "R$ " + String.format("%.2f", preco) : ""
                    ),LEFT,false);

                    if(item.has("perguntas")){
                        JSONArray perguntas = item.getJSONArray("perguntas");

                        for(int j = 0; j < perguntas.length(); j++){
                            JSONObject pergunta = perguntas.getJSONObject(j);
                            String label = pergunta.getString("per_label");

                            if(pergunta.has("resposta") && !pergunta.isNull("resposta")){
                                JSONObject respostaObj = pergunta.getJSONObject("resposta");
                                String title = respostaObj.getString("res_titulo");

                                printFormat("- " + label + ": " + title,LEFT,false);
                            }

                            if(pergunta.has("respostas") && !pergunta.isNull("respostas")){
                                JSONArray respostas = pergunta.getJSONArray("respostas");

                                for(int k = 0; k < respostas.length(); k++){
                                    JSONObject respostaObj = respostas.getJSONObject(k);
                                    String title = respostaObj.getString("res_titulo");
                                    String qntd = respostaObj.getString("res_quantidade");

                                    printFormat((qntd != null && !qntd.isEmpty() && Integer.parseInt(qntd) > 1) ? "- " + qntd + "x " + title : "- " + title,LEFT,false);
                                }
                            }
                        }
                    }
                }
            } catch(JSONException e){}

            String obs = parameters.optString("observText");
            if(obs != null && !obs.isEmpty()){
                printFormat(obs, LEFT, false);
                printer.printImage(lineConfig,createLineSpace());
            }

            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            printFormat(parameters.optString("nameUser"),LEFT,false);
            printFormat(parameters.optString("cpfCnpjUser"),LEFT,false);

            // Separador
            printer.printImage(lineConfig,createLineSeparator());

            printFormat(parameters.optString("paymentType"),LEFT,false);
            printFormat(formatLine("Sub-total",parameters.optString("valueSubtotal")),LEFT,false);
            printFormat(formatLine("Desconto",parameters.optString("valueDiscount")),LEFT,false);
            printFormat(formatLine("Taxa pagto.",parameters.optString("valueFee")),LEFT,false);
            printFormat(formatLine("Total",parameters.optString("valueTotal")),LEFT,true);

            // Separador
            printer.printImage(lineConfig,createLineSeparator());
            printFormat(new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(new Date()),CENTER,false);

            printer.scrollPaper(1);
            printer.cutPaper(CutType.PAPER_PARTIAL_CUT);
        } catch(PrinterException e){}
    }

    private void printFormat(String s, Alignment alignment, Boolean bold) throws PrinterException{
        if(s != null && !s.isEmpty()){
            final int MAX_CHARS = 32;

            List<String> linhas = new ArrayList<>();

            // Tratativa para quebra de linha
            if(s.length() > MAX_CHARS){
                // Evitando cortar palavras
                String[] palavras = s.split(" ");
                StringBuilder linhaAtual = new StringBuilder();

                for(String palavra : palavras){
                    // Se adicionar essa palavra passar do limite, salva linha e começa outra
                    if(linhaAtual.length() > 0 && (linhaAtual.length() + 1 + palavra.length()) > MAX_CHARS){
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
                textFormat.setFontSize(20);
                textFormat.setLineSpacing(-5);
                textFormat.setAlignment(alignment);

                printer.printText(textFormat,linha);
            }
        }
    }

    private String formatLine(String var1,String var2){
        int width = 32;

        if(var1.length() + var2.length() >= width){
            return var1 + " " + var2;
        }

        int spaces = width - var1.length() - var2.length();
        StringBuilder sb = new StringBuilder();
        sb.append(var1);
        for(int i = 0; i < spaces; i++){
            sb.append(" ");
        }
        sb.append(var2);
        return sb.toString();
    }

    private static Bitmap createLineSeparator(){
        int height = 2;

        // Bitmap mutável
        Bitmap bitmap = Bitmap.createBitmap(MAX_PRINT_WIDTH,height,Bitmap.Config.ARGB_8888);

        // Canvas para desenhar no Bitmap
        Canvas canvas = new Canvas(bitmap);

        // Paint para desenhar a linha
        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setStyle(Paint.Style.FILL);

        // Retângulo que representa a linha: left, top, right, bottom, paint
        canvas.drawRect(0f, 0f,MAX_PRINT_WIDTH,height,paint);

        return bitmap;
    }

    private static Bitmap createLineSpace(){
        int height = 15;

        // Bitmap mutável
        Bitmap bitmap = Bitmap.createBitmap(MAX_PRINT_WIDTH,height,Bitmap.Config.ARGB_8888);

        // Canvas para desenhar no Bitmap
        Canvas canvas = new Canvas(bitmap);

        // Paint para desenhar a linha
        Paint paint = new Paint();
        paint.setColor(Color.TRANSPARENT);
        paint.setStyle(Paint.Style.FILL);

        // Retângulo que representa a linha: left, top, right, bottom, paint
        canvas.drawRect(0f, 0f,MAX_PRINT_WIDTH,height,paint);

        return bitmap;
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

