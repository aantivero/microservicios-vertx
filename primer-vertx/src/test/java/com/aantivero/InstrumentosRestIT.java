package com.aantivero;

import io.restassured.RestAssured;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static io.restassured.RestAssured.get;
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
        final int id = get("/api/instrumentos").then()
                .assertThat()
                .statusCode(200)
                .extract()
                .jsonPath().getInt("find { it.descripcion=='Galicia 24hs' }.id");
        get("/api/instrumentos/"+id).then()
                .assertThat()
                .statusCode(200)
                .body("codigo", equalTo("GALC"))
                .body("descripcion", equalTo("Galicia 24hs"))
                .body("id", equalTo(id));
    }


}
