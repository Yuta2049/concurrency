package course.concurrency.exams.auction;

import java.util.concurrent.atomic.AtomicMarkableReference;

public class AuctionStoppableOptimistic implements AuctionStoppable {

    private final Notifier notifier;
    private final AtomicMarkableReference<Bid> latestBid =
            new AtomicMarkableReference<>(new Bid(null, null, Long.MIN_VALUE), false);

    public AuctionStoppableOptimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        Bid previousBid;
        do {
            previousBid = latestBid.getReference();
            if (bid.getPrice() <= previousBid.getPrice() || latestBid.isMarked()) {
                return false;
            }
        } while (!latestBid.compareAndSet(previousBid, bid, false, false));
        notifier.sendOutdatedMessage(previousBid);
        return true;
    }

    public Bid getLatestBid() {
        return latestBid.getReference();
    }

    public Bid stopAuction() {
        Bid latest;
        do {
            if (latestBid.isMarked()) {
                return latestBid.getReference();
            }
            latest = latestBid.getReference();
        } while (!latestBid.compareAndSet(latest, latest, false, true));
        return latestBid.getReference();
    }
}
