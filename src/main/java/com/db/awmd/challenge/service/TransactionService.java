package com.db.awmd.challenge.service;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transaction;
import com.db.awmd.challenge.exception.InvalidArgumentException;
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
    private final NotificationService notificationService;
    private final Lock withdrawLock;
    private final Lock depositLock;

    public TransactionService(AccountsService accountService) {
        this.accountService = accountService;
        this.notificationService = new EmailNotificationService();
        this.withdrawLock = new ReentrantLock();
        this.depositLock = new ReentrantLock();
    }

    public void transferMoney(Transaction transaction) {
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

    public boolean withdraw(String accountId, BigDecimal amount) {
        Account account = getAccount(accountId);
        if(amount.intValue() <= 0){
            throw new InvalidArgumentException("Invalid withdrawl amount [" + amount + "], amount must be positive");
        }
        boolean isSuccess = withdrawFromAccount(account, amount);
        if (isSuccess) {
            log.info("Withdraw success of amount {} from account {}, Notifying money transfer", amount, account);
            notifyTransfer(account, String.format("Account debited with amount %s", amount));
        }
        return isSuccess;
    }

    public void deposit(String accountId, BigDecimal amount) {
        Account account = getAccount(accountId);
        boolean isSuccess = depositToAccount(account, amount);
        if (isSuccess) {
            log.info("Deposit success of amount {} from account {}, Notifying money transfer", amount, account);
            notifyTransfer(account, String.format("Account credited with amount %s", amount));
        }

    }

    private boolean withdrawFromAccount(Account account, BigDecimal amount) {
        boolean isSuccess = false;
        try {
            if (withdrawLock.tryLock(10, TimeUnit.SECONDS)) {
                BigDecimal currentBalance = account.getBalance();
                if (amount.compareTo(currentBalance) > 0) {
                    throw new InvalidArgumentException("Insufficient balance");
                }
                try {
                    BigDecimal updatedBalance = currentBalance.subtract(amount);
                    account.setBalance(updatedBalance);
                    isSuccess = true;
                } catch (Exception e) {
                    rollbackTransaction(account, currentBalance);
                }
            }

        } catch (InterruptedException e) {
            log.error("Withdraw failed for account {}, Error: {}", account.getAccountId(), e.getMessage());
            throw new RuntimeException("Server busy, please try again later");
        } finally {
            withdrawLock.unlock();
        }
        return isSuccess;
    }

    private boolean depositToAccount(Account account, BigDecimal amount) {
        boolean isSuccess = false;
        try {
            if (depositLock.tryLock(10, TimeUnit.SECONDS)) {
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
            log.error("Deposit failed for account {}, Error: {}", account.getAccountId(), e.getMessage());
            throw new RuntimeException("Server busy, please try again later");
        }
        return isSuccess;
    }

    private void rollbackTransaction(Account account, BigDecimal originalBalance) {
        account.setBalance(originalBalance);
    }


    private void notifyTransfer(Account account, String message) {
        Runnable runnable = () -> notificationService.notifyAboutTransfer(account, message);
        new Thread(runnable).start();
    }

}
