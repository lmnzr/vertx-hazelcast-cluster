package id.lmnzr.vertx.cluster;

import io.vertx.core.AbstractVerticle;

import java.util.logging.Logger;

public class Verticle extends AbstractVerticle {
    private Logger log = Logger.getLogger(Verticle.class.getSimpleName());

    @Override
    public void start(){
        vertx.setPeriodic(1000,periodicHnadler->{
            log.info("App Verticle is alive");
        });
    }
}
