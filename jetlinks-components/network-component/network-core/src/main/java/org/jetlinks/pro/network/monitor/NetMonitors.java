package org.jetlinks.pro.network.monitor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class NetMonitors {

    public static final NoneNetMonitor none = new NoneNetMonitor();

    private static final List<NetMonitorSupplier> suppliers = new CopyOnWriteArrayList<>();

    static {
        suppliers.add((id, tags) -> {
            Logger logger = LoggerFactory.getLogger("org.jetlinks.pro.network.monitor." + id);
            if (logger.isInfoEnabled()) {
                return new LogNetMonitor(logger);
            }
            return null;
        });
    }

    public static void register(NetMonitorSupplier supplier) {
        suppliers.add(supplier);
    }

    private static NetMonitor doGet(String id, String... tags) {
        List<NetMonitor> all = suppliers.stream()
                .map(supplier -> supplier.getMonitor(id, tags))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        if (all.isEmpty()) {
            return none;
        }
        if (all.size() == 1) {
            return all.get(0);
        }
        CompositeNetMonitor monitor = new CompositeNetMonitor();
        monitor.add(all);
        return monitor;
    }

    /**
     * 获取网络监控
     * <pre>
     *     getMonitor("mqtt-server","id","test","tls","true")
     * </pre>
     *
     * @param id   唯一标识
     * @param tags 标签 奇数为key,偶数为value
     * @return 监控
     */
    public static NetMonitor getMonitor(String id, String... tags) {
        return doGet(id, tags);
    }

}
