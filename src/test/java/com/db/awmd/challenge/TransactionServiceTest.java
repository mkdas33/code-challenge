package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.domain.Transaction;
import com.db.awmd.challenge.exception.InvalidArgumentException;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransactionService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
public class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountsService accountsService;

    private final String accFromId = "Id-123";
    private final String accToId = "Id-124";

    private Account createFromAccount(){
        Account account = new Account(accFromId);
        account.setBalance(new BigDecimal(10000));
        accountsService.createAccount(account);
        return account;
    }
    private Account createToAccount(){
        Account account = new Account(accToId);
        account.setBalance(new BigDecimal(500));
        accountsService.createAccount(account);
        return account;
    }

    @Test
    public void transfer_multipleTransaction() throws InterruptedException {
        Account accFrom = createFromAccount();
        Account accTo = createToAccount();
        Transaction transaction = new Transaction(accFrom.getAccountId(), accTo.getAccountId(), new BigDecimal(500));

            for (int i=0; i<10; i++){
                Runnable runnable = () -> {transactionService.transferMoney(transaction);};
                Thread thread = new Thread(runnable);
                thread.start();
            }
            Thread.sleep(1000);
            assertThat(accFrom.getBalance()).isEqualTo("5000");
            assertThat(accTo.getBalance()).isEqualTo("5500");
    }
    @Test
    public void transfer_FailInvalidAccountToId(){
        Account accFrom = createFromAccount();
        Transaction ts = new Transaction(accFrom.getAccountId(), accToId, new BigDecimal(500));
        try {
            transactionService.transferMoney(ts);
        }catch (InvalidArgumentException ex){
            assertThat(ex.getMessage()).isEqualTo("Invalid account Id " + accToId);
        }
    }


}
