package com.gitee.freakchicken;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import com.gitee.freakchicken.entity.DBConfig;
import com.gitee.freakchicken.entity.DataSource;
import com.gitee.freakchicken.entity.Sql;
import com.gitee.freakchicken.util.JdbcUtil;
import com.gitee.freakchicken.util.XmlParser;
import com.github.freakchick.orange.SqlMeta;
import com.github.freakchick.orange.engine.DynamicSqlEngine;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.util.ResourceUtils;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @program: dbApi-starter
 * @description:
 * @author: jiangqiang
 * @create: 2021-03-11 11:21
 **/
public class DBApi {

    private static Logger logger = LoggerFactory.getLogger(DBApi.class);

    DynamicSqlEngine dynamicSqlEngine = new DynamicSqlEngine();

    DBConfig dbConfig;

    Map<String, Map<String, Sql>> sqlMap = new HashMap<>();
    Map<String, DataSource> dataSourceMap;

    public DBApi(DBConfig dbConfig) {
        this.dbConfig = dbConfig;

        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(this.dbConfig.getSql());
            for (Resource res : resources) {
                File file = res.getFile();
                if (file.isFile()) {

                    String content = FileUtils.readFileToString(file, StandardCharsets.UTF_8);
                    Map<String, Sql> stringSqlMap = XmlParser.parseSql(content);
                    this.sqlMap.put(file.getName().split("\\.")[0], stringSqlMap);
                    logger.info("DBApi register sql xml: {}", file.getName());
                }
            }

            File dsFile = ResourceUtils.getFile(this.dbConfig.getDatasource());
            String dsText = FileUtils.readFileToString(dsFile, StandardCharsets.UTF_8);
            this.dataSourceMap = XmlParser.parseDatasource(dsText);
            logger.info("DBApi register datasource xml: {}", dsFile.getName());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public <T> List<T> executeQuery(Map<String, Object> data, String namespace, String sqlId, Class<T> clazz) {
        List<JSONObject> list = executeQuery(data, namespace, sqlId);
        List<T> collect = list.stream().map(t -> JSON.parseObject(t.toJSONString(), clazz)).collect(Collectors.toList());
        return collect;
    }

    public List<JSONObject> executeQuery(Map<String, Object> data, String namespace, String sqlId) {
        try {
            if (!sqlMap.containsKey(namespace)) {
                throw new RuntimeException("namespace not found : " + sqlId);
            } else {
                if (!sqlMap.get(namespace).containsKey(sqlId)) {
                    throw new RuntimeException("sqlId not found : " + sqlId);
                } else {
                    Sql sql = this.sqlMap.get(namespace).get(sqlId);
                    if (!dataSourceMap.containsKey(sql.getDatasourceId())) {
                        throw new RuntimeException("datasource not found : " + sql.getDatasourceId());
                    }
                    DataSource dataSource = dataSourceMap.get(sql.getDatasourceId());
                    SqlMeta sqlMeta = dynamicSqlEngine.parse(sql.getText(), data);

                    return JdbcUtil.executeQuery(dataSource, sqlMeta.getSql(), sqlMeta.getJdbcParamValues());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    public int executeUpdate(Map<String, Object> data, String namespace, String sqlId) {
        try {
            if (!sqlMap.containsKey(namespace)) {
                throw new RuntimeException("n