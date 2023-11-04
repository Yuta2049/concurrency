package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;
import java.util.stream.Collectors;

import static java.lang.Double.NaN;

public class PriceAggregator {

    private PriceRetriever priceRetriever = new PriceRetriever();
    private Collection<Long> shopIds = Set.of(10l, 45l, 66l, 345l, 234l, 333l, 67l, 123l, 768l);

    public void setPriceRetriever(PriceRetriever priceRetriever) {
        this.priceRetriever = priceRetriever;
    }

    public void setShops(Collection<Long> shopIds) {
        this.shopIds = shopIds;
    }

    public double getMinPrice(long itemId) {

        Executor executor = Executors.newCachedThreadPool();

        List<CompletableFuture<Double>> cfs = shopIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() ->
                        priceRetriever.getPrice(itemId, id), executor))
                .collect(Collectors.toList());

        return cfs.parallelStream().map(cf -> {
                    try {
                        return cf.completeOnTimeout(NaN, 2900, TimeUnit.MILLISECONDS).get();
                    } catch (InterruptedException | ExecutionException e) {
                        return NaN;
                    }
                })
                .min(Double::compareTo).orElse(NaN);
    }
}
