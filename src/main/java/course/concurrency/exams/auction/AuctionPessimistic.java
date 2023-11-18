package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private final Notifier notifier;
    private volatile Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        if (latestBid == null) {
            synchronized (this) {
                if (latestBid == null) {
                    latestBid = bid;
                    return true;
                }
            }
        }

        if (bid.getPrice() > latestBid.getPrice()) {
            synchronized (this) {
                if (bid.getPrice() > latestBid.getPrice()) {
                    notifier.sendOutdatedMessage(latestBid);
                    latestBid = bid;
                    return true;
                }
            }
        }
        return false;
    }

    public Bid getLatestBid() {
        return latestBid;
    }
}
