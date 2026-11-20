package banking.model;

public class NoInterestStrategy implements InterestStrategy {
    @Override
    public double calculateInterest(double balance) {
        return 0.0;
    }

    @Override
    public double getRate() {
        return 0.0;
    }
}
