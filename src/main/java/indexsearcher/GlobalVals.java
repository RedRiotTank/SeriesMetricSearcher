package indexsearcher;

// clase para poder acceder a la misma instancia del indexSearch desde distintas clases
public class GlobalVals {
    private static String rutaIndex = "";

    public static String getRutaIndex() {
        return rutaIndex;
    }

    public static void setRutaIndex(String rutaIndex) {
        GlobalVals.rutaIndex = rutaIndex;
    }
}
