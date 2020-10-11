package com.atguigu.gulimall.order.config;

import com.zaxxer.hikari.HikariDataSource;
import io.seata.rm.datasource.DataSourceProxy;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;

/**
 * <p>Title: MySeataConfig</p>
 * 配置自己的数据源
 * Description：seata配置文件 使用代理 代理自己的数据源
 */
//@Configuration
public class MySeataConfig {

	@Bean
	public DataSource dataSource(DataSourceProperties dataSourceProperties){
		HikariDataSource dataSource = dataSourceProperties.initializeDataSourceBuilder().type(HikariDataSource.class).build();
		if(StringUtils.hasText(dataSourceProperties.getName())){
			dataSource.setPoolName(dataSourceProperties.getName());
		}
		return new DataSourceProxy(dataSource);
	}
}
