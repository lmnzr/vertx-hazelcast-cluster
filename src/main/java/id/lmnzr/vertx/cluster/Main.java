package id.lmnzr.vertx.cluster;

import com.hazelcast.config.Config;
import com.hazelcast.config.JoinConfig;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import io.vertx.core.eventbus.EventBusOptions;
import io.vertx.core.spi.cluster.ClusterManager;
import io.vertx.spi.cluster.hazelcast.HazelcastClusterManager;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class Main {
    public static void main(String[] args) {
        Logger log = Logger.getLogger(Main.class.getSimpleName());
        Config hazelcastConfig = new Config();

//        // Declare Cluster Group Name
//        hazelcastConfig.getGroupConfig().setName("my-cluster-name");

//        // Enable TCP-API Discovery (need at least one member)
//        JoinConfig joinConfig =  hazelcastConfig.getNetworkConfig().getJoin();
//        joinConfig.getMulticastConfig().setEnabled(false);
//        joinConfig.getTcpIpConfig().setEnabled(true)
//                                .getMembers().add("");

        // Enable AWS Discovery
        hazelcastConfig.getNetworkConfig().getInterfaces().setEnabled(true).addInterface("10.0.*.*");
        JoinConfig joinConfig = hazelcastConfig.getNetworkConfig().getJoin();
        joinConfig.getMulticastConfig().setEnabled(false);
        joinConfig.getAwsConfig()
                .setEnabled(true)
                .setProperty("region", "ap-southeast-1")
                .setProperty("tag-key", "aws:cloudformation:stack-name")
                .setProperty("tag-value", "EC2ContainerService-test-cluster");
        hazelcastConfig.getNetworkConfig().setJoin(joinConfig);


        ClusterManager mgr = new HazelcastClusterManager(hazelcastConfig);
        String hostAddress = getAddress();

        log.info(String.format("Host Address %s", getAddress()));

        VertxOptions options = new VertxOptions()
//                .setClustered(true)
//                .setClusterHost(hostAddress)
//                .setClusterPort(18001)
                .setClusterManager(mgr)
                //.setQuorumSize(2)
                .setHAEnabled(true);

        EventBusOptions ebOptions = new EventBusOptions()
                .setClustered(true)
                .setHost(hostAddress);
//                .setPort(18002);

        options.setEventBusOptions(ebOptions);

        Vertx.clusteredVertx(options, handler->{
            if(handler.succeeded()){
                DeploymentOptions containerOption = new DeploymentOptions().setHa(false); // ContainerVerticle should not restart
                handler.result().deployVerticle(Verticle.class,containerOption,deployHandler->{
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

    // Function to get the List
    private static <T> List<T>
    toList(Iterator<T> iterator) {
        List<T> list = new ArrayList<>();
        iterator.forEachRemaining(list::add);
        return list;
    }

    // Function to get host address
    private static String getAddress()  {
        try {
            List<NetworkInterface> networkInterfaces = toList(NetworkInterface.getNetworkInterfaces().asIterator());
            return networkInterfaces.stream().flatMap(iface->iface.inetAddresses()
                    .filter(entry->entry.getAddress().length == 4)
                    .filter(entry->!entry.isLoopbackAddress())
                    .filter(entry->entry.getAddress()[0] != Integer.valueOf(10).byteValue())
                    .map(InetAddress::getHostAddress)).findFirst().orElse(null);
        } catch (SocketException e) {
            return null;
        }
    }

}
