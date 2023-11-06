package course.concurrency.m2_async.cf.min_price;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
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

        Executor executor = Executors.newFixedThreadPool(50);

        List<CompletableFuture<Double>> cfs = shopIds.stream()
                .map(id -> CompletableFuture.supplyAsync(() -> priceRetriever.getPrice(itemId, id), executor)
                        .completeOnTimeout(NaN, 2950, TimeUnit.MILLISECONDS)
                        .handle((aDouble, throwable) -> throwable == null ? aDouble : NaN))
                .collect(Collectors.toList());

        return cfs.stream()
                .map(CompletableFuture::join)
                .min(Double::compareTo)
                .orElse(NaN);
    }
}
