package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final AtomicReference<Bid> latestBid = new AtomicReference<>();
    private volatile boolean auctionIsStopped = false;

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
        latestBid.set(new Bid(null, null, Long.MIN_VALUE));
    }

    public boolean propose(Bid bid) {
        Bid previousBid;
        do {
            previousBid = latestBid.get();
            if (bid.getPrice() <= previousBid.getPrice() || auctionIsStopped) {
                return false;
            }
        } while (!latestBid.compareAndSet(previousBid, bid));
        notifier.sendOutdatedMessage(previousBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }

    public Bid stopAuction() {
        auctionIsStopped = true;
        return latestBid.get();
    }
}
