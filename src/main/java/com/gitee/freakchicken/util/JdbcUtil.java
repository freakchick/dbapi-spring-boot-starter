package com.gitee.freakchicken.util;

import com.alibaba.druid.pool.DruidPooledConnection;
import com.alibaba.druid.util.JdbcConstants;
import com.alibaba.fastjson.JSONObject;
import com.gitee.freakchicken.entity.DataSource;
import com.gitee.freakchicken.entity.ResponseDto;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class JdbcUtil {

    public static Connection getConnection(DataSource ds) throws SQLException, ClassNotFoundException {
        String url = ds.getUrl();
        switch (ds.getType()) {
            case JdbcConstants.MYSQL:
                Class.forName("com.mysql.jdbc.Driver");
                break;
            case JdbcConstants.POSTGRESQL:
                Class.forName("org.postgresql.Driver");
                break;
            case JdbcConstants.HIVE:
                Class.forName("org.apache.hive.jdbc.HiveDriver");
                break;
            case JdbcConstants.SQL_SERVER:
                Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
                break;
            default:
                break;
        }

        Connection connection = DriverManager.getConnection(url, ds.getUsername(), ds.getPassword());
        log.info("获取连接成功");
        return connection;
    }

    public static ResponseDto executeSql(int isSelect, DataSource datasource, String sql, List<Object> jdbcParamValues) {
        log.info("sql:\n" + sql);
        DruidPooledConnection connection = null;
        try {

            connection = PoolManager.getPooledConnection(datasource);
            PreparedStatement statement = connection.prepareStatement(sql);
            //参数注入
            for (int i = 1; i <= jdbcParamValues.size(); i++) {
                statement.setObject(i, jdbcParamValues.get(i - 1));
            }

            if (isSelect == 1) {
                ResultSet rs = statement.executeQuery();

                int columnCount = rs.getMetaData().getColumnCount();

                List<String> columns = new ArrayList<>();
                for (int i = 1; i <= columnCount; i++) {
                    String columnName = rs.getMetaData().getColumnLabel(i);
                    columns.add(columnName);
                }
                List<JSONObject> list = new ArrayList<>();
                while (rs.next()) {
                    JSONObject jo = new JSONObject();
                    columns.stream().forEach(t -> {
                        try {
                            Object value = rs.getObject(t);
                            jo.put(t, value);
                        } catch (SQLException throwables) {
                            throwables.printStackTrace();
                        }
                    });
                    list.add(jo);
                }
                return ResponseDto.apiSuccess(list);
            } else {
                int rs = statement.executeUpdate();
                return ResponseDto.apiSuccess("sql修改数据行数： " + rs);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseDto.fail(e.getMessage());
        } finally {
            try {
                connection.close();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        }
    }

}