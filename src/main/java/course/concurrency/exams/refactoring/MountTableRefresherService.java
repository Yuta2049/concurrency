package course.concurrency.exams.refactoring;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class MountTableRefresherService {
    private final ExecutorService executorService =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

    private Others.RouterStore routerStore = new Others.RouterStore();
    private long cacheUpdateTimeout;
    /**
     * All router admin clients cached. So no need to create the client again and
     * again. Router admin address(host:port) is used as key to cache RouterClient
     * objects.
     */
    private Others.LoadingCache<String, Others.RouterClient> routerClientsCache;

    /**
     * Removes expired RouterClient from routerClientsCache.
     */
    private ScheduledExecutorService clientCacheCleanerScheduler;

    public void serviceInit() {
        long routerClientMaxLiveTime = 15L;
        this.cacheUpdateTimeout = 10L;
        routerClientsCache = new Others.LoadingCache<String, Others.RouterClient>();
        routerStore.getCachedRecords().stream().map(Others.RouterState::getAdminAddress)
                .forEach(addr -> routerClientsCache.add(addr, new Others.RouterClient()));

        initClientCacheCleaner(routerClientMaxLiveTime);
    }

    public void serviceStop() {
        clientCacheCleanerScheduler.shutdown();
        // remove and close all admin clients
        routerClientsCache.cleanUp();
    }

    private void initClientCacheCleaner(long routerClientMaxLiveTime) {
        ThreadFactory tf = new ThreadFactory() {
            @Override
            public Thread newThread(Runnable r) {
                Thread t = new Thread();
                t.setName("MountTableRefresh_ClientsCacheCleaner");
                t.setDaemon(true);
                return t;
            }
        };

        clientCacheCleanerScheduler =
                Executors.newSingleThreadScheduledExecutor(tf);
        /*
         * When cleanUp() method is called, expired RouterClient will be removed and
         * closed.
         */
        clientCacheCleanerScheduler.scheduleWithFixedDelay(
                () -> routerClientsCache.cleanUp(), routerClientMaxLiveTime,
                routerClientMaxLiveTime, TimeUnit.MILLISECONDS);
    }

    /**
     * Refresh mount table cache of this router as well as all other routers.
     */
    public void refresh() {
        List<MountTableRefresher> refreshThreads = routerStore.getCachedRecords().stream()
                .map(Others.RouterState::getAdminAddress)
                .filter(adminAddress -> !adminAddress.isBlank())        // this router has not enabled router admin.
                .map(adminAddress -> adminAddress.contains("local") ?   // Local router's cache update does not require
                        getRefresher("local", adminAddress) :    // RPC call, so no need for RouterClient
                        getRefresher(adminAddress, adminAddress))
                .collect(Collectors.toList());

        if (!refreshThreads.isEmpty()) {
            invokeRefresh(refreshThreads);
        }
    }

    protected MountTableRefresher getRefresher(String address, String adminAddress) {
        return new MountTableRefresher(new Others.MountTableManager(address), adminAddress);
    }

    private void removeFromCache(String adminAddress) {
        routerClientsCache.invalidate(adminAddress);
    }

    private void invokeRefresh(List<MountTableRefresher> refreshThreads) {
        List<CompletableFuture<Boolean>> cfs = refreshThreads.stream()
                .map(rt -> CompletableFuture.supplyAsync(rt::run, executorService)
                        .completeOnTimeout(null, cacheUpdateTimeout, TimeUnit.MILLISECONDS)
                        .exceptionally(ex -> null))
                .collect(Collectors.toList());

        if (cfs.stream().map(CompletableFuture::join).anyMatch(Objects::isNull)) {
            log("Not all router admins updated their cache");
        }
        logResult(refreshThreads);
    }

    private void logResult(List<MountTableRefresher> refreshThreads) {
        int successCount = 0;
        int failureCount = 0;
        for (MountTableRefresher mountTableRefresh : refreshThreads) {
            if (mountTableRefresh.isSuccess()) {
                successCount++;
            } else {
                failureCount++;
                // remove RouterClient from cache so that new client is created
                removeFromCache(mountTableRefresh.getAdminAddress());
            }
        }
        log(String.format(
                "Mount table entries cache refresh successCount=%d,failureCount=%d",
                successCount, failureCount));
    }

    public void log(String message) {
        System.out.println(message);
    }

    public void setCacheUpdateTimeout(long cacheUpdateTimeout) {
        this.cacheUpdateTimeout = cacheUpdateTimeout;
    }

    public void setRouterClientsCache(Others.LoadingCache cache) {
        this.routerClientsCache = cache;
    }

    public void setRouterStore(Others.RouterStore routerStore) {
        this.routerStore = routerStore;
    }
}