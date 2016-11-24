package com.aantivero;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Primer aplicacion vertx.
 * 1 En Vertx un Verticle es un componente
 * Created by alejandro on 20/11/2016.
 */
public class MiPrimerVerticle extends AbstractVerticle {

    //persistencia de instrumentos
    private Map<Integer, Instrumento> instrumentos = new LinkedHashMap<>();

    private JDBCClient jdbc;

    //3 este método es llamado cuando el verticle es deployado
    //5 el Future se utiliza para indicar si fue completado o no
    @Override
    public void start(Future<Void> future) {
        //crear e inicializar la instancia JDBCClient
        //en configuracion agregar los parametros url y driver_class
        jdbc = JDBCClient.createShared(vertx, config(), "instrumentos-datos");

        //iniciar la aplicacion asincronicamente
        /**
         * todos cumplen el mismo patron:
         *      1) chequear si la ultima operacion fue exitosa
         *      2) realizar tarea
         *      3) llamar al proximo paso
         */
        iniciarBackend(
                (connection) -> crearDatos(connection,
                        (nothing) -> iniciarAplicacionWeb(
                                (http) -> completarInicio(http, future)
                        ), future
                ), future);

        //cargar instrumentos
        createSomeData();
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
                    result -> { //9 se configura el puerto de escucha
                        if (result.succeeded()) { //10 lambda de resultado
                            future.complete();
                        } else {
                            future.fail(result.cause());
                        }
                });
    }

    //recupera el SQLConnection y luego llama al proximo (next step)
    private void iniciarBackend(Handler<AsyncResult<SQLConnection>> next, Future<Void> future) {
        jdbc.getConnection(ar -> {
            if (ar.failed()) {
                future.fail(ar.cause());
            } else {
                next.handle(Future.succeededFuture(ar.result()));
            }
        });
    }

    //crear la tabla (si no existe) y algunos datos
    private void crearDatos(AsyncResult<SQLConnection> result,
                            Handler<AsyncResult<Void>> next, Future<Void> future) {
        if (result.failed()) {
            future.fail(result.cause());
        } else {
            SQLConnection connection = result.result();
        }
    }

    private void updateInstrumento(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        JsonObject json = routingContext.getBodyAsJson();
        if (id == null || json == null) {
            routingContext.response().setStatusCode(400).end();
        } else {
            final Integer idInteger = Integer.valueOf(id);
            Instrumento instrumento = instrumentos.get(idInteger);
            if (instrumento == null) {
                routingContext.response().setStatusCode(404).end();
            } else {
                instrumento.setCodigo(json.getString("codigo"));
                instrumento.setDescripcion(json.getString("descripcion"));
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(instrumento));
            }
        }
    }

    private void getInstrumento(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(404).end();
        } else {
            final Integer idInteger = Integer.valueOf(id);
            Instrumento instrumento = instrumentos.get(idInteger);
            if (instrumento == null) {
                routingContext.response().setStatusCode(404).end();
            } else {
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(instrumento));
            }
        }
    }

    private void deleteInstrumento(RoutingContext routingContext) {
        //obtengo el parametro del request
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end(); //no se encuentra recurso
        } else {
            Integer idInteger = Integer.valueOf(id);
            instrumentos.remove(idInteger);
        }
        routingContext.response().setStatusCode(204).end(); //NO - CONTENT
    }

    private void addInstrumento(RoutingContext routingContext) {
        //obtengo el instrumento del request body
        final Instrumento instrumento = Json.decodeValue(routingContext.getBodyAsString(), Instrumento.class);
        instrumentos.put(instrumento.getId(), instrumento);
        routingContext.response()
                .setStatusCode(201) //creado
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(instrumento));
    }

    private void createSomeData() {
        //crear algunos instrumentos
        Instrumento ins1 = new Instrumento("GALC", "Galicia 24hs");
        instrumentos.put(ins1.getId(), ins1);
        Instrumento ins2 = new Instrumento("EDEN", "Edenor 72hs");
        instrumentos.put(ins2.getId(), ins2);
    }

    private void getAll(RoutingContext routingContext) {
        routingContext.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encodePrettily(instrumentos.values()));
    }

    //4 podemos implementar el método close() pero no es muy recomendable
    //por el momento dejo que lo haga vertx
}
