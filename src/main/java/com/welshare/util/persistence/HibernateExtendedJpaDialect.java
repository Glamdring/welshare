package com.welshare.util.persistence;

import java.sql.Connection;
import java.sql.SQLException;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceException;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.orm.jpa.vendor.HibernateJpaDialect;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionException;

/**
 * A JpaDialect to support custom isolation levels on the JDBC level.
 * Uses spring utilities which are also used by the HibernateTransactionManager
 * to prepare the jdbc connection properly.
 *
 * TODO verify various use cases:
 *  multiple connections per session, multiple transactions per session, etc.
 *
 * @author Bozhidar Bozhanov
 *
 */
public class HibernateExtendedJpaDialect extends HibernateJpaDialect {

    private static final long serialVersionUID = -4580866857367037144L;

    @Override
    public Object beginTransaction(EntityManager entityManager,
            final TransactionDefinition definition) throws PersistenceException,
            SQLException, TransactionException {

        Session session = entityManager.unwrap(Session.class);
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                DataSourceUtils.prepareConnectionForTransaction(connection, definition);
                if (connection.isReadOnly() && !definition.isReadOnly()) {
                    connection.setReadOnly(false);

                }
            }
        });

        entityManager.getTransaction().begin();

        return prepareTransaction(entityManager, definition.isReadOnly(), definition.getName());
    }

}
