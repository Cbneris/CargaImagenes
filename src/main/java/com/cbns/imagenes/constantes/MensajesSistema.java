package com.cbns.imagenes.constantes;

public final class MensajesSistema {

    private MensajesSistema() {
    }

    public static final String ERROR_ARCHIVO_VACIO = "No se recibió ningún archivo.";
    public static final String ERROR_PROPIETARIO_INVALIDO = "El usuario no es válido.";
    public static final String ERROR_ARCHIVO_DUPLICADO = "El archivo %s ya existe y no se cargó.";
    public static final String LOG_ARCHIVO_DUPLICADO =
            "[DUPLICATE] - [{}] Se omitió {} porque ya existe el destino {}.";
    public static final String ERROR_PROCESAR_CARGA = "Error al procesar la carga de imagen";
    public static final String CARGA_EXITOSA = "Imagen cargada exitosamente.";
    public static final String ERROR_CANTIDAD_ARCHIVOS_NEGATIVA =
            "La cantidad de archivos no puede ser negativa.";
    public static final String RESUMEN_CARGA_REGISTRADO = "Resumen de carga registrado.";
    public static final String LOG_RESUMEN_CARGA = "[{}] Carga finalizada: {} archivo(s) cargado(s).";
    public static final String LOG_DIRECTORIO_ALMACENAMIENTO =
            "[UPLOAD] Directorio de almacenamiento para {}: {}";
    public static final String ERROR_DIRECTORIO_USUARIOS =
            "El directorio de usuarios no existe o no es accesible: %s";
    public static final String LOG_FECHA_MODIFICACION = "[FechaModificación]";
    public static final String LOG_FECHA_ORIGINAL = "[FechaOriginal]";
    public static final String ERROR_CREAR_DIRECTORIO_ANUAL =
            "No se pudo crear el directorio para el usuario %s y el año %s";
    public static final String LOG_ARCHIVO_RENOMBRADO = "[SUCCESS] - [{}] Renombrado {} -> {}";
    public static final String ERROR_RUTA_FUERA_VOLUMEN =
            "La ruta de almacenamiento está fuera del volumen configurado.";
    public static final String ERROR_FORMATO_FECHA = "La fecha no tiene el formato esperado: %s";
    public static final String ERROR_OBTENER_IP_LOCAL = "Error al obtener IP local";
    public static final String ERROR_FECHA_EXIF_INVALIDA = "Fecha EXIF inválida";
    public static final String ERROR_FECHA_ISO_INVALIDA = "Fecha ISO inválida";
    public static final String ERROR_FORMATO_FECHA_DESCONOCIDO = "Formato desconocido: %s";
}
