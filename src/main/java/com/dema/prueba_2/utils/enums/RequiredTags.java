package com.dema.prueba_2.utils.enums;

//public enum RequiredTags {
//    AccountingSupplierParty,
//    AccountingCustomerParty,
//    PaymentMeans,
//    TaxTotal,
//    LegalMonetaryTotal
//
//}


/**
 * Enumeración que define las etiquetas XML requeridas para el procesamiento de documentos,
 * asignando cada etiqueta a su nombre completo con prefijo.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */

public enum RequiredTags {
    AccountingSupplierParty("cac:AccountingSupplierParty"),
    AccountingCustomerParty("cac:AccountingCustomerParty"),
    PaymentMeans("cac:PaymentMeans"),
    TaxTotal("cac:TaxTotal"),
    LegalMonetaryTotal("cac:LegalMonetaryTotal");

    private final String tagName;

    /**
     * Constructor que asigna el nombre de la etiqueta al campo {@code tagName}.
     *
     * @param tagName Nombre de la etiqueta XML, incluyendo su prefijo de espacio de nombres.
     */

    RequiredTags(String tagName) {
        this.tagName = tagName;
    }

    /**
     * Obtiene el nombre de la etiqueta XML con el prefijo.
     *
     * @return El nombre de la etiqueta XML completa con su prefijo.
     */

    public String getTagName() {
        return tagName;
    }
}

