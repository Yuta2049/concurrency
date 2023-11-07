package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private Notifier notifier;
    private Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public boolean propose(Bid bid) {
        boolean flag = false;
        Bid prev = null;
        synchronized (this) {
            if (latestBid == null || bid.getPrice() > latestBid.getPrice()) {
                flag = true;
                prev = latestBid;
                latestBid = bid;
            }
        }

        if (flag) {
            if (prev != null) {
                notifier.sendOutdatedMessage(prev);
            }
            return true;
        }
        return false;
    }

    public synchronized Bid getLatestBid() {
        return latestBid;
    }
}
