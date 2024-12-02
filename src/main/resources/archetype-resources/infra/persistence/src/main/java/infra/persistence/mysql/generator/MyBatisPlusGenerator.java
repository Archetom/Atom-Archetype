package ${package}.infra.persistence.mysql.generator;

import com.baomidou.mybatisplus.generator.FastAutoGenerator;
import com.baomidou.mybatisplus.generator.config.DataSourceConfig;
import com.baomidou.mybatisplus.generator.config.OutputFile;
import com.baomidou.mybatisplus.generator.config.converts.MySqlTypeConvert;
import com.baomidou.mybatisplus.generator.config.querys.MySqlQuery;
import com.baomidou.mybatisplus.generator.config.rules.DbColumnType;
import com.baomidou.mybatisplus.generator.config.rules.NamingStrategy;
import com.baomidou.mybatisplus.generator.engine.FreemarkerTemplateEngine;
import com.baomidou.mybatisplus.generator.keywords.MySqlKeyWordsHandler;
import ${package}.infra.persistence.mysql.dao.BaseDao;
import ${package}.infra.persistence.mysql.dao.impl.BaseDaoImpl;
import ${package}.infra.persistence.mysql.mapper.BaseMapper;
import ${package}.infra.persistence.mysql.po.BasePO;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.Types;
import java.util.Arrays;
import java.util.Collections;
import java.util.Properties;

import static org.springframework.util.StringUtils.capitalize;

/**
 * @author hanfeng
 */
public class MyBatisPlusGenerator {
    public static void main(String[] args) throws IOException {
        Properties ymlProperty = new Properties();
        // 获取生成dao层配置
        InputStream inputStream = Files.newInputStream(Paths.get("./infra/src/main/resources/mybatis-plus.yml"));
        ymlProperty.load(inputStream);

        // 构建数据配置信息
        DataSourceConfig.Builder databaseBuilder = new DataSourceConfig.Builder(
                ymlProperty.getProperty("url"),
                ymlProperty.getProperty("username"),
                ymlProperty.getProperty("password")
        )
                .dbQuery(new MySqlQuery())
                .typeConvert(new MySqlTypeConvert())
                .keyWordsHandler(new MySqlKeyWordsHandler());

        FastAutoGenerator.create(databaseBuilder)
                // dao 层代码输出路径
                .globalConfig(builder ->
                        builder.outputDir(ymlProperty.getProperty("output-path"))
                        .disableOpenDir()
                        .build()
                )
                .dataSourceConfig(builder -> builder.typeConvertHandler((globalConfig, typeRegistry, metaInfo) -> {
                    int typeCode = metaInfo.getJdbcType().TYPE_CODE;
                    // Smallint/TinyInt -> Integer
                    if (Arrays.asList(Types.TINYINT, Types.SMALLINT).contains(typeCode)) {
                        return DbColumnType.INTEGER;
                    }

                    return typeRegistry.getColumnType(metaInfo);
                }))
                // 包名配置
                .packageConfig(builder ->
                        builder.parent(ymlProperty.getProperty("parent-package"))
                        .service("dao") // 配置 dao 包名
                        .serviceImpl("dao.impl") // 配置 dao 实现 包名
                        .entity("po") // 配置 PO 包名
                        // Mapper xml地址
                        .pathInfo(Collections.singletonMap(OutputFile.xml, ymlProperty.getProperty("mapper-path")))
                        .build()
                )
                .strategyConfig((scanner, builder) -> {
                    String tableName = scanner.apply("请输入表名");
                    String clazz = capitalize(scanner.apply("请输入类名"));

                    builder.enableSkipView() // 开启跳过视图
                            .addInclude(tableName) //增加包含的表名
                            .build()

                            // 不生成 Controller
                            .controllerBuilder()
                            .disable()
                            .build()

                            // dao
                            .serviceBuilder()
                            .superServiceClass(BaseDao.class)
                            .superServiceImplClass(BaseDaoImpl.class)
                            .convertServiceFileName(entityName -> clazz + "Dao") // 格式化 配置类 接口文件名称
                            .convertServiceImplFileName(entityName -> clazz + "DaoImpl") // 格式化 datainterface 实现类文件名称
                            .build()

                            // po
                            .entityBuilder()
                            .superClass(BasePO.class) // 自定义继承 BasePO
                            .enableLombok() // 增加 Lombok @Getter @Setter 注解
                            .enableTableFieldAnnotation() // 增加 TableField 注解
                            .naming(NamingStrategy.underline_to_camel) // 表名 下划线转驼峰命名
                            .columnNaming(NamingStrategy.underline_to_camel) // 表字段 下划线转驼峰命名
                            .convertFileName(entityName -> clazz + "PO")
                            .disableSerialVersionUID()
                            .enableFileOverride() // 覆盖 PO
                            .build()

                            // mapper
                            .mapperBuilder()
                            .superClass(BaseMapper.class) // 配置 mapper 父类
                            .enableBaseResultMap() // 开启 生成 mapper xml 通用查询映射结果
                            .convertMapperFileName(entityName -> clazz + "Mapper")
                            .convertXmlFileName(entityName -> clazz + "Mapper")
                            .enableFileOverride()
                            .build();
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
