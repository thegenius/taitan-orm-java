package com.lvonce.taitan;

import javax.sql.DataSource;

public interface DataSourceProvider {
    DataSource getDataSource();
}