package com.dema.prueba_2.models.responses;

public class ServerResponse<T> {
    public T id;

    public ServerResponse(T id) {
        this.id = id;
    }
}