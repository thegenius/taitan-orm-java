package org.acme;

import com.google.auto.service.AutoService;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import com.lvonce.taitan.DataSourceProvider;
import javax.sql.DataSource;


import org.eclipse.microprofile.config.ConfigProvider;

@AutoService(DataSourceProvider.class)
public class HikariDataSourceProvider implements DataSourceProvider {

    private static final DataSource dataSource;

    static {
        String url = ConfigProvider.getConfig().getValue("jdbc.url", String.class);
        String username = ConfigProvider.getConfig().getValue("jdbc.username", String.class);
        String password = ConfigProvider.getConfig().getValue("jdbc.password", String.class);
        System.out.println("url=" + url);
        System.out.println("username=" + username);
        System.out.println("password=" + password);


        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(url);
        config.setUsername(username);
        config.setPassword(password);
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(5);
        config.setIdleTimeout(30000);
        config.setMaxLifetime(1200000);
        config.setConnectionTimeout(20000);
        config.setPoolName("HikariCP-Pool");
        dataSource = new HikariDataSource(config);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }
}