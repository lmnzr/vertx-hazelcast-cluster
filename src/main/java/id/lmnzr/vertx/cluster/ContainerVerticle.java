package id.lmnzr.vertx.cluster;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Promise;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;

import java.util.logging.Logger;

public class ContainerVerticle extends AbstractVerticle {
    private Logger log = Logger.getLogger(ContainerVerticle.class.getSimpleName());
    public static final String ebDeployer = ContainerVerticle.class.getPackageName()+".deployer";

    @Override
    public void start(Promise<Void> promise){
        Router router = createRouter();
        int port = 80;

        vertx.eventBus().consumer(ebDeployer,message->{
            String appID = (String) message.body();
            log.info(String.format("Deploying Application %s", appID));
            JsonObject appVerticleConfig = new JsonObject().put("ID", appID);

            vertx.deployVerticle(AppVerticle.class,
                    new DeploymentOptions()
                            .setConfig(appVerticleConfig)
                            .setInstances(1)
                            .setHa(true)
            );
        });

        vertx.createHttpServer()
                .requestHandler(router)
                .listen(port, result->{
                    if(result.succeeded()){
                        log.info(String.format("Listening on port %d", port));
                        promise.complete();
                    } else {
                        log.severe(result.cause().getMessage());
                        promise.fail(result.cause().getMessage());
                    }
                });
    }

    private Router createRouter(){
        Router router = Router.router(vertx);
        router.route().handler(BodyHandler.create());
        router.post("/").handler(this::handlerRoot);
        return router;
    }

    private void handlerRoot(RoutingContext context){
        String command = context.getBodyAsString();
        String[] tokens = command.split(" ");

        if(tokens[0].equalsIgnoreCase("deploy")){
            vertx.eventBus().send(ebDeployer,tokens[1]);
            context.response().end(String.format("Successfully handled command %s %n",command));
        } else if(tokens[0].equalsIgnoreCase("send")){
            vertx.eventBus().send(String.format("%s.%s",AppVerticle.ebApplication,tokens[1]),tokens[2]);
            context.response().end(String.format("Successfully send message %s to %s %n",tokens[2],tokens[1]));
        } else{
            context.response().end(String.format("Error unknown command %s %n",command));
        }
    }
}
