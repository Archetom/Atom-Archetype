package ${package}.infra.persistence.config;

import com.baomidou.mybatisplus.annotation.DbType;
import com.baomidou.mybatisplus.core.handlers.MetaObjectHandler;
import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.PaginationInnerInterceptor;
import org.apache.ibatis.reflection.MetaObject;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * @author hanfeng
 */
@Configuration
@MapperScan(basePackages ="${package}.infra.persistence.mapper", sqlSessionTemplateRef = "sqlSessionTemplate")
public class MyBatisPlusConfig {

    @Bean
    public MybatisPlusInterceptor mybatisPlusInterceptor() {
        MybatisPlusInterceptor interceptor = new MybatisPlusInterceptor();
        interceptor.addInnerInterceptor(new PaginationInnerInterceptor(DbType.MYSQL));
        return interceptor;
    }

    @Component
    static class MyMetaObjectHandler implements MetaObjectHandler {
        //使用mp实现添加操作，这个方法执行
        @Override
        public void insertFill(MetaObject metaObject) {
            this.setFieldValByName("createdTime", LocalDateTime.now(), metaObject);
            this.setFieldValByName("updatedTime", LocalDateTime.now(), metaObject);
        }

        //使用mp实现修改操作，这个方法执行
        @Override
        public void updateFill(MetaObject metaObject) {
            this.setFieldValByName("updatedTime", LocalDateTime.now(), metaObject);
        }
    }
}
