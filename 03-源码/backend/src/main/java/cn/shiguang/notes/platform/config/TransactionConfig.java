package cn.shiguang.notes.platform.config;

import org.springframework.context.annotation.*;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Scheduled application use cases need explicit transaction scopes rather than self-invocation. */
@Configuration
public class TransactionConfig {
  @Bean
  TransactionTemplate transactionTemplate(PlatformTransactionManager manager) {
    return new TransactionTemplate(manager);
  }
}
