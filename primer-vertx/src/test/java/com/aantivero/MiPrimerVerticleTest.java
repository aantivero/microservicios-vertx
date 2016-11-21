package com.aantivero;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
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
                .setConfig(new JsonObject().put("http.port", port));

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
}
