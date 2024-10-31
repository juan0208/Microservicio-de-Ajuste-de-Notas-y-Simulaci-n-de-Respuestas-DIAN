package com.dema.prueba_2.utils.enums;

/**
 * Enumeración que define las etiquetas específicas que deben copiarse durante el procesamiento XML.
 * Cada constante de la enumeración representa una etiqueta con su valor correspondiente en el documento XML.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */

public enum CopyTags {
    ID("cbc:ID"),
    UUID("cbc:UUID"),
    IssueDate("cbc:IssueDate");

    private final String value;

    /**
     * Constructor para inicializar la constante con el valor correspondiente.
     *
     * @param value Valor de la etiqueta en el XML.
     */

    CopyTags(String value) {
        this.value = value;
    }

    /**
     * Obtiene el valor de la etiqueta XML asociada con la constante.
     *
     * @return Valor de la etiqueta en formato de cadena.
     */

    public String getValue() {
        return value;
    }
}
