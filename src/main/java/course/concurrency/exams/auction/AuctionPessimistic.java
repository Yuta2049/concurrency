package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;
    private volatile Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
        latestBid = new Bid(null, null, Long.MIN_VALUE);
    }

    public boolean propose(Bid bid) {
        if (bid.getPrice() < latestBid.getPrice()) {
            return false;
        }
        synchronized (this) {
            if (bid.getPrice() > latestBid.getPrice()) {
                notifier.sendOutdatedMessage(latestBid);
                latestBid = bid;
                return true;
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
