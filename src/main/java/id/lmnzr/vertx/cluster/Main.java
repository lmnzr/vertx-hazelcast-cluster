package id.lmnzr.vertx.cluster;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.zookeeper.ZookeeperClusterManager;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        Logger log = Logger.getLogger(Main.class.getSimpleName());

        JsonObject zkConfig = new JsonObject();
        zkConfig.put("zookeeperHosts", "ec2-3-0-93-219.ap-southeast-1.compute.amazonaws.com");
        zkConfig.put("rootPath", "io.vertx");
        zkConfig.put("retry", new JsonObject()
                .put("initialSleepTime", 3000)
                .put("maxTimes", 3));


        ClusterManager mgr = new ZookeeperClusterManager(zkConfig);

        VertxOptions options = new VertxOptions()
//                .setClustered(true)
//                .setClusterHost(hostAddress)
//                .setClusterPort(18001)
                .setClusterManager(mgr)
                //.setQuorumSize(2)
                .setHAEnabled(true);

        EventBusOptions ebOptions = new EventBusOptions()
                .setClustered(true);
//                .setHost(hostAddress);
//                .setPort(18002);

        options.setEventBusOptions(ebOptions);

        Vertx.clusteredVertx(options, handler->{
            if(handler.succeeded()){
                DeploymentOptions containerOption = new DeploymentOptions().setHa(false); // ContainerVerticle should not restart
                handler.result().deployVerticle(ContainerVerticle.class,containerOption, deployHandler->{
                    if(handler.succeeded()){
                        log.info("Verticle Deployed");
                    }else{
                        log.severe("Verticle Deployment Failed");
                    }
                });
            } else{
                log.severe(handler.cause().getMessage());
            }
        });

    }
}
