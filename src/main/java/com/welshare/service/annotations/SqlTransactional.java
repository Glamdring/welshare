package com.welshare.service.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Retention(RetentionPolicy.RUNTIME)
@Transactional(value="transactionManager", isolation=Isolation.READ_UNCOMMITTED, propagation=Propagation.REQUIRED)
public @interface SqlTransactional {

}
