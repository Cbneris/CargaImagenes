package com.cbns.imagenes.util;

import java.io.File;

public class DirectorioUtil {
	public static boolean validaDirectorio(String ruta) {
        File directorio = new File(ruta);
        return directorio.exists() || directorio.mkdirs();
    }
}
