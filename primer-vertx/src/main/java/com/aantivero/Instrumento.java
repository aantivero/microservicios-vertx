package com.aantivero;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Objeto de Dominio
 * Created by alejandro on 20/11/2016.
 */
public class Instrumento {

    private static final AtomicInteger COUNTER = new AtomicInteger();

    private final int id;

    private String codigo;

    private String descripcion;

    public Instrumento(String codigo, String descripcion) {
        this.id = COUNTER.getAndIncrement();
        this.codigo = codigo;
        this.descripcion = descripcion;
    }

    public Instrumento() {
        this.id = COUNTER.getAndIncrement();
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
