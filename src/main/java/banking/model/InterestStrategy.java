package banking.model;

public interface InterestStrategy {
    double calculateInterest(double balance);

    double getRate();
}
