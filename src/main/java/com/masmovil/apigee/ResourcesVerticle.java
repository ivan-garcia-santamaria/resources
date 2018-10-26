package com.masmovil.apigee;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.Json;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.ext.web.handler.BodyHandler;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ResourcesVerticle extends AbstractVerticle {


    private WebClient client;

    private Map<String,String> cacheSpec=new HashMap<>();
    private Map<String,String> cacheSchema=new HashMap<>();
    @Override
    public void start(Future<Void> fut) {

        // Create a router object.
        Router router = Router.router(vertx);

        router.route("/v1/schemas*").handler(BodyHandler.create());
        router.get("/v1/schemas/:id").handler(this::getResourceSchema);
        router.get("/v1/schemas/cache").handler(this::getCacheSchemas);
        router.delete("/v1/schemas/cache/:id").handler(this::deleteCacheSchema);

        router.route("/v1/specs*").handler(BodyHandler.create());
        router.get("/v1/specs/:id").handler(this::getResourceSpec);
        router.get("/v1/specs/cache").handler(this::getCacheSpecs);
        router.delete("/v1/specs/cache/:id").handler(this::deleteCacheSpec);

        vertx.createHttpServer()
                .requestHandler(router::accept)
                .listen(// Retrieve the port from the configuration,
                        // default to 8080.
                        config().getInteger("http.port", 8080),
                        result -> {
                            if (result.succeeded()) {
                                fut.complete();
                            } else {
                                fut.fail(result.cause());
                            }
                        });
        // Create the web client and enable SSL/TLS with a trust store
        client = WebClient.create(vertx,
                new WebClientOptions()
                        .setSsl(true)
/*
                        .setTrustStoreOptions(new JksOptions()
                                .setPath("client-truststore.jks")
                                .setPassword("wibble")
                        )
*/
        );
    }


    private void getCacheSpecs(RoutingContext rc) {
        rc.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encode(cacheSpec));
    }

    private void deleteCacheSpec(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        if (id == null) {
            rc.response().setStatusCode(400).end();
        } else {
            log.info("borrando de la cache la especificacion {}",id);
            cacheSpec.remove(id);
        }
        rc.response()
                .putHeader("content-type", "application/json; charset=utf-8")
                .end(Json.encode(cacheSpec));


    }

    private void getCacheSchemas(RoutingContext rc) {
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(cacheSchema));
    }

    private void deleteCacheSchema(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        if (id == null) {
            rc.response().setStatusCode(400).end();
        } else {
            log.info("borrando de la cache la especificacion {}",id);
            cacheSchema.remove(id);
        }
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(Json.encode(cacheSchema));


    }

    private void getResourceSpec(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        if (id == null) {
            rc.response().setStatusCode(400).end();
        } else {
            getResource(rc,
                ResourceSource.builder().host("raw.githubusercontent.com")
                    .port(443)
                    .path("/ivan-garcia-santamaria/jolt-spec/master/spec_" + id + ".json")
                    .build(),
                id,
                cacheSpec);
        }
    }

    private void getResourceSchema(RoutingContext rc) {
        final String id = rc.request().getParam("id");
        if (id == null) {
            rc.response().setStatusCode(400).end();
        } else {
            getResource(rc,
                ResourceSource.builder().host("raw.githubusercontent.com")
                    .port(443)
                    .path("/ivan-garcia-santamaria/schema-validator/master/schema_" + id + ".json")
                    .build(),
                id,
                cacheSchema);
        }
    }

    private void getResource(RoutingContext routingContext,
                             ResourceSource resourceSource, String id,
                             Map<String,String> cache) {
        log.info("id de recurso {}",id);
        String resource=cache.get(id);
        if (resource!=null) {
            log.info("resource cacheada3");
            returnResource(routingContext, resource);
        }else {
            // Send a GET request
            client.get(resourceSource.getPort(),
                        resourceSource.getHost(),
                        resourceSource.getPath())
                    .send(ar -> {
                        if (ar.succeeded()) {
                            // Obtain response
                            HttpResponse<Buffer> response = ar.result();

                            log.info("response.statusCode() {}", response.statusCode());
                            log.info("response.headers().get(\"content-type\") {}", response.headers().get("content-type"));
                            String resourceN=response.bodyAsString();
                            //log.info("body: {}", spec);
                            cache.put(id,resourceN);
                            returnResource(routingContext, resourceN);
                        } else {
                            log.info("Something went wrong {}", ar.cause().getMessage());
                        }
                    });
        }
    }

    private void returnResource(RoutingContext rc, String resource) {
        rc.response()
            .putHeader("content-type", "application/json; charset=utf-8")
            .end(resource);

    }

}
