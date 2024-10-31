package com.dema.prueba_2.models.responses;

/**
 * Representa la respuesta de la DIAN, indicando si la respuesta fue exitosa o no.
 *
 * @since 29 de octubre de 2024
 * @autor Juan Alejandro Londoño Lopez
 */

public class DIANResponse {
    private boolean dianResponse;

    public boolean isDianResponse() {
        return dianResponse;
    }

    public boolean getDianResponse(){
        return dianResponse;
    }

    public void setDianResponse(boolean dianResponse) {
        this.dianResponse = dianResponse;
    }
}