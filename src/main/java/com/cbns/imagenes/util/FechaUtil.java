package com.cbns.imagenes.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class FechaUtil {
    public static String normalizarFecha(String fecha) {
        if (fecha.matches("\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}")) {
            return fecha;
        }

        if (fecha.matches("\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}")) {
            try {
                return new SimpleDateFormat("yyyy-MM-dd_HH.mm.ss")
                        .format(new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").parse(fecha));
            } catch (ParseException e) {
                return "Fecha EXIF inválida";
            }
        }

        if (fecha.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*")) {
            try {
                Instant instant = Instant.parse(fecha);
                LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
                return DateTimeFormatter.ofPattern("yyyy-MM-dd_HH.mm.ss").format(ldt);
            } catch (DateTimeParseException e) {
                return "Fecha ISO inválida";
            }
        }

        return "Formato desconocido: " + fecha;
    }
}