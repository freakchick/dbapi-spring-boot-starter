# dbApi-spring-boot-starter

## 概述
- dbApi-spring-boot-starter 是接口快速开发工具，可以极大的降低代码量，类似于mybatis-plus框架，不需要再编写mapper接口、resultMap、resultType、javaBean(数据库表对应的java实体)
- 通过xml编写sql和数据库配置，可以快速开发接口，支持多数据源，支持动态sql，支持mysql/pg/hive/sqlserver
- dbApi-spring-boot-starter 是[DBApi开源框架](https://github.com/freakchick/dbApi-spring-boot-starter) 的spring boot集成

## 对比mybatis优劣
- 如果使用mybatis框架的话，我们要编写 mapper java接口、mapper.xml、数据库表对应的javaBean实体类。
当join查询的时候还要封装resultMap(xml)和java dto实体类。
- 如果使用本框架，相当于只需要编写mapper.xml中的sql脚本，参数类型返回类型都是自动的。极大的减少代码量。
## 适用场景
- 接口中没有复杂逻辑，都是sql执行，尤其适用于报表类应用
- 需要多种数据源

## 引入依赖
```xml
<dependency>
    <groupId>com.gitee.freakchicken</groupId>
    <artifactId>dbApi-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
```

## 使用案例
- 新建一个springboot的web项目，pom.xml中引入依赖
```xml
<dependency>
    <groupId>com.gitee.freakchicken</groupId>
    <artifactId>dbApi-spring-boot-starter</artifactId>
    <version>1.1.0</version>
</dependency>
<!--需要引入数据库的jdbc驱动-->
<dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>5.1.34</version>
</dependency>
```

- 在`resources`目录下创建数据库地址配置文件`ds.xml`
```xml
<datasource>
    <ds id="mysql">
        <!--注意xml下&符号要写成&amp;-->
        <url>jdbc:mysql://localhost:3306/story?useSSL=false&amp;characterEncoding=UTF-8</url>
        <username>root</username>
        <password>root</password>
    </ds>

    <ds id="local_mysql">
        <url>jdbc:mysql://localhost:3306/story?useSSL=false&amp;characterEncoding=UTF-8</url>
        <username>root</username>
        <password>root</password>
    </ds>

</datasource>
```

- 在`resources`目录下创建`sql`目录，并在`sql`目录下新建两个文件`user.xml` `user2.xml`
- user.xml
```xml
<sql>
    <defaultDB>mysql</defaultDB>

    <sql id="getUser">
        select * from student
    </sql>

    <sql id="getUserIn" db="local_mysql">
        select * from user where id in
        <foreach collection="ids" open="(" close=")" separator=",">
            #{item}
        </foreach>

    </sql>

</sql>
```

- user2.xml
```xml
<sql>
    <defaultDB>mysql</defaultDB>

    <sql id="getUserById">
        select * from student where id = #{id}
    </sql>

    <sql id="createStudent">
        insert into student (name,age) values (#{name},#{age})
    </sql>

</sql>
```

- 在`application.properties`中配置xml地址
```properties
dbapi.config.datasource=classpath:ds.xml
dbapi.config.sql=classpath:sql/*.xml
```

- 新建controller，注入DBApi，通过DBApi就可以执行sql
```java
package com.demo.controller;


import com.alibaba.fastjson.JSONObject;
import com.gitee.freakchicken.DBApi;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @program: starterDemo
 * @description:
 * @author: jiangqiang
 * @create: 2021-03-11 11:42
 **/
@Slf4j
@RequestMapping("/student")
@RestController
public class HomeController {

    @Autowired
    DBApi dbApi;

    @RequestMapping("/getAll")
    public List<JSONObject> getAllStudent() {
        List<JSONObject> jsonObjects = dbApi.executeQuery(null, "user", "getUser");
        return jsonObjects;
    }

    @RequestMapping("/getById")
    public List<JSONObject> getStudentById(Integer id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        List<JSONObject> jsonObjects = dbApi.executeQuery(map, "user2", "getUserById");
        return jsonObjects;
    }

    @RequestMapping("/getById2")
    public List<Student> getStudentById2(Integer id) {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        List<Student> list = dbApi.executeQuery(map, "user2", "getUserById", Student.class);
        return list;
    }

    @RequestMapping("/add")
    public Integer add(String name, Integer age) {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("age", age);
        int i = dbApi.executeUpdate(map, "user2", "createStudent");
        return i;
    }

}

```

- 案例完整代码请查看 [dbApi-spring-boot-starter-demo](https://gitee.com/freakchicken/dbApi-spring-boot-starter-demo.git)

## xml配置详解
### 数据库配置
支持多数据源，使用ds标签来指定，
ds标签有个id属性，值是任意字符串，这个id必须全局唯一，sql配置的时候会指定db属性，也就是指向这个id
```xml
<datasource>
    <ds id="">
        <url></url>
        <username></username>
        <password></password>
    </ds>
</datasource>
```

### sql配置

- 类似mybatis的语法，使用 select、update、insert、delete标签，
标签上有id和db两个属性，id必须全局唯一，DBApi执行的时候根据这个id查找到sql内容，sql内容是动态sql，语法和mybatis一样
db属性指定了数据库地址的id，必须在数据库配置的xml中能找到，也就是这个sql使用db对应的数据库来执行
- defaultDB配置默认db，如果配置了该选项，那么select、update、insert、delete标签可以不用加db属性

```xml
<sql>
    <defaultDB></defaultDB>
    <select id="" db="">
    
    </select>
    
    <update id="" db="">
    
    </update>
    
    <insert id="" db="">
    
    </insert>
    
    <delete id="" db="">
    
    </delete>
</sql>
```

## 联系作者：
### wechat：
<div style="text-align: center"> 
<img src="https://freakchicken.gitee.io/images/kafkaui/wechat.jpg" width = "30%" />
</div>


### 捐赠：
如果您喜欢此项目，请给捐助作者一杯咖啡
<div style="text-align: center"> 
<img src="https://freakchicken.gitee.io/images/kafkaui/wechatpay.jpg" width = "30%" />
<img src="https://freakchicken.gitee.io/images/kafkaui/alipay.jpg" width = "33%" />
</div>