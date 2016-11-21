package com.aantivero;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.ext.web.Router;

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
        //crear el objeto Router
        //Es el responsable de enviar las solicitudes HTTP al controlador correcto
        Router router = Router.router(vertx);

        //se realiza el Bind de '/' para que siga siendo compatible
        router.route("/").handler(routingContext -> { //lambda
            HttpServerResponse response = routingContext.response();
            response
                    .putHeader("content-type", "text/html")
                    .end("<h1>Mi Primer Aplicacion con Vert.x</h1>");
        });
        //2 al extender de AbstractVerticle ya tengo la propiedad vertx
        vertx
                .createHttpServer() //6 se crea un HTTP Server
                .requestHandler(router::accept) //7 pasa el método "accept" al request handler
                .listen(
                    config().getInteger("http.port", 8080), //11 el puerto se determina por configuración
                    result -> { //9 se configura el puerto de escucha
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
