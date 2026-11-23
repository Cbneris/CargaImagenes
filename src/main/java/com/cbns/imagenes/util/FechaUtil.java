package com.cbns.imagenes.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import com.cbns.imagenes.constantes.ConstantesTecnicas;
import com.cbns.imagenes.constantes.MensajesSistema;

public class FechaUtil {
    public static String normalizarFecha(String fecha) {
        if (fecha.matches(ConstantesTecnicas.PATRON_FECHA_NORMALIZADA)) {
            return fecha;
        }

        if (fecha.matches(ConstantesTecnicas.PATRON_FECHA_EXIF)) {
            try {
                return new SimpleDateFormat(ConstantesTecnicas.FORMATO_FECHA_NORMALIZADA)
                        .format(new SimpleDateFormat(ConstantesTecnicas.FORMATO_FECHA_EXIF).parse(fecha));
            } catch (ParseException e) {
                return MensajesSistema.ERROR_FECHA_EXIF_INVALIDA;
            }
        }

        if (fecha.matches(ConstantesTecnicas.PATRON_FECHA_ISO)) {
            try {
                Instant instant = Instant.parse(fecha);
                LocalDateTime ldt = instant.atZone(ZoneId.systemDefault()).toLocalDateTime();
                return DateTimeFormatter.ofPattern(ConstantesTecnicas.FORMATO_FECHA_NORMALIZADA).format(ldt);
            } catch (DateTimeParseException e) {
                return MensajesSistema.ERROR_FECHA_ISO_INVALIDA;
            }
        }

        return MensajesSistema.ERROR_FORMATO_FECHA_DESCONOCIDO.formatted(fecha);
    }
}