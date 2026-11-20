package banking.model;

public class SavingsInterestStrategy implements InterestStrategy {
    private final InterestCalculator calculator = new InterestCalculator();

    @Override
    public double calculateInterest(double balance) {
        return calculator.calculateSavingsInterest(balance);
    }

    @Override
    public double getRate() {
        return InterestCalculator.SAVINGS_MONTHLY_RATE;
    }
}
