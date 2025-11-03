package com.projeto.totemserver.enums;

public enum Transaction {
        CAMPO_COMPROVANTE_CLIENTE(121),
        CAMPO_COMPROVANTE_ESTAB(122),
        CAMPO_NSU(134),
        CAMPO_ADM(500);

        private final int valor;

        Transaction(int valor) {
            this.valor = valor;
        }

        public int getValor() {
            return valor;
        }
}
