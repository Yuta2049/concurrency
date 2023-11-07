package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicReference;

public class AuctionOptimistic implements Auction {

    private Notifier notifier;
    private AtomicReference<Bid> latestBid = new AtomicReference<>();

    public AuctionOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid previousBid = latestBid.get();
        if (previousBid == null || bid.getPrice() > previousBid.getPrice()) {
            while (!latestBid.compareAndSet(previousBid, bid)) {
                previousBid = latestBid.get();
                if (previousBid != null && bid.getPrice() <= previousBid.getPrice()) {
                    return false;
                }
            }
            if (previousBid != null) {
                notifier.sendOutdatedMessage(previousBid);
            }
            return true;
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid.get();
    }
}
