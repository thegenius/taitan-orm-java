package org.acme;


import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

@ApplicationScoped
public class DaoProvider {

    @Produces
    public  UserEntityDao getUserEntityDao() {
        return new UserEntityDao(new HikariDataSourceProvider().getDataSource());
    }
}
