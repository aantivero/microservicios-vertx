package com.aantivero;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objeto de Dominio
 * Created by alejandro on 20/11/2016.
 */
public class Instrumento {

    private String id;

    private String codigo;

    private String descripcion;

    public Instrumento(String codigo, String descripcion) {
        this.id = "";
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Instrumento(String id, String codigo, String descripcion) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Instrumento(JsonObject json) {
        this.codigo = json.getString("codigo");
        this.descripcion = json.getString("descripcion");
        this.id = json.getString("_id");
    }

    public Instrumento() {
        this.id = "";
    }

    public String getId() {
        return id;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public void setCodigo(String codigo) {
        this.codigo = codigo;
    }

    public void setDescripcion(String descripcion) {
        this.descripcion = descripcion;
    }

    public Instrumento setId(String id) {
        this.id = id;
        return this;
    }

    public JsonObject toJson() {
        JsonObject json = new JsonObject()
                .put("codigo", codigo)
                .put("decripcion", descripcion);
        if (id != null && !id.isEmpty()) {
            json.put("_id", id);
        }
        return json;
    }
}
