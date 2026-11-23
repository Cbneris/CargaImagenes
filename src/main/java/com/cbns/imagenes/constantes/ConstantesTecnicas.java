package com.cbns.imagenes.constantes;

public final class ConstantesTecnicas {

    private ConstantesTecnicas() {
    }

    public static final String DIRECTORIO_HOMES = "/volume1/homes";
    public static final String PROPIEDAD_DIRECTORIO_UPLOAD = "${upload.dir:" + DIRECTORIO_HOMES + "}";
    public static final String DIRECTORIO_FOTOS = "Photos";
    public static final String PREFIJO_MIME_VIDEO = "video/";
    public static final String IP_LOCAL_FALLBACK = "127.0.0.1";
    public static final String SEPARADOR_SUFIJO_DUPLICADO = "-";
    public static final String DIRECTORIO_ACTUAL = ".";
    public static final String DIRECTORIO_PADRE = "..";
    public static final String CADENA_VACIA = "";

    public static final String PATRON_FECHA_NORMALIZADA =
            "\\d{4}-\\d{2}-\\d{2}_\\d{2}\\.\\d{2}\\.\\d{2}";
    public static final String PATRON_FECHA_EXIF =
            "\\d{4}:\\d{2}:\\d{2} \\d{2}:\\d{2}:\\d{2}";
    public static final String PATRON_FECHA_ISO =
            "\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*";
    public static final String PATRON_PROPIETARIO = "[A-Za-z0-9._-]+";
    public static final String FORMATO_FECHA_NORMALIZADA = "yyyy-MM-dd_HH.mm.ss";
    public static final String FORMATO_FECHA_EXIF = "yyyy:MM:dd HH:mm:ss";

    public static final String ORIGEN_CORS_FRONTEND = "http://192.168.1.13:5174";
    public static final String RUTA_CONTROLADOR_CARGA = "/cargaImagenes";
    public static final String RUTA_OWNERS = "/owners";
    public static final String RUTA_UPLOAD = "/upload";
    public static final String RUTA_UPLOAD_COMPLETE = "/upload/complete";
    public static final String PARAMETRO_IMAGES = "images";
    public static final String PARAMETRO_MODIFIED_DATE = "modifiedDate";
    public static final String PARAMETRO_OWNER = "owner";
    public static final String PARAMETRO_UPLOADED_COUNT = "uploadedCount";
}
