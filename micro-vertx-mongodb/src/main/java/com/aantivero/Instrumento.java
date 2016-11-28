package com.aantivero;

import io.vertx.core.json.JsonObject;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objeto de Dominio
 * Created by alejandro on 20/11/2016.
 */
public class Instrumento {

    private final int id;

    private String codigo;

    private String descripcion;

    public Instrumento(String codigo, String descripcion) {
        this.id = -1;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Instrumento(Integer id, String codigo, String descripcion) {
        this.id = id;
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Instrumento(JsonObject json) {
        this.codigo = json.getString("CODIGO");
        this.descripcion = json.getString("DESCRIPCION");
        this.id = json.getInteger("ID");
    }

    public Instrumento() {
        this.id = -1;
    }

    public int getId() {
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
}
