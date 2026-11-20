package banking.model;

public class InvestmentInterestStrategy implements InterestStrategy {
    private final InterestCalculator calculator = new InterestCalculator();

    @Override
    public double calculateInterest(double balance) {
        return calculator.calculateInvestmentInterest(balance);
    }

    @Override
    public double getRate() {
        return InterestCalculator.INVESTMENT_MONTHLY_RATE;
    }
}
