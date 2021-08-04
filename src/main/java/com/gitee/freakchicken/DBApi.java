package com.gitee.freakchicken;

import com.github.freakchick.orange.SqlMeta;
import com.github.freakchick.orange.engine.DynamicSqlEngine;
import com.gitee.freakchicken.entity.DBConfig;
import com.gitee.freakchicken.entity.DataSource;
import com.gitee.freakchicken.entity.ResponseDto;
import com.gitee.freakchicken.entity.Sql;
import com.gitee.freakchicken.util.JdbcUtil;
import com.gitee.freakchicken.util.XmlParser;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * @program: dbApi-starter
 * @description:
 * @author: jiangqiang
 * @create: 2021-03-11 11:21
 **/
@Slf4j
public class DBApi {

    DynamicSqlEngine dynamicSqlEngine = new DynamicSqlEngine();

    DBConfig dbConfig;

    Map<String, Sql> sqlMap;
    Map<String, DataSource> dataSourceMap;

    public DBApi(DBConfig dbConfig) {
        this.dbConfig = dbConfig;

        try {
            File file = ResourceUtils.getFile(this.dbConfig.getSql());
            String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
            this.sqlMap = XmlParser.parseSql(content);

            File dsFile = ResourceUtils.getFile(this.dbConfig.getDatasource());
            String dsText = FileUtils.readFileToString(dsFile, StandardCharsets.UTF_8);
            this.dataSourceMap = XmlParser.parseDatasource(dsText);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public ResponseDto execute(Map<String, Object> data, String sqlId) {
        try {
            if (!sqlMap.containsKey(sqlId)) {
                return ResponseDto.fail("sql not found by id : " + sqlId);
            }
            Sql sql = this.sqlMap.get(sqlId);
            if (!dataSourceMap.containsKey(sql.getDatasourceId())) {
                return ResponseDto.fail("datasource not found : " + sql.getDatasourceId());
            }
            DataSource dataSource = dataSourceMap.get(sql.getDatasourceId());
            SqlMeta sqlMeta = dynamicSqlEngine.parse(sql.getText(), data);
            int isSelect = 0;
            if (sql.getType().equals("select")) {
                isSelect = 1;
            }

            ResponseDto responseDto = JdbcUtil.executeSql(isSelect, dataSource, sqlMeta.getSql(), sqlMeta.getJdbcParamValues());
            return responseDto;
        } catch (Exception e) {
            log.error(e.getMessage());
            return ResponseDto.fail(e.getMessage());
        }

    }

}
