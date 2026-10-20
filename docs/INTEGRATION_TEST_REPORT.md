# Integration Test Report

**System:** OOAD Banking System (Java 17, JavaFX 17, H2 2.3 via JDBC)
**Command:** `mvn test`
**Result:** 30 tests, 0 failures, 0 errors

| Suite | Layer(s) exercised | Tests | Result |
|-------|--------------------|-------|--------|
| `model.AccountTest` | Domain model | 10 | Pass |
| `integration.BankingIntegrationTest` | Controller -> Service -> Domain -> DAO (JDBC) -> H2 (in-memory) | 15 | Pass |
| `view.GuiFlowTest` | JavaFX views (headless TestFX/Monocle) -> Controller -> Service -> Domain -> DAO -> H2 | 3 | Pass |

## Test environment

Each integration/GUI test creates a fresh, uniquely named in-memory H2 database
(`Database.inMemory(...)`), runs `schema.sql` and the `DataSeeder` (10 customers + accounts +
transactions + 1 admin), so tests are independent and repeatable.

## Full user flow (GUI): login -> deposit -> persistence

`GuiFlowTest.customerCanLoginAndDeposit` follows the Login and Deposit sequence diagrams:

1. Types `kmolefe` / `password123` into `LoginView` and clicks **Login**
   -> `AuthenticationController.validateCredentials` -> `CustomerService.authenticateUser` -> `CustomerDAO`, `SessionDAO`.
2. Verifies the `DashboardView` welcome label shows the customer's name.
3. Opens **Deposit Funds**, selects an account, enters `150.25` and the password, clicks **Deposit**
   -> `AccountController.processDeposit` -> `AccountService.deposit` -> `Account.deposit` -> `AccountDAO` / `TransactionDAO`.
4. Verifies the success message and re-reads the account **from the database** to confirm the
   balance increased by exactly BWP 150.25.
5. Logs out and verifies the login screen is shown again.

## Scenario coverage

| # | Scenario | Test | Expected | Result |
|---|----------|------|----------|--------|
| 1 | Sample data present | `sampleDataContainsAtLeastTenRecords` | >= 10 customers, accounts, transactions | Pass |
| 2 | Login + deposit persisted | `loginDepositAndPersistFlow` | balance + 250, one new transaction row | Pass |
| 3 | Wrong password / invalid amount on deposit | `depositIsRejectedWithWrongPasswordOrInvalidAmount` | rejected, no change | Pass |
| 4 | Savings withdrawal blocked; investment min BWP 500; investment withdrawal | `savingsWithdrawalIsBlockedAndInvestmentWithdrawalWorks` | savings rejected, 100 rejected, 1000 accepted, balance 600 after 400 withdrawal | Pass |
| 5 | Cheque account needs employment details | `chequeAccountNeedsEmploymentDetails` | blank details rejected, full details accepted | Pass |
| 6 | Transfer between accounts | `transferMovesMoneyBetweenAccounts` | 2000 -> 1500, 10 -> 510 in DB | Pass |
| 7 | 3 failed logins lock customer, admin unlocks | `threeFailedLoginsLockTheCustomerAndAdminCanUnlock` | locked, then login works after unlock | Pass |
| 8 | Monthly interest | `adminPaysMonthlyInterestOnlyToInterestBearingAccounts` | savings +0.05 %, investment +5 %, cheque unchanged | Pass |
| 9 | Registration age rule | `registrationValidatesAgeAndCreatesLoginableCustomer` | 16-year-old rejected, adult can log in | Pass |
| 10 | Account ownership | `customerCannotAccessAnotherCustomersAccount` | deposit into another customer's account rejected | Pass |
| 11 | Invalid login (GUI) | `invalidLoginShowsErrorMessage` | error shown, stays on login screen | Pass |
| 12 | Admin login (GUI) | `administratorCanOpenAdminDashboard` | admin customer table shown | Pass |
| 13 | Domain rules | `AccountTest` (10 tests) | interest rates, limits, state machines, age, lockout | Pass |
| 14 | Interest run twice in one month | `interestIsPaidOnlyOncePerMonth` | second run pays nothing | Pass |
| 15 | Customer locked while logged in | `lockedCustomerCannotMoveFundsEvenWhileLoggedIn` | deposit rejected | Pass |
| 16 | Amount parsing | `centAmountsAreAcceptedAndHugeOrExponentAmountsRejected` | 0.29 accepted; 1e16, >1 billion, 3 decimals rejected | Pass |
| 17 | Delete customer that has accounts | `customerWithAccountsCannotBeDeleted` | rejected, customer kept | Pass |
| 18 | Password storage | `passwordsAreStoredAsSaltedPbkdf2Hashes` | salted PBKDF2, unique per user | Pass |
| 19 | Legacy SHA-256 password hashes | `PasswordUtilTest` (2 tests) | still verify, flagged and rehashed to PBKDF2 on login | Pass |
