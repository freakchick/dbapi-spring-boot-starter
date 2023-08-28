package com.gitee.freakchicken;

import com.alibaba.fastjson.JSONObject;
import com.gitee.freakchicken.entity.DBConfig;
import com.gitee.freakchicken.entity.DataSource;
import com.gitee.freakchicken.entity.SqlNode;
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

    Map<String, Map<String, SqlNode>> sqlMap = new HashMap<>();
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
                    Map<String, SqlNode> stringSqlMap = XmlParser.parseSql(content);
                    this.sqlMap.put(file.getName().split("\\.")[0], stringSqlMap);
                    logger.info("DBApi register sql xml: {}", file.getName());
                }
            }

            File dsFile = ResourceUtils.getFile(this.dbConfig.getDatasource());
            String dsText = FileUtils.readFileToString(dsFile, StandardCharsets.UTF_8);
            this.dataSourceMap = XmlParser.parseDatasource(dsText);
            logger.info("DBApi register datasource xml: {}", dsFile.getName());
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
        }
    }

    /**
     * execute select sql with parameters, return a list of java bean entity
     *
     * @param namespace name of xml file
     * @param sqlId     sql id in <sql> tag
     * @param data      sql parameters
     * @param clazz     class of java bean entity
     * @return
     */
    public <T> List<T> executeQueryEntity(String namespace, String sqlId, Map<String, Object> data, Class<T> clazz) {
        List<JSONObject> list = executeQuery(namespace, sqlId, data);

        List<T> collect = list.stream().map(t -> t.toJavaObject(clazz))
                .collect(Collectors.toList());
        return collect;
    }

    /**
     * execute select sql with parameters, return a list of JSONObject
     *
     * @param namespace name of xml file
     * @param sqlId     sql id in <sql> tag
     * @param data      sql parameters
     * @return
     */
    public List<JSONObject> executeQuery(String namespace, String sqlId, Map<String, Object> data) {
        try {
            if (!sqlMap.containsKey(namespace)) {
                throw new RuntimeException("namespace not found : " + sqlId);
            } else {
                if (!sqlMap.get(namespace).containsKey(sqlId)) {
                    throw new RuntimeException("sqlId not found : " + sqlId);
                } else {
                    SqlNode sql = this.sqlMap.get(namespace).get(sqlId);
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

    /**
     * execute select sql without parameters, return a list of JSONObject
     *
     * @param namespace name of xml file
     * @param sqlId     sql id in <sql> tag
     * @return
     */
    public List<JSONObject> executeQuery(String namespace, String sqlId) {
        return executeQuery(namespace, sqlId, null);
    }

    /**
     * execute select sql without parameters, return a list of java bean entity
     *
     * @param namespace name of xml file
     * @param sqlId     sql id in <sql> tag
     * @param clazz     class of java bean entity
     * @return
     */
    public <T> List<T> executeQueryEntity(String namespace, String sqlId, Class<T> clazz) {
        return executeQueryEntity(namespace, sqlId, null, clazz);
    }

    /**
     * execute insert/delete/update sql with parameters
     *
     * @param namespace name of xml file
     * @param sqlId     sql id in <sql> tag
     * @param data      sql parameters
     * @return
     */
    public int executeUpdate(String namespace, String sqlId, Map<String, Object> data) {
        try {
            if (!sqlMap.containsKey(namespace)) {
                throw new RuntimeException("namespace not found : " + sqlId);
            } else {
                if (!sqlMap.get(namespace).containsKey(sqlId)) {
                    throw new RuntimeException("sqlId not found : " + sqlId);
                } else {
                    SqlNode sql = this.sqlMap.get(namespace).get(sqlId);
                    if (!dataSourceMap.containsKey(sql.getDatasourceId())) {
                        throw new RuntimeException("datasource not found : " + sql.getDatasourceId());
                    }
                    DataSource dataSource = dataSourceMap.get(sql.getDatasourceId());
                    SqlMeta sqlMeta = dynamicSqlEngine.parse(sql.getText(), data);

                    return JdbcUtil.executeUpdate(dataSource, sqlMeta.getSql(), sqlMeta.getJdbcParamValues());
                }
            }
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage());
        }
    }

    /**
     * execute insert/delete/update sql without parameters
     *
     * @param namespace name of xml file
     * @param sqlId     sql id in <sql> tag
     * @return
     */
    public int executeUpdate(String namespace, String sqlId) {
        return executeUpdate(namespace, sqlId, null);
    }
}