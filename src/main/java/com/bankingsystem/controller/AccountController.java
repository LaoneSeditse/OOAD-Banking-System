package com.bankingsystem.controller;

import com.bankingsystem.controller.dto.AccountSummary;
import com.bankingsystem.controller.dto.ActionResult;
import com.bankingsystem.controller.dto.TransactionRow;
import com.bankingsystem.model.Account;
import com.bankingsystem.model.AccountType;
import com.bankingsystem.model.EmploymentDetails;
import com.bankingsystem.model.InvestmentAccount;
import com.bankingsystem.model.Money;
import com.bankingsystem.model.Transaction;
import com.bankingsystem.model.exception.BankingException;
import com.bankingsystem.service.AccountService;
import com.bankingsystem.service.CustomerService;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.OptionalDouble;

/**
 * Mediates between the customer views and the domain model for everything to do with accounts:
 * opening/closing accounts, deposits, withdrawals, transfers and history
 * (AccountController in the Deposit sequence diagram).
 */
public class AccountController {
    public static final List<String> BRANCHES = List.of("Gaborone Main", "Francistown", "Maun", "Serowe",
            "Lobatse", "Palapye", "Kasane");

    private final AccountService accountService;
    private final CustomerService customerService;
    private final UserSession session;

    public AccountController(AccountService accountService, CustomerService customerService, UserSession session) {
        this.accountService = accountService;
        this.customerService = customerService;
        this.session = session;
    }

    public List<AccountSummary> getMyAccounts() {
        if (!session.isCustomer()) {
            return List.of();
        }
        return accountService.getAccountsForCustomer(session.getUserId()).stream().map(DtoMapper::toSummary).toList();
    }

    public double getMyTotalBalance() {
        return getMyAccounts().stream().filter(a -> !"CLOSED".equals(a.status()))
                .mapToDouble(AccountSummary::balance).sum();
    }

    public List<String> getAccountTypes() {
        return Arrays.stream(AccountType.values()).map(Enum::name).toList();
    }

    public List<String> getBranches() {
        return BRANCHES;
    }

    public String describeAccountType(String type) {
        return switch (AccountType.valueOf(type)) {
            case SAVINGS -> "Savings: earns 0.05% interest monthly. Deposits only - withdrawals are not allowed.";
            case INVESTMENT -> "Investment: earns 5% interest monthly. Deposits and withdrawals allowed. Minimum opening deposit "
                    + Money.format(InvestmentAccount.MINIMUM_INITIAL_DEPOSIT) + ".";
            case CHEQUE -> "Cheque: for salary payments. Deposits and withdrawals of any amount. "
                    + "Only for employed customers - employer details required.";
        };
    }

    public ActionResult<AccountSummary> openAccount(String type, String branch, String initialDepositText,
                                                    String employerName, String employerAddress, String position) {
        if (!session.isCustomer()) {
            return ActionResult.fail("Please log in first");
        }
        if (InputValidator.isBlank(type)) {
            return ActionResult.fail("Please select an account type");
        }
        OptionalDouble deposit = InputValidator.parseNonNegative(initialDepositText);
        if (deposit.isEmpty()) {
            return ActionResult.fail("Please enter a valid opening deposit amount");
        }
        AccountType accountType = AccountType.valueOf(type);
        EmploymentDetails employment = null;
        if (accountType == AccountType.CHEQUE) {
            if (InputValidator.isBlank(employerName) || InputValidator.isBlank(employerAddress)
                    || InputValidator.isBlank(position)) {
                return ActionResult.fail("A Cheque account requires your employer's name, address and your position");
            }
            employment = new EmploymentDetails(employerName.trim(), employerAddress.trim(), position.trim());
        }
        try {
            Account account = accountService.openAccount(session.getUserId(), accountType, deposit.getAsDouble(),
                    branch, employment);
            return ActionResult.ok(accountType.getDisplayName() + " " + account.getAccountNumber()
                    + " opened successfully with a balance of " + Money.format(account.getBalance()),
                    DtoMapper.toSummary(account));
        } catch (BankingException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    /** Step 2 of the Deposit sequence diagram: validate the raw input. @return the parsed amount */
    public ActionResult<Double> validateInput(String accountNumber, String amountText, String pin) {
        if (InputValidator.isBlank(accountNumber)) {
            return ActionResult.fail("Please select an account");
        }
        OptionalDouble amount = InputValidator.parseAmount(amountText);
        if (amount.isEmpty()) {
            return ActionResult.fail("Please enter a valid amount greater than zero (max 2 decimal places)");
        }
        if (InputValidator.isBlank(pin)) {
            return ActionResult.fail("Please enter your password to authorise the transaction");
        }
        return ActionResult.ok("Input valid", amount.getAsDouble());
    }

    /** Steps 4-23 of the Deposit sequence diagram. */
    public ActionResult<TransactionRow> processDeposit(String accountNumber, String amountText, String pin) {
        ActionResult<Double> input = validateInput(accountNumber, amountText, pin);
        if (!input.success()) {
            return ActionResult.fail(input.message());
        }
        Optional<Account> account = findOwnAccount(accountNumber, pin);
        if (account.isEmpty()) {
            return ActionResult.fail("Account not found or invalid credentials");
        }
        try {
            Transaction tx = accountService.deposit(account.get().getAccountNumber(), input.data());
            StringBuilder message = new StringBuilder("Deposit successful. New balance: ")
                    .append(Money.format(tx.getBalanceAfter()));
            Account updated = accountService.findAccount(accountNumber).orElseThrow();
            if (accountService.checkInterestEligibility(updated)) {
                double interest = accountService.updateInterest(accountNumber);
                message.append(String.format("%nInterest updated: this account will earn about %s at the next "
                        + "monthly interest payment (%.2f%% monthly).", Money.format(interest),
                        updated.getInterestRate() * 100));
            }
            return ActionResult.ok(message.toString(), DtoMapper.toRow(tx));
        } catch (BankingException e) {
            return ActionResult.fail("Deposit failed: " + e.getMessage());
        }
    }

    public ActionResult<TransactionRow> processWithdrawal(String accountNumber, String amountText, String pin) {
        ActionResult<Double> input = validateInput(accountNumber, amountText, pin);
        if (!input.success()) {
            return ActionResult.fail(input.message());
        }
        Optional<Account> account = findOwnAccount(accountNumber, pin);
        if (account.isEmpty()) {
            return ActionResult.fail("Account not found or invalid credentials");
        }
        try {
            Transaction tx = accountService.withdraw(account.get().getAccountNumber(), input.data());
            return ActionResult.ok("Withdrawal successful. New balance: " + Money.format(tx.getBalanceAfter()),
                    DtoMapper.toRow(tx));
        } catch (BankingException e) {
            return ActionResult.fail("Withdrawal failed: " + e.getMessage());
        }
    }

    public ActionResult<TransactionRow> processTransfer(String fromAccount, String toAccount, String amountText,
                                                        String pin) {
        ActionResult<Double> input = validateInput(fromAccount, amountText, pin);
        if (!input.success()) {
            return ActionResult.fail(input.message());
        }
        if (InputValidator.isBlank(toAccount)) {
            return ActionResult.fail("Please enter the destination account number");
        }
        Optional<Account> from = findOwnAccount(fromAccount, pin);
        if (from.isEmpty()) {
            return ActionResult.fail("Account not found or invalid credentials");
        }
        if (accountService.findAccount(toAccount).isEmpty()) {
            return ActionResult.fail("Destination account " + toAccount.trim() + " does not exist");
        }
        try {
            List<Transaction> legs = accountService.transfer(fromAccount, toAccount.trim(), input.data());
            return ActionResult.ok(String.format("Transferred %s to %s. New balance: %s",
                    Money.format(input.data()), toAccount.trim(), Money.format(legs.get(0).getBalanceAfter())),
                    DtoMapper.toRow(legs.get(0)));
        } catch (BankingException e) {
            return ActionResult.fail("Transfer failed: " + e.getMessage());
        }
    }

    public ActionResult<Void> closeAccount(String accountNumber, String pin) {
        if (InputValidator.isBlank(accountNumber)) {
            return ActionResult.fail("Please select an account");
        }
        Optional<Account> account = findOwnAccount(accountNumber, pin);
        if (account.isEmpty()) {
            return ActionResult.fail("Account not found or invalid credentials");
        }
        try {
            Transaction payout = accountService.closeAccount(accountNumber);
            return ActionResult.ok("Account " + accountNumber + " has been closed."
                    + (payout == null ? "" : " Remaining balance of " + Money.format(-payout.getAmount()) + " paid out."));
        } catch (BankingException e) {
            return ActionResult.fail(e.getMessage());
        }
    }

    public List<TransactionRow> getTransactionHistory(String accountNumber) {
        if (!ownsAccount(accountNumber)) {
            return List.of();
        }
        return accountService.getTransactionHistory(accountNumber).stream().map(DtoMapper::toRow).toList();
    }

    public String getStatement(String accountNumber) {
        if (!ownsAccount(accountNumber)) {
            return "";
        }
        Account account = accountService.findAccount(accountNumber).orElseThrow();
        StringBuilder sb = new StringBuilder("STATEMENT\n").append(account.getAccountDetails()).append("\n\n");
        accountService.getTransactionHistory(accountNumber)
                .forEach(t -> sb.append(t.getTransactionDetails()).append('\n'));
        return sb.toString();
    }

    private boolean ownsAccount(String accountNumber) {
        return session.isCustomer() && accountService.findAccount(accountNumber)
                .map(a -> a.getHolder().getCustomerId().equals(session.getUserId()))
                .orElse(false);
    }

    private Optional<Account> findOwnAccount(String accountNumber, String pin) {
        if (!session.isCustomer() || !customerService.verifyPassword(session.getUserId(), pin)) {
            return Optional.empty();
        }
        return accountService.findAccount(accountNumber)
                .filter(a -> a.getHolder().getCustomerId().equals(session.getUserId()));
    }
}
