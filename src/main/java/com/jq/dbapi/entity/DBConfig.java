package com.jq.dbapi.entity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @program: dbApi-starter
 * @description:
 * @author: jiangqiang
 * @create: 2021-03-11 10:49
 **/
@Data
@ConfigurationProperties("dbapi.config")
public class DBConfig {
    String sql;

    String datasource;


}
