package org.tcc.monitor.jrf.utils;

import java.sql.Date;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public class DateTimeUtils {
    private static final String PADRAO = "ddMMyyyy";
    private static final DateTimeFormatter FORMATO_DATA = DateTimeFormatter.ofPattern(PADRAO);
    private static final ThreadLocal<SimpleDateFormat> FORMATO_TIMESTAMP = ThreadLocal.withInitial(() -> new SimpleDateFormat(PADRAO));

    private DateTimeUtils() {
    }

    public static String formatar(LocalDate data) {
        if (data == null) {
            return null;
        }

        return data.format(FORMATO_DATA);
    }

    public static LocalDate parse(String dataStr) throws ParseException {
        if (dataStr == null || dataStr.isEmpty()) {
            return null;
        }

        return LocalDate.parse(dataStr, FORMATO_DATA);
    }

    public static String formatar(Timestamp timestamp) {
        if (timestamp == null) {
            return null;
        }

        return FORMATO_TIMESTAMP.get().format(timestamp);
    }

    public static LocalDate getPrimerioDiaMes(LocalDate data) {
        if (data == null) {
            return null;
        }

        return data.withDayOfMonth(1);
    }

    public static LocalDate getUltimoDiaMes(LocalDate data) {
        if (data == null) {
            return null;
        }

        return YearMonth.from(data).atEndOfMonth();
    }

    public static LocalDate getProximoMes(LocalDate data) {
        if (data == null) {
            return null;
        }

        return data.plusMonths(1).atStartOfDay().toLocalDate();
    }

    public static Timestamp toTimestamp(LocalDate data) {
        if (data == null) {
            return null;
        }

        return Timestamp.valueOf(data.atStartOfDay());
    }

    public static LocalDate toLocalDate(Timestamp data) {
        if (data == null) {
            return null;
        }

        return data.toLocalDateTime().toLocalDate();
    }

    public static LocalDate toLocalDate(Date data) {
        if (data == null) {
            return null;
        }

        return data.toLocalDate();
    }
}
