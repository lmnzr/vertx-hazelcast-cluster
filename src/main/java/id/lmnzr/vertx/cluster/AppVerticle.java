package id.lmnzr.vertx.cluster;

import io.vertx.core.AbstractVerticle;

import java.util.logging.Logger;

public class AppVerticle extends AbstractVerticle {
    private Logger log = Logger.getLogger(AppVerticle.class.getSimpleName());
    public static final String ebApplication = AppVerticle.class.getPackageName();

    @Override
    public void start(){
        String appId = config().getString("ID");
        log.info(String.format("Deploy App Verticle %s", appId));

        vertx.eventBus().consumer(String.format("%s.%s",ebApplication,appId),message->{
            String receivedMessage = (String) message.body();
            log.info(String.format("App Verticle Receive Message %s", receivedMessage));
        });

        vertx.setPeriodic(3000,periodicHnadler->{
            log.info(String.format("App Verticle %s is alive", appId));
        });
    }
}

