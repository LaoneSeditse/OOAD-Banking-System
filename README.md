# OOAD Banking System (Part B)

A Java 17 / JavaFX / JDBC (H2) banking system implemented from the Part A UML design
(use-case, class, state and sequence diagrams in [`docs/diagrams`](docs/diagrams)).

## Running in GitHub Codespaces

1. **Code -> Codespaces -> Create codespace on main**. The dev container installs JDK 17, Maven
   and a lightweight desktop, then compiles the project.
2. Open the **Ports** tab and open port **6080** (desktop) in the browser - password `vscode`.
3. In the terminal run:

   ```bash
   mvn javafx:run
   ```

   The JavaFX window appears on the Codespaces desktop.

Run the tests (headless, no desktop needed):

```bash
mvn test
```

Locally you only need JDK 17+ and Maven: `mvn javafx:run`.

## Sample data / demo logins

On first start the H2 database (`./data/bankingdb`) is created from
[`schema.sql`](src/main/resources/db/schema.sql) and seeded by
[`DataSeeder`](src/main/java/com/bankingsystem/dao/DataSeeder.java) with **10 customers**
(8 individual, 2 company), their savings / investment / cheque accounts and transactions,
plus one administrator. These are demo-only credentials:

| Role | Username | Password |
|------|----------|----------|
| Customer | `kmolefe`, `tsetshwane`, `nmokoena`, `lkgosi`, `omasire`, `bseretse`, `pdintwe`, `gmotsumi`, `kalaharitech`, `okavangofoods` | `password123` |
| Administrator (tick "Log in as bank administrator") | `admin` | `admin123` |

Delete the `data/` folder to reset the database.

## Business rules implemented

| Rule | Where |
|------|-------|
| Savings: deposits only, no withdrawals, 0.05 % monthly interest | `SavingsAccount` |
| Investment: deposits + withdrawals, min opening deposit BWP 500, 5 % monthly interest | `InvestmentAccount` |
| Cheque: deposits + withdrawals, requires employer name/address/position, no interest | `ChequeAccount`, `EmploymentDetails` |
| Customers must be 18+; one customer may hold many accounts; an account always has a customer | `User`, `Customer`, `Account` |
| Withdrawal limit BWP 5,000 per transaction; balance cannot go negative | `Account` |
| Account states Pending -> Active <-> Suspended -> Closed | `Account`, `AccountStatus` |
| Transaction states Initiated -> Validated -> Completed / Failed / Cancelled | `Transaction`, `TransactionStatus` |
| Customer locked after 3 failed logins, administrator can unlock | `User`, `AuthenticationController`, `AdminController` |

## Architecture

```
view (JavaFX boundary)  ->  controller  ->  service  ->  model (domain)
                                               |
                                               v
                                         dao (JDBC) -> H2
```

| Package | Contents |
|---------|----------|
| `model` | `User` (abstract) -> `Customer` (abstract) -> `IndividualCustomer` / `CompanyCustomer`; `BankAdministrator`; `Account` (abstract) -> `SavingsAccount` / `InvestmentAccount` / `ChequeAccount`; `Transaction`, `EmploymentDetails`, `InterestCalculator`, enums and exceptions |
| `service` | `BankService` / `BankServiceImpl`, `TransactionService` / `TransactionServiceImpl`, `AccountService`, `CustomerService`, `AdminService` |
| `dao` | `Database` (JDBC connection + schema), `CustomerDAO`, `AccountDAO`, `TransactionDAO`, `SessionDAO`, `AdministratorDAO`, `DataSeeder` |
| `controller` | `AuthenticationController`, `RegistrationController`, `AccountController`, `AdminController`, `UserSession`, DTOs |
| `view` | `LoginView`, `RegisterView`, `DashboardView`, `AccountView`, `OpenAccountView`, `DepositView`, `WithdrawView`, `TransferView`, `TransactionHistoryView`, `CloseAccountView`, `AdminView` |

Views only display data and collect input; all validation and rules live in the controllers,
services and domain classes. Views receive DTOs (`AccountSummary`, `TransactionRow`, ...) and never touch
domain objects or SQL.

### OOP principles

- **Abstraction** - abstract `User`, `Customer`, `Account`; interfaces `BankService`, `TransactionService`, `InterestStrategy`, `AccountNumberGenerator`.
- **Inheritance** - `Customer extends User`, `SavingsAccount extends Account`, `BankingException` hierarchy, ...
- **Interfaces** - `InterestStrategy` (implemented by `InterestCalculator`, used by the account types), `BankService`, `TransactionService`, `AccountNumberGenerator` (implemented by `AccountDAO`); services are used through their interfaces.
- **Polymorphism** - `account.calculateInterest()`, `account.withdraw()`, `customer.getDisplayName()` behave per subclass; `AccountDAO` rebuilds the right subclass from the DB.
- **Overriding** - `SavingsAccount.withdraw(...)` throws `WithdrawalNotAllowedException`; `ChequeAccount.validateOpening()` checks employment details; `calculateInterest()` / `getInterestRate()` per account type, etc.
- **Overloading** - `deposit(double)` / `deposit(double, String)`, `withdraw(...)`, `createAccount(...)`, `calculateInterest(balance)` / `calculateInterest(balance, months)`.
- **Encapsulation** - private fields; balances can only change through validated domain methods.

## Mapping to the sequence diagrams

- **Login**: `LoginView` -> `AuthenticationController.validateCredentials` -> `CustomerService.authenticateUser` -> `CustomerDAO` (find user, update failed attempts / lock) -> `SessionDAO.createSession` -> dashboard.
- **Deposit**: `DepositView` -> `AccountController.validateInput` / `processDeposit` (password check, account ownership) -> `AccountService.deposit` -> `Account.deposit` (`Transaction` validate/complete) -> `AccountDAO.saveAccount` + `TransactionDAO.insert` -> interest eligibility/update -> success message.

## Tests

`mvn test` runs:

- `model/AccountTest` - domain unit tests (rules, interest, state machines).
- `integration/BankingIntegrationTest` - controller -> service -> domain -> JDBC/H2 flows on an in-memory DB.
- `view/GuiFlowTest` - headless TestFX test that drives the real JavaFX screens: login -> deposit -> balance persisted, admin login.

See [`docs/INTEGRATION_TEST_REPORT.md`](docs/INTEGRATION_TEST_REPORT.md).
