package com.aantivero;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.http.HttpServer;
import io.vertx.core.http.HttpServerResponse;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.jdbc.JDBCClient;
import io.vertx.ext.sql.SQLConnection;
import io.vertx.ext.sql.UpdateResult;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Primer aplicacion vertx.
 * 1 En Vertx un Verticle es un componente
 * Created by alejandro on 20/11/2016.
 */
public class MiPrimerVerticle extends AbstractVerticle {

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
    }

    @Override
    public void stop() throws Exception {
        //cierre de la conexion
        jdbc.close();
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
            //siempre chequea en base a la conexion
            SQLConnection connection = result.result();
            connection.execute(
                    "CREATE TABLE IF NOT EXISTS instrumento (id INTEGER IDENTITY, " +
                            "codigo varchar(20), descripcion varchar(100))",
                    ar -> {
                        if (ar.failed()) {
                            future.fail(ar.cause());
                            connection.close();
                            return;
                        }
                        connection.query("SELECT * FROM instrumento", select -> {
                            if (select.failed()) {
                                future.fail(select.cause());
                                connection.close();
                                return;
                            }
                            //si no hay datos en la tabla inserta 2 instrumentos
                            if (select.result().getNumRows() == 0) {
                                insertarDatos(
                                        new Instrumento("GALC", "Galicia 24hs"),
                                        connection,
                                        (v) -> insertarDatos(
                                                new Instrumento("EDEN", "Edenor 72hs"),
                                                connection,
                                                (r) -> {
                                                    next.handle(Future.<Void>succeededFuture());
                                                    connection.close();
                                                }
                                        )
                                );
                            } else {
                                next.handle(Future.<Void>succeededFuture());
                                connection.close();
                            }
                        });
                    });
        }
    }

    private void insertarDatos(Instrumento instrumento, SQLConnection connection, Handler<AsyncResult<Instrumento>> next) {
        String sql = "INSERT INTO instrumento (codigo, descripcion) VALUES ?, ?";
        //ejecuta el update con parametros, pasando los valores. este impide el SQL injection
        connection.updateWithParams(sql,
                new JsonArray().add(instrumento.getCodigo()).add(instrumento.getDescripcion()),
                (ar) -> {
                    if (ar.failed()) {
                        next.handle(Future.failedFuture(ar.cause()));
                        return;
                    }
                    UpdateResult result = ar.result();

                    //crea un nuevo objeto de Instrumento con el id generado
                    Instrumento instrumentoNuevo = new Instrumento(result.getKeys().getInteger(0),
                            instrumento.getCodigo(), instrumento.getDescripcion());

                    next.handle(Future.succeededFuture(instrumentoNuevo));
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
            jdbc.getConnection(ar -> {
                actualizar(id, json, ar.result(), (instrumento) -> {
                    if (instrumento.failed()) {
                        routingContext.response().setStatusCode(404).end();
                    } else {
                        routingContext.response()
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(instrumento.result()));
                    }
                    ar.result().close();
                });
            });
        }
    }

    private void getInstrumento(RoutingContext routingContext) {
        final String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(404).end();
        } else {
            jdbc.getConnection(ar -> {
               SQLConnection connection = ar.result();
                select(id, connection, result -> {
                    if (result.succeeded()) {
                        routingContext.response()
                                .setStatusCode(200)
                                .putHeader("content-type", "application/json; charset=utf-8")
                                .end(Json.encodePrettily(result.result()));
                    } else {
                        routingContext.response().setStatusCode(404).end();
                    }
                    connection.close();
                });
            });
        }
    }

    private void select(String id, SQLConnection connection, Handler<AsyncResult<Instrumento>> resultHandler) {
        connection.queryWithParams("SELECT * FROM instrumento WHERE id=?", new JsonArray().add(id), ar -> {
           if (ar.failed()) {
               resultHandler.handle(Future.failedFuture("Instrumento no encontrado"));
           } else {
               if (ar.result().getNumRows() >= 1) {
                   resultHandler.handle(Future.succeededFuture(new Instrumento(ar.result().getRows().get(0))));
               } else {
                   resultHandler.handle(Future.failedFuture("Instrumento no encontrado"));
               }
           }
        });
    }

    private void actualizar(String id, JsonObject contenido, SQLConnection connection,
                        Handler<AsyncResult<Instrumento>> resultHandler) {
        String sql = "UPDATE instrumento SET codigo=?, descripcion=? WHERE id=?";
        connection.updateWithParams(sql, new JsonArray().add(contenido.getString("codigo"))
                .add(contenido.getString("descripcion")).add(id),
                resultado -> {
                    if (resultado.failed()) {
                        resultHandler.handle(Future.failedFuture("No se pudo actualizar el instrumento"));
                        return;
                    }
                    if (resultado.result().getUpdated() == 0) {
                        resultHandler.handle(Future.failedFuture("No se encontro el instrumento"));
                    }
                    resultHandler.handle(
                            Future.succeededFuture(new Instrumento(Integer.getInteger(id),
                                    contenido.getString("codigo"), contenido.getString("descripcion"))));
                });
    }

    private void deleteInstrumento(RoutingContext routingContext) {
        //obtengo el parametro del request
        String id = routingContext.request().getParam("id");
        if (id == null) {
            routingContext.response().setStatusCode(400).end(); //no se encuentra recurso
        } else {
            jdbc.getConnection(ar -> {
                SQLConnection connection = ar.result();
                connection.execute("DELETE FROM instrumento WHERE id='" + id + "'",
                        result -> {
                            routingContext.response().setStatusCode(204).end(); //NO - CONTENT
                            connection.close();
                        });
            });
        }
    }

    private void addInstrumento(RoutingContext routingContext) {
        jdbc.getConnection(ar -> {
            //obtengo el instrumento del request body
            final Instrumento instrumento = Json.decodeValue(routingContext.getBodyAsString(), Instrumento.class);
            SQLConnection connection = ar.result();
            insertarDatos(instrumento, connection,
                    (r) -> routingContext.response()
                            .setStatusCode(201) //creado
                            .putHeader("content-type", "application/json; charset=utf-8")
                            .end(Json.encodePrettily(r.result())));
            connection.close();
        });
    }

    private void getAll(RoutingContext routingContext) {
        jdbc.getConnection(ar -> {
            SQLConnection connection = ar.result();
            connection.query("SELECT * FROM instrumento", resultado -> {
                //se agrego un constructor de json
                List<Instrumento> instrumentos = resultado.result().getRows()
                        .stream().map(Instrumento::new).collect(Collectors.toList());
                routingContext.response()
                        .putHeader("content-type", "application/json; charset=utf-8")
                        .end(Json.encodePrettily(instrumentos));
                connection.close();
            });
        });
    }

}
