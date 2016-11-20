package com.aantivero;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;

/**
 * Primer aplicacion vertx.
 * 1 En Vertx un Verticle es un componente
 * Created by alejandro on 20/11/2016.
 */
public class MiPrimerVerticle extends AbstractVerticle{

    //3 este método es llamado cuando el verticle es deployado
    //5 el Future se utiliza para indicar si fue completado o no
    @Override
    public void start(Future<Void> future) {
        //2 al extender de AbstractVerticle ya tengo la propiedad vertx
        vertx
                .createHttpServer() //6 se crea un HTTP Server
                .requestHandler(r -> { //7 se le incorpora un handler que en este caso sera un lambda
                    r.response().end("<h1>Mi Primer Aplicacion con Vert.x</h1>"); //8 lambda qe se ejecuta ante cada request
                })
                .listen(8080, result -> { //9 se configura el puerto de escucha
                    if (result.succeeded()) { //10 lambda de resultado
                        future.complete();
                    } else {
                        future.fail(result.cause());
                    }
                });
    }

    //4 podemos implementar el método close() pero no es muy recomendable
    //por el momento dejo que lo haga vertx
}
