package com.projeto.gertecserver;

import static com.projeto.gertecserver.MyWebSocketServer.cupom;

import static br.com.gertec.easylayer.printer.Alignment.CENTER;

import android.content.Context;
import android.graphics.Bitmap;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import br.com.gertec.easylayer.printer.Alignment;
import br.com.gertec.easylayer.printer.CutType;
import br.com.gertec.easylayer.printer.OrientationType;
import br.com.gertec.easylayer.printer.Printer;
import br.com.gertec.easylayer.printer.PrinterError;
import br.com.gertec.easylayer.printer.PrinterException;
import br.com.gertec.easylayer.printer.PrinterUtils;
import br.com.gertec.easylayer.printer.Receipt;
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

        printer.setPrinterOrientation(OrientationType.DEFAULT);
    }

    public void imprimirComprovante(Bitmap image, JSONArray items, JSONObject parameters){
//        if(items != null){
//            Receipt cupom = new Receipt();
//            cupom.setLogoReceiptHead(image);
//            cupom.setNameFantasy(parameters.optString("nameFantasy"));
//            cupom.setReasonSocial(parameters.optString("reasonSocial"));
//            cupom.setAdressRef(parameters.optString("adressRef"));
//            cupom.setCpfCnpjLocal(parameters.optString("cpfCnpjLocal"));
//            cupom.setPaymentType(parameters.optString("paymentType"));
//            cupom.setCardNumber(null);
//            cupom.setIENumber(parameters.optString("IENumber"));
//            cupom.setIMNumer(parameters.optString("IMNumber"));
//            cupom.setNumberExtract(parameters.optString("numberExtract"));
//            cupom.setHeadCoupon(parameters.optString("headCoupon"));
//            cupom.setNameUser(parameters.optString("nameUser"));
//            cupom.setCpfCnpjUser(parameters.optString("cpfCnpjUser"));
//            cupom.setValueTotal(parameters.optString("valueTotal"));
//            cupom.setValuePay("");
//            cupom.setValueReturn("");
//            cupom.setObservText(parameters.optString("observText"));
//            cupom.setNumberSat("");
//            cupom.setDateHour(new SimpleDateFormat("ddMMyyyyHHmmss").format(new Date()));
//            cupom.setCodeNumber1(parameters.optString("codeNumber1"));
//            cupom.setCodeNumber2(parameters.optString("codeNumber2"));
//            cupom.setImageCodebar1(image);
//            cupom.setImageCodebar2(image);
//            cupom.setImageQrCode(image);
//
//            ArrayList<String> listItens = new ArrayList<>();
//            ArrayList<String> listItensValue = new ArrayList<>();
//
//            try {
//                for(int i = 0; i < items.length(); i++){
//                    JSONObject item = items.getJSONObject(i);
//
//                    String titulo = item.getString("ite_titulo");
//                    double preco = item.getDouble("ite_preco");
//                    String quantidade = String.valueOf(item.get("ite_quantidade"));
//
//                    String linhaPrincipal = quantidade + "x " + titulo;
//                    listItens.add(linhaPrincipal);
//                    listItensValue.add(String.format(Locale.US, "%.2f", preco));
//
//                    if(item.has("perguntas")){
//                        JSONArray perguntas = item.getJSONArray("perguntas");
//                        for(int j = 0; j < perguntas.length(); j++){
//                            JSONObject pergunta = perguntas.getJSONObject(j);
//                            String label = pergunta.getString("per_label");
//
//                            String resposta = "";
//                            if(pergunta.has("resposta") && !pergunta.isNull("resposta")){
//                                JSONObject respostaObj = pergunta.getJSONObject("resposta");
//                                resposta = respostaObj.getString("res_titulo");
//                            }
//
//                            listItens.add("   - " + label + ": " + resposta);
//                            listItensValue.add(""); // valor vazio para manter alinhamento
//                        }
//                    }
//                }
//
//                cupom.setListItens(listItens);
//                cupom.setListValueItens(listItensValue);
//            } catch(JSONException e){}
//
//            printer.printXml(cupom);
//            printer.scrollPaper(3);
//            printer.cutPaper(CutType.PAPER_PARTIAL_CUT);
//        }


        try {
            //Impressão imagem
            Bitmap monochromaticBitmap = printerUtils.toMonochromatic(image,0.5);
            printer.printImageAutoResize(monochromaticBitmap);

            // Cabeçalho
            printFormat(printer,parameters.optString("nameFantasy"));
            printFormat(printer,parameters.optString("reasonSocial"));
            printFormat(printer,parameters.optString("adressRef"));
            printFormat(printer, "CNPJ: " + parameters.optString("cpfCnpjLocal"));
            printFormat(printer, "Pagamento: " + parameters.optString("paymentType"));
            printFormat(printer, "Razão Social/Nome: " + parameters.optString("nameUser"));
            printFormat(printer,"--------------------------------");

            // Itens
            try {
                final int TOTAL_WIDTH = 42; // largura total da imp

                for(int i = 0; i < items.length(); i++){
                    JSONObject item = items.getJSONObject(i);

                    String titulo = item.getString("ite_titulo");
                    String preco = item.getString("ite_preco");
                    String quantidade = item.getString("ite_quantidade");

                    printFormat(printer,formatLine(quantidade + "x " + titulo,preco, 42));

                    if(item.has("perguntas")){
                        JSONArray perguntas = item.getJSONArray("perguntas");
                        for(int j = 0; j < perguntas.length(); j++){
                            JSONObject pergunta = perguntas.getJSONObject(j);
                            String label = pergunta.getString("per_label");

                            String resposta = "";
                            if(pergunta.has("resposta") && !pergunta.isNull("resposta")){
                                JSONObject respostaObj = pergunta.getJSONObject("resposta");
                                resposta = respostaObj.getString("res_titulo");
                            }

                            printFormat(printer,"   - " + label + ": " + resposta);
                        }
                    }

                }
            } catch(JSONException e){}

            // Separador
            printFormat(printer,"--------------------------------");
            printFormat(printer,formatLine("TOTAL",parameters.optString("valueTotal"), 42));
            printFormat(printer,"Obs: " + parameters.optString("observText"));
            printFormat(printer,new SimpleDateFormat("dd/MM/yyyy - HH:mm:ss").format(new Date()));
            printer.scrollPaper(3);

            printer.cutPaper(CutType.PAPER_PARTIAL_CUT);
        } catch(PrinterException e){}

//        try {
//             if(MyWebSocketServer.cupom.trim().isEmpty()){
//                MyWebSocketServer.cupom = "Lorem Ipsum is simply dummy text of th...";
//             }
//
//             // Divide por linha
//            String[] linhas = MyWebSocketServer.cupom.split("\n");
//            List<String> linhasList = Arrays.asList(linhas);
//            // Inverte
//            Collections.reverse(linhasList);
//
//            // Verifica o tamanho da maior linha
//            int maxLen = linhasList.stream().mapToInt(String::length).max().orElse(0);
//
//            // Define tamanho da fonte baseado na maior linha
//            int fontSize = maxLen <= 32 ? 30 :
//                            maxLen <= 36 ? 26 :
//                            maxLen <= 42 ? 22 :
//                            maxLen <= 48 ? 18 : 16;
//
//            // Configuração base de formatação
//            TextFormat format = new TextFormat();
//            format.setBold(true);
//            format.setUnderscore(false);
//            format.setFontSize(fontSize);
//            format.setAlignment(Alignment.LEFT);
//            format.setLineSpacing(4);
//
//            for(String linha : linhasList){
//                printer.printText(format,linha);
//            }
//
//            //Imprimir cupom
             // Receipt cupom = new Receipt();
//            // printer.printXml(cupom);
//
//            //Impressão imagem
//            Bitmap monochromaticBitmap = printerUtils.toMonochromatic(image,0.5);
//            // printer.printImageAutoResize(monochromaticBitmap);
//            // printer.scrollPaper(3);
//
//            printer.cutPaper(CutType.PAPER_PARTIAL_CUT);
//            MyWebSocketServer.cupom = "";
//        } catch(PrinterException e){}
    }

    private void printFormat(Printer p, String s) throws PrinterException {
        TextFormat textFormat = new TextFormat();
        textFormat.setBold(true);
        textFormat.setUnderscore(false);
        textFormat.setFontSize(20);
        textFormat.setLineSpacing(6);
        textFormat.setAlignment(CENTER);
        p.printText(textFormat,s);
    }

    private String formatLine(String var1, String var2, int totalWidth){
        int espacos = totalWidth - var1.length() - var2.length();
        if (espacos < 1) espacos = 1;
        StringBuilder sb = new StringBuilder();
        sb.append(var1);
        for(int i = 0; i < espacos; i++) sb.append(" ");
        sb.append(var2);
        return sb.toString();
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
