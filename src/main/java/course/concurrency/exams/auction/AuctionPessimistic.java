package course.concurrency.exams.auction;

public class AuctionPessimistic implements Auction {

    private Notifier notifier;
    private Bid latestBid;

    public AuctionPessimistic(Notifier notifier) {
        this.notifier = notifier;
    }

    public synchronized boolean propose(Bid bid) {
        if (latestBid == null || bid.getPrice() > latestBid.getPrice()) {
            notifier.sendOutdatedMessage(latestBid);
            latestBid = bid;
            return true;
        }
        return false;
    }

    public synchronized Bid getLatestBid() {
        return latestBid;
    }
}
