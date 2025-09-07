package org.tcc.monitor.jrf.utils;

import java.time.Duration;

public class DurationUtils {

    public static String format(Duration duration) {
        long horas = duration.toHours();
        long minutos = duration.toMinutes() % 60;
        long segundos = duration.getSeconds() % 60;
        long milissegundos = duration.toMillis() % 1000;

        return String.format("%02d:%02d:%02d.%03d", horas, minutos, segundos, milissegundos);
    }
}