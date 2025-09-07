package org.tcc.monitor.jrf.utils;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

public class DecimalUtils {

    private static final DecimalFormat FORMATO_MOEDA_PTBR;

    static {
        DecimalFormatSymbols simbolos = new DecimalFormatSymbols(new Locale("pt", "BR"));
        simbolos.setDecimalSeparator(',');
        simbolos.setGroupingSeparator('\0');

        FORMATO_MOEDA_PTBR = new DecimalFormat("0.00", simbolos);
    }

    private DecimalUtils() {
    }

    public static String formatar(BigDecimal valor) {
        if (valor == null) {
            return null;
        }

        return FORMATO_MOEDA_PTBR.format(valor);
    }

    public static String formatar(double valor) {
        return FORMATO_MOEDA_PTBR.format(valor);
    }

    public static String formatar(float valor) {
        return FORMATO_MOEDA_PTBR.format(valor);
    }
}
