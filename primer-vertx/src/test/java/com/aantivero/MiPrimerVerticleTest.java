package com.aantivero;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * Clase de Test para el primer Verticle utilizando JUnit y vertx-unit
 * Vertx-unit no ayuda a testear interacciones asincronicas
 * Created by alejandro on 20/11/2016.
 */
@RunWith(VertxUnitRunner.class)
public class MiPrimerVerticleTest {

    private Vertx vertx;
    private int port;

    @Before
    public void setUp(TestContext context) throws IOException {
        //1 crear una instancia de Vertx y deployar nuestro Verticle
        //2 el TestContext no ayuda al control asincronico de nuestros tests
        vertx = Vertx.vertx();

        ServerSocket socket = new ServerSocket(0);//la configuracion del puerto del server es random
        port = socket.getLocalPort();
        socket.close();

        DeploymentOptions deploymentOptions = new DeploymentOptions()//utiliza json como medio de configuracion
                .setConfig(new JsonObject()
                        .put("http.port", port)
                        .put("url", "jdbc:hsqldb:mem:test?shutdown=true")//configuracion bbdd para test en memoria
                        .put("driver_class","org.hsqldb.jdbcDriver"));

        vertx.deployVerticle(MiPrimerVerticle.class.getName(),
                deploymentOptions,
                context.asyncAssertSuccess());//3 nos indica que el contexto fue deployado
        //falla si el verticle no inicia correctamente ademas espera a que el verticle complete su secuencia de inicio
        //en el verticle lo llamamos future.complete(), espera a que este metodo sea llamado
        //en caso de falla el test falla
    }

    @After
    public void tearDown(TestContext context) {
        //4 finaliza la instancia de vertx
        vertx.close();
    }

    @Test
    public void testMiAplicacion(TestContext context) {
        //5 el test genera un request a la aplicacion y chequea el resultado
        //6 la emision del request y el resultado es asincronico
        // el TestContext sirve para esto
        final Async async = context.async(); //handler asincronico

        vertx.createHttpClient().getNow(port, "localhost", "/", //el getNow = get(...).end()
                httpClientResponse -> { //el response es un lambda
                    httpClientResponse.handler(body -> { //el response body es otro lambda
                        //el body es un buffer object
                        context.assertTrue(body.toString().contains("Mi Primer Aplicacion"));
                        async.complete();
                    });
                });
    }

    @Test
    public void testIndexStaticPage(TestContext context) {
        final Async async = context.async();
        vertx.createHttpClient().getNow(port, "localhost", "/assets/index.html",
                response -> {
                    context.assertEquals(response.statusCode(), 200);
                    context.assertTrue(response.headers().get("content-type").contains("text/html"));
                    response.bodyHandler(body -> {
                       context.assertTrue(body.toString().contains("Instrumentos"));
                        async.complete();
                    });
                });
    }

    @Test
    public void testAgregarInstrumento(TestContext context) {
        final Async async = context.async();
        final String json = Json.encodePrettily(new Instrumento("Test01", "Descripcion Test 01"));
        final String length = Integer.toString(json.length());
        vertx.createHttpClient().post(port, "localhost", "/api/instrumentos")
                .putHeader("content-type", "application/json")
                .putHeader("content-length", length)
                .handler(response -> {
                    context.assertEquals(response.statusCode(), 201);
                    context.assertTrue(response.headers().get("content-type").contains("application/json"));
                    response.bodyHandler(body -> {
                       final Instrumento instrumento = Json.decodeValue(body.toString(), Instrumento.class);
                        context.assertEquals(instrumento.getCodigo(), "Test01");
                        context.assertEquals(instrumento.getDescripcion(), "Descripcion Test 01");
                        context.assertNotNull(instrumento.getId());
                        async.complete();
                    });
                })
                .write(json)
                .end();
    }
}
