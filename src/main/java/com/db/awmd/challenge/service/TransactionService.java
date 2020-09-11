package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transaction;
import com.db.awmd.challenge.exception.InvalidArgumentException;
import com.db.awmd.challenge.exception.TransactionFailureException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

@Slf4j
@Service
public class TransactionService {

    private final AccountsService accountService;
    private final TransactionServiceHelper helper;

    private final Lock withdrawLock;
    private final Lock depositLock;

    public TransactionService(AccountsService accountService) {
        this.accountService = accountService;
        this.withdrawLock = new ReentrantLock();
        this.depositLock = new ReentrantLock();
        this.helper = new TransactionServiceHelper();
    }

    public void transferMoney(final Transaction transaction) {
        boolean isWithdrawSuccess = withdraw(transaction.getAccountFromId(), transaction.getAmount());
        if (isWithdrawSuccess) {
            deposit(transaction.getAccountToId(), transaction.getAmount());
        }
    }

    private Account getAccount(String accountId){
        Account account = accountService.getAccount(accountId);
        if (null == account){
            log.error("Invalid account, No account found for account id {}", accountId);
            throw new InvalidArgumentException("Invalid account Id "+ accountId);
        }
        return account;
    }

    /**
     * Try to withdraw the requested amount from the account and notifies the user on successful transaction
     * @param accountId
     * @param amount
     * @return
     */
    public boolean withdraw(String accountId, BigDecimal amount) {
        if(amount.intValue() <= 0){
            throw new InvalidArgumentException("Invalid withdrawl amount [" + amount + "], amount must be positive");
        }
        boolean isSuccess = withdrawFromAccount(accountId, amount);
        if (isSuccess) {
            log.info("Withdraw success of amount {} from account {}, Notifying money transfer", amount, accountId);
            notifyTransfer(accountId, String.format("Account debited with amount %s", amount));
        }
        return isSuccess;
    }

    public void deposit(String accountId, BigDecimal amount) {
        boolean isSuccess = depositToAccount(accountId, amount);
        if (isSuccess) {
            log.info("Deposit success of amount {} from account {}, Notifying money transfer", amount, accountId);
            notifyTransfer(accountId, String.format("Account credited with amount %s", amount));
        }

    }

    /**
     * Withdraw amount operation is performed within a thread lock context, the time out (5sec) is set to avoid any dead lock situation.
     * Upon a failure operation the transaction is rolled back
     * @param accountId
     * @param amount
     * @return
     */
    private boolean withdrawFromAccount(String accountId, BigDecimal amount) {
        boolean isSuccess = false;
        try {
            if (withdrawLock.tryLock(5, TimeUnit.SECONDS)) {
                Account account = getAccount(accountId);
                BigDecimal currentBalance = account.getBalance();
                checkBalance(amount, account, currentBalance);
                try {
                    BigDecimal updatedBalance = currentBalance.subtract(amount);
                    account.setBalance(updatedBalance);
                    isSuccess = true;
                } catch (Exception e) {
                    rollbackTransaction(account, currentBalance);
                }
            }else{
                throw new TransactionFailureException("Server busy, please try again later");
            }

        } catch (InterruptedException e) {
            log.error("Withdraw failed for account {}, Error: {}", accountId, e.getMessage());
            throw new TransactionFailureException("Server busy, please try again later", e.getCause());
        } finally {
            withdrawLock.unlock();
        }
        return isSuccess;
    }

    private void checkBalance(BigDecimal amount, Account account, BigDecimal currentBalance) {
        if (amount.compareTo(currentBalance) > 0) {
            log.error("Withdraw amount {} failed on account {} ", amount, account);
            throw new InvalidArgumentException("Withdraw failed, Insufficient balance");
        }
    }

    /**
     * Deposit operation is performed only for successful withdraw operation. Upon any failure the transaction is rolled back
     * @param accountId
     * @param amount
     * @return
     */
    private boolean depositToAccount(String accountId, BigDecimal amount) {
        boolean isSuccess = false;
        try {
            if (depositLock.tryLock(5, TimeUnit.SECONDS)) {
                Account account = getAccount(accountId);
                BigDecimal originalBalance = account.getBalance();
                try {
                    BigDecimal updatedBalance = originalBalance.add(amount);
                    account.setBalance(updatedBalance);
                    isSuccess = true;
                } catch (Exception e) {
                    rollbackTransaction(account, originalBalance);
                }
            }
        } catch (InterruptedException e) {
            log.error("Deposit failed for account {}, Error: {}", accountId, e.getMessage());
            throw new TransactionFailureException("Server busy, please try again later", e.getCause());
        }finally {
            depositLock.unlock();
        }
        return isSuccess;
    }

    private void rollbackTransaction(Account account, BigDecimal originalBalance) {
        account.setBalance(originalBalance);
    }


    private void notifyTransfer(final String accountId, final String message) {
        helper.notifyUser(getAccount(accountId), message);
    }

}
