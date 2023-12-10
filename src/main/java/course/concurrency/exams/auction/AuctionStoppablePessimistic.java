package course.concurrency.exams.auction;

public class AuctionStoppablePessimistic implements AuctionStoppable {

    private final Notifier notifier;
    private volatile Bid latestBid;
    private volatile boolean auctionIsStopped = false;

    public AuctionStoppablePessimistic(Notifier notifier) {
        this.notifier = notifier;
        latestBid = new Bid(null, null, Long.MIN_VALUE);
    }

    public boolean propose(Bid bid) {
        if (bid.getPrice() < latestBid.getPrice() || auctionIsStopped) {
            return false;
        }
        synchronized (this) {
            if (bid.getPrice() > latestBid.getPrice() && !auctionIsStopped) {
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

    public Bid stopAuction() {
        auctionIsStopped = true;
        synchronized (this) {
            return latestBid;
        }
    }
}
