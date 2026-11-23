package ru.practicum.ewm.common.transaction;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

@Component
public class TransactionRunner {

    private PlatformTransactionManager transactionManager;

    public TransactionRunner() {
    }

    @Autowired
    public TransactionRunner(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    @Autowired
    public void setTransactionManager(PlatformTransactionManager transactionManager) {
        this.transactionManager = transactionManager;
    }

    public <T> T execute(Supplier<T> action) {
        if (transactionManager == null) {
            return action.get();
        }
        return new TransactionTemplate(transactionManager).execute(status -> action.get());
    }

    public void execute(Runnable action) {
        if (transactionManager == null) {
            action.run();
            return;
        }
        new TransactionTemplate(transactionManager).executeWithoutResult(status -> action.run());
    }
}
