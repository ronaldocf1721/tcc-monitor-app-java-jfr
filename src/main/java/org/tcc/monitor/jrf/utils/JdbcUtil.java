package org.tcc.monitor.jrf.utils;

import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.stereotype.Component;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Consumer;

@Component
public class JdbcUtil {

    @PersistenceContext
    private EntityManager entityManager;

    public void executeQuery(String sql, Consumer<PreparedStatement> preparer, Consumer<ResultSet> resultSetConsumer) {
        Session session = entityManager.unwrap(Session.class);

        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) {
                try (PreparedStatement stmt = connection.prepareStatement(sql)) {

                    if (preparer != null) {
                        preparer.accept(stmt);
                    }

                    try (ResultSet rs = stmt.executeQuery()) {
                        resultSetConsumer.accept(rs);
                    }

                } catch (SQLException e) {
                    throw new RuntimeException("Erro ao executar a consulta: " + sql, e);
                }
            }
        });
    }
}