package com.needlos.common.correo;

import java.time.Year;
import java.util.List;

/**
 * Plantilla HTML de los correos transaccionales: una tarjeta blanca con el
 * mismo azul de marca del borde y los botones del login (`#022859`), para que
 * cualquier correo se reconozca de un vistazo como de NeedlOS.
 *
 * Usa solo tablas y estilos en linea (los clientes de correo no soportan CSS
 * moderno de forma confiable) y una pila de fuentes del sistema: las fuentes
 * web (Inter) no cargan de forma confiable dentro de un correo.
 */
public final class PlantillaCorreo {

    private static final String AZUL = "#022859";
    private static final String TEXTO = "#1d1d1f";
    private static final String TEXTO_SUAVE = "#6e6e73";
    private static final String TEXTO_PIE = "#9a9a9f";
    private static final String FONDO = "#f5f5f7";
    private static final String BORDE_SUAVE = "rgba(2, 40, 89, 0.12)";
    private static final String BORDE_TARJETA = "rgba(2, 40, 89, 0.22)";
    private static final String FUENTE =
            "-apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";

    private PlantillaCorreo() {
    }

    /** Boton principal del correo (p. ej. "Restablecer contraseña"). */
    public record Boton(String texto, String url) {
    }

    /** Codigo de un solo uso mostrado en grande (p. ej. verificacion de correo). */
    public record Codigo(String valor) {
    }

    private static final String NOTA_SEGURIDAD = "Si no fuiste tú, ignora este mensaje: no se hizo ningún cambio en tu cuenta.";

    /** Arma el HTML completo: titulo, parrafos (se escapan) y un boton opcional. */
    public static String html(String titulo, List<String> parrafos, Boton boton) {
        return base(titulo, parrafos, boton == null ? "" : botonHtml(boton), NOTA_SEGURIDAD);
    }

    /** Variante con un codigo grande en vez de un boton (p. ej. "123456"). */
    public static String htmlConCodigo(String titulo, List<String> parrafos, Codigo codigo) {
        return base(titulo, parrafos, codigoHtml(codigo), NOTA_SEGURIDAD);
    }

    /** Variante sin boton ni codigo, con una nota al pie propia (p. ej. el correo de bienvenida). */
    public static String html(String titulo, List<String> parrafos, Boton boton, String notaPie) {
        return base(titulo, parrafos, boton == null ? "" : botonHtml(boton), notaPie);
    }

    private static String base(String titulo, List<String> parrafos, String bloqueExtra, String notaPie) {
        StringBuilder cuerpo = new StringBuilder();
        for (String parrafo : parrafos) {
            cuerpo.append("<p style=\"margin:0 0 16px;font-size:15px;line-height:1.5;color:")
                    .append(TEXTO).append(";\">").append(escapar(parrafo)).append("</p>\n");
        }

        return BASE
                .replace("{{fuente}}", FUENTE)
                .replace("{{fondo}}", FONDO)
                .replace("{{bordeTarjeta}}", BORDE_TARJETA)
                .replace("{{azul}}", AZUL)
                .replace("{{textoSuave}}", TEXTO_SUAVE)
                .replace("{{bordeSuave}}", BORDE_SUAVE)
                .replace("{{texto}}", TEXTO)
                .replace("{{titulo}}", escapar(titulo))
                .replace("{{parrafos}}", cuerpo.toString())
                .replace("{{extra}}", bloqueExtra)
                .replace("{{notaPie}}", escapar(notaPie))
                .replace("{{textoPie}}", TEXTO_PIE)
                .replace("{{anio}}", String.valueOf(Year.now().getValue()));
    }

    private static String botonHtml(Boton boton) {
        String url = escapar(boton.url());
        return BOTON
                .replace("{{azul}}", AZUL)
                .replace("{{textoPie}}", TEXTO_PIE)
                .replace("{{url}}", url)
                .replace("{{textoBoton}}", escapar(boton.texto()));
    }

    private static String codigoHtml(Codigo codigo) {
        // Sin espacios ni separadores dentro del valor: al copiarlo y pegarlo debe quedar
        // exactamente igual. La legibilidad la da el letter-spacing (solo visual).
        return CODIGO
                .replace("{{azul}}", AZUL)
                .replace("{{bordeSuave}}", BORDE_SUAVE)
                .replace("{{codigo}}", escapar(codigo.valor()));
    }

    /** Escapado minimo: nombres y correos de cuentas no deben romper el marcado. */
    private static String escapar(String texto) {
        return texto
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    private static final String BOTON = """
            <tr><td style="padding:8px 40px 8px;" align="center">
              <a href="{{url}}" style="display:inline-block;background:{{azul}};color:#ffffff;text-decoration:none;font-weight:600;font-size:15px;padding:14px 32px;border-radius:14px;">{{textoBoton}}</a>
            </td></tr>
            <tr><td style="padding:0 40px 16px;">
              <p style="margin:0;font-size:12px;color:{{textoPie}};word-break:break-all;">
                Si el boton no funciona, copia y pega este enlace en tu navegador:<br>
                <a href="{{url}}" style="color:{{azul}};">{{url}}</a>
              </p>
            </td></tr>
            """;

    private static final String CODIGO = """
            <tr><td style="padding:8px 40px 24px;" align="center">
              <div style="display:inline-block;background:#f5f5f7;border:1px solid {{bordeSuave}};border-radius:14px;padding:16px 28px;font-size:32px;font-weight:700;letter-spacing:0.08em;color:{{azul}};font-family:ui-monospace,SFMono-Regular,Menlo,Consolas,monospace;user-select:all;-webkit-user-select:all;">{{codigo}}</div>
            </td></tr>
            """;

    private static final String BASE = """
            <!doctype html>
            <html lang="es">
            <head>
              <meta charset="utf-8">
              <meta name="viewport" content="width=device-width, initial-scale=1">
            </head>
            <body style="margin:0;padding:0;background:{{fondo}};font-family:{{fuente}};">
              <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background:{{fondo}};padding:40px 16px;">
                <tr><td align="center">
                  <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="max-width:480px;background:#ffffff;border:1px solid {{bordeTarjeta}};border-radius:20px;">
                    <tr><td style="padding:40px 40px 24px;text-align:center;">
                      <div style="font-size:24px;font-weight:700;letter-spacing:-0.02em;color:{{azul}};">NeedlOS</div>
                      <div style="font-size:11px;letter-spacing:0.08em;text-transform:uppercase;color:{{textoSuave}};margin-top:4px;">Tailor Software</div>
                    </td></tr>
                    <tr><td style="padding:0 40px;"><div style="border-top:1px solid {{bordeSuave}};line-height:0;">&nbsp;</div></td></tr>
                    <tr><td style="padding:32px 40px 8px;">
                      <h1 style="margin:0 0 16px;font-size:20px;font-weight:700;color:{{texto}};letter-spacing:-0.01em;">{{titulo}}</h1>
                      {{parrafos}}
                    </td></tr>
                    {{extra}}
                    <tr><td style="padding:8px 40px 40px;">
                      <p style="margin:0;font-size:13px;color:{{textoSuave}};">{{notaPie}}</p>
                    </td></tr>
                  </table>
                  <p style="margin:24px 0 0;font-size:12px;color:{{textoPie}};">&copy; {{anio}} NeedlOS &middot; Software para sastrerías</p>
                </td></tr>
              </table>
            </body>
            </html>
            """;
}
