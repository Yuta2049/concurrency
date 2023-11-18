package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private final Notifier notifier;
    private final AtomicReference<Bid> latestBid = new AtomicReference<>();

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid previousBid = latestBid.get();
        if (previousBid == null) {
            if (latestBid.compareAndSet(null, bid)) {
                return true;
            }
        }

        do {
            previousBid = latestBid.get();
            if (bid.getPrice() <= previousBid.getPrice()) {
                return false;
            }
        } while (!latestBid.compareAndSet(previousBid, bid));
        notifier.sendOutdatedMessage(previousBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
