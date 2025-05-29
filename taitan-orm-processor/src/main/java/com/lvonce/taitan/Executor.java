package com.lvonce.taitan;



import javax.sql.DataSource;
import java.sql.*;
import java.util.ServiceLoader;

public class Executor {
    private static DataSource dataSource;


    public Executor() {
        // 如果 dataSource 为空，则通过 SPI 初始化
        if (Executor.dataSource == null) {
            ServiceLoader<DataSourceProvider> loader = ServiceLoader.load(DataSourceProvider.class);
            DataSourceProvider provider = loader.findFirst().orElseThrow(() -> new RuntimeException("No DataSourceProvider implementation found"));
            Executor.dataSource = provider.getDataSource();
        }
    }

    public Executor(DataSource dataSource) {
        // 如果静态 dataSource 为空，则设置为传入的 dataSource
        if (Executor.dataSource == null) {
            Executor.dataSource = dataSource;
        }
    }



    protected Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    protected long executeUpdate(String sql, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            return rs.next() ? rs.getLong(1) : 0;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    protected <T> T executeQuery(String sql, ResultSetMapper<T> mapper, Object... params) {
        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            System.out.println("sql: " + sql);
            for (int i = 0; i < params.length; i++) {
                ps.setObject(i + 1, params[i]);
            }
            ResultSet rs = ps.executeQuery();
            return rs.next() ? mapper.map(rs) : null;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public interface ResultSetMapper<T> {
        T map(ResultSet rs) throws SQLException;
    }
}