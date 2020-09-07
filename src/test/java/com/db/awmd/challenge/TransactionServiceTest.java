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
        account.setBalance(new BigDecimal(1000));
        accountsService.createAccount(account);
        return account;
    }
    private Account createToAccount(){
        Account account = new Account(accToId);
        account.setBalance(new BigDecimal(500));
        accountsService.createAccount(account);
        return account;
    }

    private void createFromAndToAccount(){
        createFromAccount();
        createToAccount();
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
