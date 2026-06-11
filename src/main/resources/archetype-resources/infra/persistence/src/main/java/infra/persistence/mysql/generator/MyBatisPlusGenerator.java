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
        // get generate dao layer configuration
        InputStream inputStream = Files.newInputStream(Paths.get("./infra/src/main/resources/mybatis-plus.yml"));
        ymlProperty.load(inputStream);

        // build data configuration
        DataSourceConfig.Builder databaseBuilder = new DataSourceConfig.Builder(
                ymlProperty.getProperty("url"),
                ymlProperty.getProperty("username"),
                ymlProperty.getProperty("password")
        )
                .dbQuery(new MySqlQuery())
                .typeConvert(new MySqlTypeConvert())
                .keyWordsHandler(new MySqlKeyWordsHandler());

        FastAutoGenerator.create(databaseBuilder)
                // dao layer code path
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
                // package configuration
                .packageConfig(builder ->
                        builder.parent(ymlProperty.getProperty("parent-package"))
                        .service("dao") // configuration dao package
                        .serviceImpl("dao.impl") // configuration dao implementation package
                        .entity("po") // configuration PO package
                        // Mapper xml
                        .pathInfo(Collections.singletonMap(OutputFile.xml, ymlProperty.getProperty("mapper-path")))
                        .build()
                )
                .strategyConfig((scanner, builder) -> {
                    String tableName = scanner.apply(" table ");
                    String clazz = capitalize(scanner.apply(" class "));

                    builder.enableSkipView() // enable skip view
                            .addInclude(tableName) // package of table
                            .build()

                            // generate Controller
                            .controllerBuilder()
                            .disable()
                            .build()

                            // dao
                            .serviceBuilder()
                            .superServiceClass(BaseDao.class)
                            .superServiceImplClass(BaseDaoImpl.class)
                            .convertServiceFileName(entityName -> clazz + "Dao") // format configuration class interface file
                            .convertServiceImplFileName(entityName -> clazz + "DaoImpl") // format datainterface implementation class file
                            .build()

                            // po
                            .entityBuilder()
                            .superClass(BasePO.class) // define BasePO
                            .enableLombok() // Lombok @Getter @Setter
                            .enableTableFieldAnnotation() // TableField
                            .naming(NamingStrategy.underline_to_camel) // table underscore
                            .columnNaming(NamingStrategy.underline_to_camel) // table field underscore
                            .convertFileName(entityName -> clazz + "PO")
                            .disableSerialVersionUID()
                            .enableFileOverride() // override PO
                            .build()

                            // mapper
                            .mapperBuilder()
                            .superClass(BaseMapper.class) // configuration mapper class
                            .enableBaseResultMap() // enable generate mapper xml common query mapping result
                            .convertMapperFileName(entityName -> clazz + "Mapper")
                            .convertXmlFileName(entityName -> clazz + "Mapper")
                            .enableFileOverride()
                            .build();
                })
                .templateEngine(new FreemarkerTemplateEngine())
                .execute();
    }
}
