package com.odoo.core.rpc.listeners;

/**
 * Created by cracker
 * Created on 1/10/22.
 */

public class JsonObjectResponse {
    private String response = null;
    private Object object;

    public void setObject(Object obj) {
        object = obj;
    }

    public Object getObject() {
        return object;
    }

    public void setResponse(String response) {
        this.response = response;
    }

    public String get() {
        return response;
    }

    @Override
    public String toString() {
        return "OdooSyncResponse{" +
                "response=" + response +
                ", object=" + object +
                '}';
    }
}
