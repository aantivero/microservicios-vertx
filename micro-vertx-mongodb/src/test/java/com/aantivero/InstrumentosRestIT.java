package com.aantivero;

import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.delete;
import static io.restassured.RestAssured.get;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Instrumentos REST API Integration Test
 * Por convención, Failsafe, entiende que los archivos que inician con IT o finalizan con IT son Integration Tests
 * Created by aantivero on 21/11/2016.
 */
public class InstrumentosRestIT {

    @BeforeClass
    public static void configureRestAssured() {
        RestAssured.baseURI = "http://localhost";
        RestAssured.port = Integer.getInteger("http.port", 8080);
    }

    @AfterClass
    public static void unconfigureRestAssured() {
        RestAssured.reset();
    }

    @Test
    public void checkRetornarInstrumentoIndividual() {
        final String id = get("/api/instrumentos").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath().getString("find { it.descripcion=='Galicia 24hs' }.id");
        get("/api/instrumentos/"+id).then()
                .assertThat()
                .statusCode(200)
                .body("codigo", equalTo("GALC"))
                .body("descripcion", equalTo("Galicia 24hs"))
                .body("id", equalTo(id));
    }

    @Test
    public void checkInsertarYBorrarInstrumento() {
        //crear un nuevo instrumento y retornar el resultado (instancia de Instrumento)
        Instrumento instrumento = given()
                .body("{\"codigo\":\"ALA\", \"descripcion\":\"Aluar Metalurgica\"}")
                .request()
                .post("/api/instrumentos/")
                .thenReturn().as(Instrumento.class);
        assertThat(instrumento.getCodigo()).isEqualToIgnoringCase("ALA");
        assertThat(instrumento.getDescripcion()).isEqualToIgnoringCase("Aluar Metalurgica");
        assertThat(instrumento.getId()).isNotEmpty();

        //chequear que se creo y que son válidos los parámetros
        get("/api/instrumentos/" + instrumento.getId())
                .then()
                .assertThat()
                .statusCode(200)
                .body("codigo", equalTo("ALA"))
                .body("descripcion", equalTo("Aluar Metalurgica"))
                .body("id", equalTo(instrumento.getId()));

        //borrar el instrumento
        delete("/api/instrumentos/" + instrumento.getId())
                .then()
                .assertThat()
                .statusCode(204);

        //el instrumento no existe mas
        get("/api/instrumentos/" + instrumento.getId())
                .then()
                .assertThat()
                .statusCode(404);

    }

}
