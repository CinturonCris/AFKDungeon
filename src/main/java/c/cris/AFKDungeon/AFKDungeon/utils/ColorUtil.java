package c.cris.AFKDungeon.AFKDungeon.utils;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ColorUtil {

    /**
     * Reformatea los códigos RGB en el formato nativo de Spigot.
     * @param message Mensaje con códigos de color RGB
     * @return Mensaje reformateado
     */
    public static String reformatRGB(String message) {
        String rgbReformatted = message.replaceAll("(?i)\\&(x|#)([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])", "&x&$2&$3&$4&$5&$6&$7");

        return ChatColor.translateAlternateColorCodes('&', rgbReformatted);
    }

    /**
     * Reemplaza los códigos hexadecimales en el mensaje con códigos de color de Minecraft.
     * @param message Mensaje con códigos hexadecimales
     * @return Mensaje con códigos de color de Minecraft
     */
    public static String approximateHexCodes(String message) {
        message = message.replaceAll("(?i)(\\&|§)x(\\&|§)([0-9A-F])(\\&|§)([0-9A-F])(\\&|§)([0-9A-F])(\\&|§)([0-9A-F])(\\&|§)([0-9A-F])(\\&|§)([0-9A-F])", "&#$3$5$7$9$11$13");

        List<String> allMatches = new ArrayList<>();
        Matcher m = Pattern.compile("(?i)\\&(x|#)([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])([0-9A-F])").matcher(message);
        while (m.find()) {
            allMatches.add(m.group());
        }

        for (String match : allMatches) {
            String hexOnly = match.split("#")[1];
            String minecraftCode = hexToMinecraft(hexOnly);
            message = message.replace(match, "§" + minecraftCode);
        }

        return message;
    }

    /**
     * Convierte un código de color hexadecimal en el formato más cercano de Minecraft.
     * @param hex Código hexadecimal (6 dígitos)
     * @return Código de color en formato Minecraft
     */
    public static String hexToMinecraft(String hex) {
        String rcode = hex.substring(0, 2);
        String gcode = hex.substring(2, 4);
        String bcode = hex.substring(4, 6);

        int rint = Integer.parseInt(rcode, 16);
        int gint = Integer.parseInt(gcode, 16);
        int bint = Integer.parseInt(bcode, 16);

        String[] cga = {"000000", "0000aa", "00aa00", "00aaaa", "aa0000", "aa00aa", "ffaa00", "aaaaaa", "555555", "5555ff", "55ff55", "55ffff", "ff5555", "ff55ff", "ffff55", "ffffff"};

        int diff = Integer.MAX_VALUE;
        int best = -1;

        for (int i = 0; i < 16; i++) {
            String current = cga[i];
            String rcode2 = current.substring(0, 2);
            String gcode2 = current.substring(2, 4);
            String bcode2 = current.substring(4, 6);

            int rint2 = Integer.parseInt(rcode2, 16);
            int gint2 = Integer.parseInt(gcode2, 16);
            int bint2 = Integer.parseInt(bcode2, 16);

            int val = Math.abs(rint - rint2) + Math.abs(gint - gint2) + Math.abs(bint - bint2);

            if (val < diff) {
                best = i;
                diff = val;
            }
        }

        return Integer.toHexString(best);
    }

    /**
     * Concatena los argumentos para obtener el mensaje como una cadena.
     * @param args Los argumentos del comando
     * @param start El índice de inicio del mensaje (inclusive)
     * @param end El índice de fin del mensaje (inclusive)
     * @return El mensaje concatenado
     */
    public static String getMessageFromArgs(String[] args, int start, int end) {
        StringBuilder message = new StringBuilder();
        for (int i = start; i <= end && i < args.length; i++) {
            if (i > start) {
                message.append(" ");
            }
            message.append(args[i]);
        }
        return message.toString();
    }

    /**
     * Concatena los argumentos para obtener el mensaje como una cadena.
     * @param args Los argumentos del comando
     * @param start El índice de inicio del mensaje (inclusive)
     * @return El mensaje concatenado
     */
    public static String getMessageFromArgs(String[] args, int start) {
        return getMessageFromArgs(args, start, args.length - 1);
    }

    /**
     * Concatena los argumentos para obtener el mensaje como una cadena.
     * @param args Los argumentos del comando
     * @return El mensaje concatenado
     */
    public static String getMessageFromArgs(String[] args) {
        return getMessageFromArgs(args, 0);
    }

    /**
     * Convierte una colección de cadenas en una sola cadena.
     * @param collection La colección de cadenas
     * @return La cadena concatenada
     */
    public static String getStringFromCollection(Collection<String> collection) {
        return String.join(" ", collection);
    }

    /**
     * Verifica si el servidor soporta colores RGB.
     * @return true si el servidor soporta colores RGB, false de lo contrario
     */
    public static boolean supportsRGB() {
        // Implementar lógica específica para verificar el soporte de colores RGB
        return Bukkit.getVersion().contains("1.20") || Bukkit.getVersion().contains("1.21"); // Ejemplo para versiones específicas
    }
}
