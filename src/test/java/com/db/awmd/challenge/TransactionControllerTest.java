package com.db.awmd.challenge;

import com.db.awmd.challenge.domain.Account;
import com.db.awmd.challenge.service.AccountsService;
import com.db.awmd.challenge.service.TransactionService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.context.WebApplicationContext;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.webAppContextSetup;

@RunWith(SpringRunner.class)
@SpringBootTest
@WebAppConfiguration
public class TransactionControllerTest {

    private MockMvc mockMvc;

    @Autowired
    private TransactionService transactionService;

    @Autowired
   // @Mock
    private AccountsService accountsService;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Before
    public void prepareMockMvc() {
        this.mockMvc = webAppContextSetup(this.webApplicationContext).build();
        accountsService.getAccountsRepository().clearAccounts();
        createFromAndToAccounts();
    }

    private void createFromAndToAccounts() {
        accountsService.createAccount(new Account("Id-123", new BigDecimal(1500)));
        accountsService.createAccount(new Account("Id-124", new BigDecimal(500)));
    }

    @Test
    public void transferMoney() throws Exception {
        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
               .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-124\",\"amount\":500}")).andExpect(status().isAccepted());

        Account accountFrom = accountsService.getAccount("Id-123");
        Account accountTo = accountsService.getAccount("Id-124");
        assertThat(accountFrom.getBalance()).isEqualTo("1000");
        assertThat(accountTo.getBalance()).isEqualTo("1000");
    }
    @Test
    public void transferMultipleTransaction() throws Exception {
        Account accountFrom = accountsService.getAccount("Id-123");
        Account accountTo = accountsService.getAccount("Id-124");

        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-124\",\"amount\":500}")).andExpect(status().isAccepted());


        assertThat(accountFrom.getBalance()).isEqualTo("1000");
        assertThat(accountTo.getBalance()).isEqualTo("1000");

        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-124\",\"amount\":500}")).andExpect(status().isAccepted());


        assertThat(accountFrom.getBalance()).isEqualTo("500");
        assertThat(accountTo.getBalance()).isEqualTo("1500");
    }


    @Test
    public void transferInsufficientBalance() throws Exception{
        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-124\",\"amount\":2000}")).andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyBlankAccountFromId() throws Exception{
        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"\",\"accountToId\":\"Id-124\",\"amount\":1500}")).andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyBlankAccountToId() throws Exception{
        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"\",\"amount\":1500}")).andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyZeroAmount() throws Exception{
        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"Id-123\",\"accountToId\":\"Id-124\",\"amount\":0}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void transferMoneyInvalidAccountTo() throws Exception{
        this.mockMvc.perform(post("/v1/transactions").contentType(MediaType.APPLICATION_JSON)
                .content("{\"accountFromId\":\"Id-1234\",\"accountToId\":\"Id-124\",\"amount\":500}"))
                .andExpect(status().isBadRequest());
    }

}
