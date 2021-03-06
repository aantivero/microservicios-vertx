package com.aantivero;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Primer aplicacion vertx.
 * 1 En Vertx un Verticle es un componente
 * Created by alejandro on 20/11/2016.
 */
public class MiPrimerVerticle extends AbstractVerticle {

    private MongoClient mongo;
    public static final String COLLECTION = "instrumentos";

    //3 este método es llamado cuando el verticle es deployado
    //5 el Future se utiliza para indicar si fue completado o no
    @Override
    public void start(Future<Void> future) {
        //instancia MongoClient,
        // con este cliente no se necesita manejar la conexion sino que lo realiza internamente
        mongo = MongoClient.createShared(vertx, config());

        //iniciar la aplicacion asincronicamente
        /**
         * todos cumplen el mismo patron:
         *      1) chequear si la ultima operacion fue exitosa
         *      2) realizar tarea
         *      3) llamar al proximo paso
         */
        crearDatos(
                (nothing) -> iniciarAplicacionWeb(
                        (http) -> completarInicio(http, future)
                ),future);
    }

    @Override
    public void stop() throws Exception {
        //cierre el driver mongo
        mongo.close();
    }

    //verficar la existencia de la coleccion y si existen datos
    private void crearDatos(Handler<AsyncResult<Void>> next, Future<Void> future) {
        Instrumento gal = new Instrumento("GALC", "Galicia 24hs");
        Instrumento eden = new Instrumento("EDEN", "Edenor 72hs");
        System.out.println(gal.toJson());
        System.out.println(eden.toJson());

        //verifico que existan datos en la coleccion de instrumentos
        //le paso por parametro un objeto JsonObject vacio. Es equivalente al Select * from
        mongo.count(COLLECTION, new JsonObject(), count -> {
           if (count.succeeded()) {
               if (count.result() == 0 ){
                   //no hay datos en la coleccion de instrumentos
                   mongo.insert(COLLECTION, gal.toJson(), handler -> {
                       if (handler.failed()) {
                           future.fail(handler.cause());
                       } else {
                           mongo.insert(COLLECTION, eden.toJson(), handler2 -> {
                              if (handler2.failed()) {
                                  future.failed();
                              } else {
                                  next.handle(Future.<Void>succeededFuture());
                              }
                           });
                       }
                   });
               } else {
                   next.handle(Future.<Void>succeededFuture());
               }
           } else {
               //existio algun erro
               future.fail(count.cause());
           }
        });
    }

    private void iniciarAplicacionWeb(Handler<AsyncResult<HttpServer>> next) {
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

        //devolver contenido estático desde el directorio /assets
        router.route("/assets/*").handler(StaticHandler.create("assets"));

        //routes de instrumentos
        router.get("/api/instrumentos").handler(this::getAll);
        //habilita la lectura del request body para todas las rutas dentro '/api/instrumentos'
        //la forma de habilitar de manera global sería router.route().handler(BodyHandler.create())
        router.route("/api/instrumentos*").handler(BodyHandler.create());
        router.post("/api/instrumentos").handler(this::addInstrumento);
        router.get("/api/instrumentos/:id").handler(this::getInstrumento);
        router.put("/api/instrumentos/:id").handler(this::updateInstrumento);
        router.delete("/api/instrumentos/:id").handler(this::deleteInstrumento);

        //2 al extender de AbstractVerticle ya tengo la propiedad vertx
        vertx
                .createHttpServer() //6 se crea un HTTP Server
                .requestHandler(router::accept) //7 pasa el método "accept" al request handler
                .listen(
                        config().getInteger("http.port", 8080), //11 el puerto se determina por configuración
                        next::handle
                );
    }

    private void completarInicio(AsyncResult<HttpServer> http, Future<Void> future) {
        if (http.succeeded()) {
            future.complete();
        } else {
            future.fail(http.cause());
        }
    }

    private void updateInstrumento(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        JsonObject json = routingContext.getBodyAsJson();
        if (id == null || json == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            mongo.updateCollection(COLLECTION,
                    new JsonObject().put("_id", id),//seleccion del documento
                    new JsonObject().put("$set", json),//update syntax
                    resultado -> {
                        if (resultado.failed()) {
                            routingContext.response().setStatusCode(404).end();
                        } else {
                            routingContext.response()
                                    .putHeader("content-type", "application/json; charset=utf-8")
                                    .end(Json.encodePrettily(
                                            new Instrumento(id, json.getString("codigo"), json.getString("descripcion")))
                                    );
                        }
            });
        }
    }

    private void getInstrumento(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(404).end();
        } else {
            mongo.findOne(COLLECTION, new JsonObject().put("_id", id), null, resultado -> {
               if (resultado.succeeded()) {
                   if (resultado.result() == null) {
                       routingContext.response().setStatusCode(404).end();
                       return;
                   }
                   Instrumento instrumento = new Instrumento(resultado.result());
                   routingContext.response()
                           .setStatusCode(200)
                           .putHeader("content-type", "application/json; charset=utf-8")
                           .end(Json.encodePrettily(instrumento));
               } else {
                   routingContext.response().setStatusCode(404).end();
               }
            });
        }
    }

    private void deleteInstrumento(RoutingContext routingContext) {
        //obtengo el parametro del request
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end(); //no se encuentra recurso
        } else {
            //removeDocument para eliminar al que cumple con la condicion
            mongo.removeDocument(COLLECTION, new JsonObject().put("_id", id), resultado ->
                routingContext.response().setStatusCode(204).end()
            );
        }
    }

    private void addInstrumento(RoutingContext routingContext) {
        final Instrumento instrumento = Json.decodeValue(routingContext.getBodyAsString(), Instrumento.class);
        mongo.insert(COLLECTION, instrumento.toJson(), resultado ->
            routingContext.response()
                    .setStatusCode(201)
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(instrumento.setId(resultado.result())))
        );
    }

    private void getAll(RoutingContext routingContext) {
        //devuelve todos los instrumentos de la coleccion, igual que el método count le pasamos un JsonObject vacio
        mongo.find(COLLECTION, new JsonObject(), resultado -> {
            //se obtiene una lista de JSON
            List<JsonObject> objetos = resultado.result();
            //crear un lista de instrumentos mapeados a la coleccion resultante
            List<Instrumento> instrumentos = objetos.stream().map(Instrumento::new).collect(Collectors.toList());
            routingContext.response()
                    .putHeader("content-type", "application/json; charset=utf-8")
                    .end(Json.encodePrettily(instrumentos));
        });
    }

}
