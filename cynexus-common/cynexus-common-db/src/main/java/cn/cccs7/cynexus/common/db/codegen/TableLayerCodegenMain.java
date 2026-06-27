package cn.cccs7.cynexus.common.db.codegen;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

/**
 * Generate PO / BO / DTO / Converter / Mapper / MyBatis XML from PostgreSQL tables.
 */
public final class TableLayerCodegenMain {

    private static final Set<String> BASE_PO_COLUMNS = Set.of("id", "created_at", "updated_at", "deleted_at");
    private static final String JSONB_HANDLER = "cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler";

    private TableLayerCodegenMain() {
    }

    public static void main(String[] args) throws Exception {
        CodegenConfig config = CodegenConfig.parse(args);
        validate(config);

        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.username(), config.password())) {
            List<String> tables = resolveTables(connection, config);
            if (tables.isEmpty()) {
                throw new IllegalStateException("No tables found in schema: " + config.schema());
            }
            for (String table : tables) {
                TableMeta tableMeta = loadTableMeta(connection, config.schema(), table);
                generateAll(config, tableMeta);
                System.out.println("Generated table: " + config.schema() + "." + table);
            }
        }
    }

    private static void validate(CodegenConfig config) {
        require(config.jdbcUrl(), "jdbcUrl");
        require(config.username(), "username");
        require(config.password(), "password");
        require(config.schema(), "schema");
        require(config.service(), "service");
        if (config.repoRoot() == null) {
            throw new IllegalArgumentException("Missing required arg: --repoRoot=...");
        }
    }

    private static void require(String value, String key) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required arg: --" + key + "=...");
        }
    }

    private static List<String> resolveTables(Connection connection, CodegenConfig config) throws SQLException {
        if (!config.tables().isEmpty()) {
            return config.tables();
        }
        String sql = """
                select table_name
                from information_schema.tables
                where table_schema = ?
                  and table_type = 'BASE TABLE'
                order by table_name
                """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, config.schema());
            try (ResultSet rs = ps.executeQuery()) {
                List<String> tables = new ArrayList<>();
                while (rs.next()) {
                    tables.add(rs.getString("table_name"));
                }
                return tables;
            }
        }
    }

    private static TableMeta loadTableMeta(Connection connection, String schema, String tableName) throws SQLException {
        String sql = """
                select column_name,
                       data_type,
                       udt_name,
                       is_nullable,
                       ordinal_position
                from information_schema.columns
                where table_schema = ?
                  and table_name = ?
                order by ordinal_position
                """;

        List<ColumnMeta> columns = new ArrayList<>();
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, schema);
            ps.setString(2, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String columnName = rs.getString("column_name");
                    String dataType = rs.getString("data_type");
                    String udtName = rs.getString("udt_name");
                    boolean nullable = "YES".equalsIgnoreCase(rs.getString("is_nullable"));
                    columns.add(ColumnMeta.from(columnName, dataType, udtName, nullable));
                }
            }
        }
        if (columns.isEmpty()) {
            throw new IllegalStateException("No columns found for table: " + schema + "." + tableName);
        }
        return new TableMeta(schema, tableName, columns);
    }

    private static void generateAll(CodegenConfig config, TableMeta tableMeta) {
        String aggregateName = buildAggregateName(tableMeta.tableName(), config.stripPrefixes());
        String basePackage = "cn.cccs7.cynexus." + config.service();

        writeJava(
                config.infraEntityDir(),
                aggregateName + "PO.java",
                renderPo(basePackage, aggregateName, tableMeta),
                config.overwrite()
        );
        writeJava(
                config.domainEntityDir(),
                aggregateName + "BO.java",
                renderBo(basePackage, aggregateName, tableMeta),
                config.overwrite()
        );
        writeJava(
                config.apiDtoDir(),
                aggregateName + "DTO.java",
                renderDto(basePackage, aggregateName, tableMeta),
                config.overwrite()
        );
        writeJava(
                config.apiFeignDir(),
                aggregateName + "FeignClient.java",
                renderFeignClient(basePackage, aggregateName, tableMeta.tableName()),
                config.overwrite()
        );
        writeJava(
                config.infraConvertDir(),
                aggregateName + "POConverter.java",
                renderPoConverter(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.applicationConvertDir(),
                aggregateName + "DTOConverter.java",
                renderDtoConverter(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.applicationServiceDir(),
                aggregateName + "CrudAppService.java",
                renderCrudAppService(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.applicationControllerDir(),
                aggregateName + "CrudController.java",
                renderCrudController(basePackage, aggregateName, tableMeta.tableName()),
                config.overwrite()
        );
        writeJava(
                config.domainRepositoryDir(),
                aggregateName + "Repository.java",
                renderRepository(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.domainServiceDir(),
                aggregateName + "DomainService.java",
                renderDomainService(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.domainServiceImplDir(),
                aggregateName + "DomainServiceImpl.java",
                renderDomainServiceImpl(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.infraMapperDir(),
                aggregateName + "Mapper.java",
                renderMapper(basePackage, aggregateName),
                config.overwrite()
        );
        writeText(
                config.infraMapperXmlDir(),
                aggregateName + "Mapper.xml",
                renderMapperXml(basePackage, aggregateName, tableMeta),
                config.overwrite()
        );
        writeJava(
                config.infraBasicServiceDir(),
                aggregateName + "BasicService.java",
                renderBasicService(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.infraBasicServiceImplDir(),
                aggregateName + "BasicServiceImpl.java",
                renderBasicServiceImpl(basePackage, aggregateName),
                config.overwrite()
        );
        writeJava(
                config.infraRepositoryDir(),
                aggregateName + "RepositoryImpl.java",
                renderRepositoryImpl(basePackage, aggregateName),
                config.overwrite()
        );
    }

    private static String renderPo(String basePackage, String aggregateName, TableMeta tableMeta) {
        Set<String> imports = new TreeSet<>();
        imports.add("cn.cccs7.cynexus.common.db.entity.BasePO");
        imports.add("com.baomidou.mybatisplus.annotation.TableName");
        imports.add("lombok.Data");
        imports.add("lombok.EqualsAndHashCode");

        boolean hasJsonb = tableMeta.columns().stream().anyMatch(ColumnMeta::jsonb);
        boolean hasAnnotatedFields = false;
        for (ColumnMeta column : tableMeta.columns()) {
            if (BASE_PO_COLUMNS.contains(column.columnName())) {
                continue;
            }
            imports.addAll(column.imports());
            if (column.jsonb() || column.requiresTableField()) {
                hasAnnotatedFields = true;
            }
        }
        if (hasJsonb) {
            imports.add("cn.cccs7.cynexus.common.db.typehandler.JsonbTypeHandler");
        }
        if (hasAnnotatedFields) {
            imports.add("com.baomidou.mybatisplus.annotation.TableField");
        }

        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(basePackage).append(".infra.basic.entity;\n\n");
        appendImports(builder, imports);
        builder.append("@Data\n")
                .append("@EqualsAndHashCode(callSuper = true)\n");
        if (hasJsonb) {
            builder.append("@TableName(value = \"").append(tableMeta.tableName()).append("\", autoResultMap = true)\n");
        } else {
            builder.append("@TableName(\"").append(tableMeta.tableName()).append("\")\n");
        }
        builder.append("public class ").append(aggregateName).append("PO extends BasePO {\n\n");
        for (ColumnMeta column : tableMeta.columns()) {
            if (BASE_PO_COLUMNS.contains(column.columnName())) {
                continue;
            }
            appendField(builder, column);
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static String renderBo(String basePackage, String aggregateName, TableMeta tableMeta) {
        return renderSimpleModel(basePackage + ".domain.entity", aggregateName + "BO", tableMeta.columns());
    }

    private static String renderDto(String basePackage, String aggregateName, TableMeta tableMeta) {
        return renderSimpleModel(basePackage + ".api.dto", aggregateName + "DTO", tableMeta.columns());
    }

    private static String renderSimpleModel(String pkg, String className, List<ColumnMeta> columns) {
        Set<String> imports = new TreeSet<>();
        imports.add("lombok.AllArgsConstructor");
        imports.add("lombok.Builder");
        imports.add("lombok.Data");
        imports.add("lombok.NoArgsConstructor");
        for (ColumnMeta column : columns) {
            imports.addAll(column.imports());
        }

        StringBuilder builder = new StringBuilder();
        builder.append("package ").append(pkg).append(";\n\n");
        appendImports(builder, imports);
        builder.append("@Data\n")
                .append("@Builder\n")
                .append("@NoArgsConstructor\n")
                .append("@AllArgsConstructor\n")
                .append("public class ").append(className).append(" {\n\n");
        for (ColumnMeta column : columns) {
            builder.append("    private ").append(column.javaSimpleType()).append(" ").append(column.propertyName()).append(";\n");
        }
        builder.append("}\n");
        return builder.toString();
    }

    private static String renderPoConverter(String basePackage, String aggregateName) {
        return """
                package %s.infra.convert;

                import %s.common.core.mapper.BaseMapper;
                import %s.domain.entity.%sBO;
                import %s.infra.basic.entity.%sPO;
                import org.mapstruct.Mapper;
                import org.mapstruct.factory.Mappers;

                @Mapper
                public interface %sPOConverter extends BaseMapper<%sPO, %sBO> {

                    %sPOConverter INSTANCE = Mappers.getMapper(%sPOConverter.class);
                }
                """.formatted(
                basePackage,
                "cn.cccs7.cynexus",
                basePackage,
                aggregateName,
                basePackage,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName
        );
    }

    private static String renderDtoConverter(String basePackage, String aggregateName) {
        return """
                package %s.application.convert;

                import %s.common.core.mapper.BaseMapper;
                import %s.api.dto.%sDTO;
                import %s.domain.entity.%sBO;
                import org.mapstruct.Mapper;
                import org.mapstruct.factory.Mappers;

                @Mapper
                public interface %sDTOConverter extends BaseMapper<%sDTO, %sBO> {

                    %sDTOConverter INSTANCE = Mappers.getMapper(%sDTOConverter.class);
                }
                """.formatted(
                basePackage,
                "cn.cccs7.cynexus",
                basePackage,
                aggregateName,
                basePackage,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName
        );
    }

    private static String renderMapper(String basePackage, String aggregateName) {
        return """
                package %s.infra.basic.mapper;

                import cn.cccs7.cynexus.common.db.mapper.BaseMapperX;
                import %s.infra.basic.entity.%sPO;

                public interface %sMapper extends BaseMapperX<%sPO> {
                }
                """.formatted(basePackage, basePackage, aggregateName, aggregateName, aggregateName);
    }

    private static String renderCrudAppService(String basePackage, String aggregateName) {
        String fieldName = lowerFirst(aggregateName) + "DomainService";
        return """
                package {basePackage}.application.service;

                import {basePackage}.application.convert.{aggregateName}DTOConverter;
                import {basePackage}.api.dto.{aggregateName}DTO;
                import {basePackage}.domain.entity.{aggregateName}BO;
                import {basePackage}.domain.service.{aggregateName}DomainService;
                import java.util.List;
                import org.springframework.stereotype.Service;

                @Service
                public class {aggregateName}CrudAppService {

                    private final {aggregateName}DomainService {fieldName};

                    public {aggregateName}CrudAppService({aggregateName}DomainService {fieldName}) {
                        this.{fieldName} = {fieldName};
                    }

                    public List<{aggregateName}DTO> list() {
                        return {aggregateName}DTOConverter.INSTANCE.toSourceList({fieldName}.list());
                    }

                    public {aggregateName}DTO findById(Long id) {
                        {aggregateName}BO value = {fieldName}.findById(id);
                        return value == null ? null : {aggregateName}DTOConverter.INSTANCE.toSource(value);
                    }

                    public Long create({aggregateName}DTO dto) {
                        return {fieldName}.create({aggregateName}DTOConverter.INSTANCE.toTarget(dto));
                    }

                    public boolean update({aggregateName}DTO dto) {
                        return {fieldName}.update({aggregateName}DTOConverter.INSTANCE.toTarget(dto));
                    }

                    public boolean delete(Long id) {
                        return {fieldName}.delete(id);
                    }
                }
                """
                .replace("{basePackage}", basePackage)
                .replace("{aggregateName}", aggregateName)
                .replace("{fieldName}", fieldName);
    }

    private static String renderCrudController(String basePackage, String aggregateName, String tableName) {
        String path = tableName.replace('_', '-');
        return """
                package {basePackage}.application.controller;

                import cn.cccs7.cynexus.common.core.result.Result;
                import {basePackage}.api.dto.{aggregateName}DTO;
                import {basePackage}.application.service.{aggregateName}CrudAppService;
                import java.util.List;
                import org.springframework.web.bind.annotation.DeleteMapping;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.PutMapping;
                import org.springframework.web.bind.annotation.RequestBody;
                import org.springframework.web.bind.annotation.RequestMapping;
                import org.springframework.web.bind.annotation.RestController;

                @RestController
                @RequestMapping("/api/{serviceSegment}/{path}")
                public class {aggregateName}CrudController {

                    private final {aggregateName}CrudAppService appService;

                    public {aggregateName}CrudController({aggregateName}CrudAppService appService) {
                        this.appService = appService;
                    }

                    @GetMapping
                    public Result<List<{aggregateName}DTO>> list() {
                        return Result.success(appService.list());
                    }

                    @GetMapping("/{id}")
                    public Result<{aggregateName}DTO> get(@PathVariable Long id) {
                        return Result.success(appService.findById(id));
                    }

                    @PostMapping
                    public Result<Long> create(@RequestBody {aggregateName}DTO dto) {
                        return Result.success(appService.create(dto));
                    }

                    @PutMapping
                    public Result<Boolean> update(@RequestBody {aggregateName}DTO dto) {
                        return Result.success(appService.update(dto));
                    }

                    @DeleteMapping("/{id}")
                    public Result<Boolean> delete(@PathVariable Long id) {
                        return Result.success(appService.delete(id));
                    }
                }
                """
                .replace("{basePackage}", basePackage)
                .replace("{aggregateName}", aggregateName)
                .replace("{serviceSegment}", serviceSegment(basePackage))
                .replace("{path}", path);
    }

    private static String renderFeignClient(String basePackage, String aggregateName, String tableName) {
        String service = serviceSegment(basePackage);
        String serviceName = "cynexus-" + service;
        String path = tableName.replace('_', '-');
        return """
                package {basePackage}.api.feign;

                import cn.cccs7.cynexus.common.core.result.Result;
                import {basePackage}.api.dto.{aggregateName}DTO;
                import java.util.List;
                import org.springframework.cloud.openfeign.FeignClient;
                import org.springframework.web.bind.annotation.DeleteMapping;
                import org.springframework.web.bind.annotation.GetMapping;
                import org.springframework.web.bind.annotation.PathVariable;
                import org.springframework.web.bind.annotation.PostMapping;
                import org.springframework.web.bind.annotation.PutMapping;
                import org.springframework.web.bind.annotation.RequestBody;

                @FeignClient(name = "{serviceName}", path = "/api/{service}/{path}")
                public interface {aggregateName}FeignClient {

                    @GetMapping
                    Result<List<{aggregateName}DTO>> list();

                    @GetMapping("/{id}")
                    Result<{aggregateName}DTO> get(@PathVariable("id") Long id);

                    @PostMapping
                    Result<Long> create(@RequestBody {aggregateName}DTO dto);

                    @PutMapping
                    Result<Boolean> update(@RequestBody {aggregateName}DTO dto);

                    @DeleteMapping("/{id}")
                    Result<Boolean> delete(@PathVariable("id") Long id);
                }
                """
                .replace("{basePackage}", basePackage)
                .replace("{aggregateName}", aggregateName)
                .replace("{serviceName}", serviceName)
                .replace("{service}", service)
                .replace("{path}", path);
    }

    private static String renderRepository(String basePackage, String aggregateName) {
        return """
                package %s.domain.repository;

                import %s.domain.entity.%sBO;
                import java.util.List;

                public interface %sRepository {

                    List<%sBO> list();

                    %sBO findById(Long id);

                    Long create(%sBO value);

                    boolean update(%sBO value);

                    boolean delete(Long id);
                }
                """.formatted(
                basePackage,
                basePackage, aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName
        );
    }

    private static String renderDomainService(String basePackage, String aggregateName) {
        return """
                package %s.domain.service;

                import %s.domain.entity.%sBO;
                import java.util.List;

                public interface %sDomainService {

                    List<%sBO> list();

                    %sBO findById(Long id);

                    Long create(%sBO value);

                    boolean update(%sBO value);

                    boolean delete(Long id);
                }
                """.formatted(
                basePackage,
                basePackage, aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName,
                aggregateName
        );
    }

    private static String renderDomainServiceImpl(String basePackage, String aggregateName) {
        String fieldName = lowerFirst(aggregateName) + "Repository";
        return """
                package {basePackage}.domain.service.impl;

                import {basePackage}.domain.entity.{aggregateName}BO;
                import {basePackage}.domain.repository.{aggregateName}Repository;
                import {basePackage}.domain.service.{aggregateName}DomainService;
                import java.util.List;
                import org.springframework.stereotype.Service;

                @Service
                public class {aggregateName}DomainServiceImpl implements {aggregateName}DomainService {

                    private final {aggregateName}Repository {fieldName};

                    public {aggregateName}DomainServiceImpl({aggregateName}Repository {fieldName}) {
                        this.{fieldName} = {fieldName};
                    }

                    @Override
                    public List<{aggregateName}BO> list() {
                        return {fieldName}.list();
                    }

                    @Override
                    public {aggregateName}BO findById(Long id) {
                        return {fieldName}.findById(id);
                    }

                    @Override
                    public Long create({aggregateName}BO value) {
                        return {fieldName}.create(value);
                    }

                    @Override
                    public boolean update({aggregateName}BO value) {
                        return {fieldName}.update(value);
                    }

                    @Override
                    public boolean delete(Long id) {
                        return {fieldName}.delete(id);
                    }
                }
                """
                .replace("{basePackage}", basePackage)
                .replace("{aggregateName}", aggregateName)
                .replace("{fieldName}", fieldName);
    }

    private static String renderBasicService(String basePackage, String aggregateName) {
        return """
                package %s.infra.basic.service;

                import cn.cccs7.cynexus.common.db.service.CrudBasicService;
                import %s.infra.basic.entity.%sPO;

                public interface %sBasicService extends CrudBasicService<%sPO> {
                }
                """.formatted(basePackage, basePackage, aggregateName, aggregateName, aggregateName);
    }

    private static String renderBasicServiceImpl(String basePackage, String aggregateName) {
        return """
                package {basePackage}.infra.basic.service.impl;

                import cn.cccs7.cynexus.common.core.id.SnowflakeIdGenerator;
                import cn.cccs7.cynexus.common.db.service.AbstractCrudBasicService;
                import {basePackage}.infra.basic.entity.{aggregateName}PO;
                import {basePackage}.infra.basic.mapper.{aggregateName}Mapper;
                import {basePackage}.infra.basic.service.{aggregateName}BasicService;
                import org.springframework.stereotype.Service;

                @Service
                public class {aggregateName}BasicServiceImpl extends AbstractCrudBasicService<{aggregateName}PO>
                        implements {aggregateName}BasicService {

                    public {aggregateName}BasicServiceImpl({aggregateName}Mapper mapper,
                                                           SnowflakeIdGenerator snowflakeIdGenerator) {
                        super(mapper, snowflakeIdGenerator);
                    }
                }
                """
                .replace("{basePackage}", basePackage)
                .replace("{aggregateName}", aggregateName);
    }

    private static String renderRepositoryImpl(String basePackage, String aggregateName) {
        String basicServiceField = lowerFirst(aggregateName) + "BasicService";
        return """
                package {basePackage}.infra.repository;

                import {basePackage}.domain.entity.{aggregateName}BO;
                import {basePackage}.domain.repository.{aggregateName}Repository;
                import {basePackage}.infra.basic.service.{aggregateName}BasicService;
                import {basePackage}.infra.convert.{aggregateName}POConverter;
                import java.util.List;
                import java.util.Optional;
                import org.springframework.stereotype.Repository;

                @Repository
                public class {aggregateName}RepositoryImpl implements {aggregateName}Repository {

                    private final {aggregateName}BasicService {basicServiceField};

                    public {aggregateName}RepositoryImpl({aggregateName}BasicService {basicServiceField}) {
                        this.{basicServiceField} = {basicServiceField};
                    }

                    @Override
                    public List<{aggregateName}BO> list() {
                        return {aggregateName}POConverter.INSTANCE.toTargetList({basicServiceField}.listAll());
                    }

                    @Override
                    public {aggregateName}BO findById(Long id) {
                        return Optional.ofNullable({basicServiceField}.getById(id))
                                .map({aggregateName}POConverter.INSTANCE::toTarget)
                                .orElse(null);
                    }

                    @Override
                    public Long create({aggregateName}BO value) {
                        return {basicServiceField}.create({aggregateName}POConverter.INSTANCE.toSource(value));
                    }

                    @Override
                    public boolean update({aggregateName}BO value) {
                        return {basicServiceField}.update({aggregateName}POConverter.INSTANCE.toSource(value));
                    }

                    @Override
                    public boolean delete(Long id) {
                        return {basicServiceField}.delete(id);
                    }
                }
                """
                .replace("{basePackage}", basePackage)
                .replace("{aggregateName}", aggregateName)
                .replace("{basicServiceField}", basicServiceField);
    }

    private static String renderMapperXml(String basePackage, String aggregateName, TableMeta tableMeta) {
        StringBuilder builder = new StringBuilder();
        builder.append("""
                <?xml version="1.0" encoding="UTF-8" ?>
                <!DOCTYPE mapper
                        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
                        "https://mybatis.org/dtd/mybatis-3-mapper.dtd">
                <mapper namespace="%s.infra.basic.mapper.%sMapper">
                """.formatted(basePackage, aggregateName));

        builder.append("\n    <resultMap id=\"BaseResultMap\" type=\"")
                .append(basePackage)
                .append(".infra.basic.entity.")
                .append(aggregateName)
                .append("PO\">\n");

        for (ColumnMeta column : tableMeta.columns()) {
            String tag = "id".equals(column.columnName()) ? "id" : "result";
            builder.append("        <").append(tag)
                    .append(" property=\"").append(column.propertyName()).append("\"")
                    .append(" column=\"").append(column.columnName()).append("\"");
            if (column.jsonb()) {
                builder.append(" jdbcType=\"OTHER\" typeHandler=\"").append(JSONB_HANDLER).append("\"");
            }
            builder.append("/>\n");
        }
        builder.append("    </resultMap>\n\n");

        builder.append("    <sql id=\"BaseColumnList\">\n        ");
        builder.append(tableMeta.columns().stream().map(ColumnMeta::columnName).collect(Collectors.joining(", ")));
        builder.append("\n    </sql>\n");
        builder.append("</mapper>\n");
        return builder.toString();
    }

    private static void appendField(StringBuilder builder, ColumnMeta column) {
        if (column.jsonb()) {
            builder.append("    @TableField(typeHandler = JsonbTypeHandler.class)\n");
        } else if (column.requiresTableField()) {
            builder.append("    @TableField(\"").append(column.columnName()).append("\")\n");
        }
        builder.append("    private ")
                .append(column.javaSimpleType())
                .append(" ")
                .append(column.propertyName())
                .append(";\n");
    }

    private static void appendImports(StringBuilder builder, Set<String> imports) {
        Set<String> filtered = imports.stream()
                .filter(TableLayerCodegenMain::needsImport)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        for (String importName : filtered) {
            builder.append("import ").append(importName).append(";\n");
        }
        builder.append('\n');
    }

    private static boolean needsImport(String fqcn) {
        return !fqcn.startsWith("java.lang.");
    }

    private static String buildAggregateName(String tableName, List<String> stripPrefixes) {
        String working = tableName;
        for (String prefix : stripPrefixes) {
            if (working.startsWith(prefix)) {
                working = working.substring(prefix.length());
                break;
            }
        }
        return Arrays.stream(working.split("_"))
                .filter(part -> !part.isBlank())
                .map(TableLayerCodegenMain::capitalize)
                .collect(Collectors.joining());
    }

    private static String capitalize(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }

    private static String lowerFirst(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        return Character.toLowerCase(value.charAt(0)) + value.substring(1);
    }

    private static String serviceSegment(String basePackage) {
        int idx = basePackage.lastIndexOf('.');
        return idx < 0 ? basePackage : basePackage.substring(idx + 1);
    }

    private static void writeJava(Path directory, String fileName, String content, boolean overwrite) {
        writeText(directory, fileName, content, overwrite);
    }

    private static void writeText(Path directory, String fileName, String content, boolean overwrite) {
        try {
            Files.createDirectories(directory);
            Path file = directory.resolve(fileName);
            if (!overwrite && Files.exists(file)) {
                System.out.println("Skip existing file: " + file);
                return;
            }
            Files.writeString(file, content, StandardCharsets.UTF_8);
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    private record CodegenConfig(
            String jdbcUrl,
            String username,
            String password,
            String schema,
            String service,
            Path repoRoot,
            List<String> tables,
            List<String> stripPrefixes,
            boolean overwrite
    ) {

        private static final Map<String, String> DEFAULT_PREFIXES = Map.of(
                "market", "msi_,mkt_,sec_",
                "core", "scheduler_,sched_"
        );

        static CodegenConfig parse(String[] args) {
            Map<String, String> values = Arrays.stream(args)
                    .filter(Objects::nonNull)
                    .filter(arg -> arg.startsWith("--"))
                    .map(arg -> arg.substring(2))
                    .map(arg -> arg.split("=", 2))
                    .collect(Collectors.toMap(parts -> parts[0], parts -> parts.length > 1 ? parts[1] : "", (a, b) -> b));

            String service = values.get("service");
            Path repoRoot = values.containsKey("repoRoot")
                    ? Path.of(values.get("repoRoot"))
                    : Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
            List<String> tables = splitCsv(values.get("tables"));
            List<String> stripPrefixes = splitCsv(values.getOrDefault("stripPrefixes", DEFAULT_PREFIXES.getOrDefault(service, "")));

            return new CodegenConfig(
                    values.get("jdbcUrl"),
                    values.get("username"),
                    values.get("password"),
                    values.get("schema"),
                    service,
                    repoRoot,
                    tables,
                    stripPrefixes,
                    Boolean.parseBoolean(values.getOrDefault("overwrite", "false"))
            );
        }

        private static List<String> splitCsv(String value) {
            if (value == null || value.isBlank()) {
                return List.of();
            }
            return Arrays.stream(value.split(","))
                    .map(String::trim)
                    .filter(part -> !part.isBlank())
                    .toList();
        }

        Path serviceRoot() {
            return repoRoot.resolve("cynexus-" + service);
        }

        Path apiDtoDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-api")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".api.dto"));
        }

        Path apiFeignDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-api")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".api.feign"));
        }

        Path domainEntityDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-domain")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".domain.entity"));
        }

        Path applicationConvertDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-application")
                    .resolve("cynexus-" + service + "-application-controller")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".application.convert"));
        }

        Path applicationServiceDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-application")
                    .resolve("cynexus-" + service + "-application-controller")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".application.service"));
        }

        Path applicationControllerDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-application")
                    .resolve("cynexus-" + service + "-application-controller")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".application.controller"));
        }

        Path domainRepositoryDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-domain")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".domain.repository"));
        }

        Path domainServiceDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-domain")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".domain.service"));
        }

        Path domainServiceImplDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-domain")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".domain.service.impl"));
        }

        Path infraEntityDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".infra.basic.entity"));
        }

        Path infraMapperDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".infra.basic.mapper"));
        }

        Path infraConvertDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".infra.convert"));
        }

        Path infraBasicServiceDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".infra.basic.service"));
        }

        Path infraBasicServiceImplDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".infra.basic.service.impl"));
        }

        Path infraRepositoryDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/java")
                    .resolve(packagePath("cn.cccs7.cynexus." + service + ".infra.repository"));
        }

        Path infraMapperXmlDir() {
            return serviceRoot()
                    .resolve("cynexus-" + service + "-infra")
                    .resolve("src/main/resources/mapper");
        }

        private static Path packagePath(String packageName) {
            return Path.of(packageName.replace('.', '/'));
        }
    }

    private record TableMeta(String schema, String tableName, List<ColumnMeta> columns) {
    }

    private record ColumnMeta(
            String columnName,
            String propertyName,
            String javaType,
            String importName,
            boolean nullable,
            boolean jsonb,
            boolean requiresTableField
    ) {

        static ColumnMeta from(String columnName, String dataType, String udtName, boolean nullable) {
            String propertyName = resolvePropertyName(columnName, dataType);
            JavaType javaType = resolveJavaType(dataType, udtName);
            boolean jsonb = "jsonb".equalsIgnoreCase(udtName) || "json".equalsIgnoreCase(udtName);
            boolean requiresTableField = !columnName.equals(toSnakeCase(propertyName));
            return new ColumnMeta(columnName, propertyName, javaType.fqcn(), javaType.importName(), nullable, jsonb, requiresTableField);
        }

        private static String resolvePropertyName(String columnName, String dataType) {
            if (columnName.startsWith("is_") && isBooleanType(dataType)) {
                return snakeToCamel(columnName.substring(3));
            }
            return snakeToCamel(columnName);
        }

        private static boolean isBooleanType(String dataType) {
            return "boolean".equalsIgnoreCase(dataType);
        }

        private static JavaType resolveJavaType(String dataType, String udtName) {
            String normalizedDataType = dataType == null ? "" : dataType.toLowerCase(Locale.ROOT);
            String normalizedUdt = udtName == null ? "" : udtName.toLowerCase(Locale.ROOT);
            return switch (normalizedDataType) {
                case "bigint", "bigserial" -> JavaType.of(Long.class);
                case "integer", "serial", "smallint", "smallserial" -> JavaType.of(Integer.class);
                case "numeric", "decimal", "money" -> JavaType.of(BigDecimal.class);
                case "real" -> JavaType.of(Float.class);
                case "double precision" -> JavaType.of(Double.class);
                case "boolean" -> JavaType.of(Boolean.class);
                case "date" -> JavaType.of(LocalDate.class);
                case "timestamp with time zone" -> JavaType.of(OffsetDateTime.class);
                case "timestamp without time zone" -> JavaType.of(LocalDateTime.class);
                case "time with time zone" -> JavaType.of(OffsetTime.class);
                case "json", "jsonb" -> JavaType.of(String.class);
                case "bytea" -> new JavaType("byte[]", null);
                default -> resolveByUdt(normalizedUdt);
            };
        }

        private static JavaType resolveByUdt(String udtName) {
            return switch (udtName) {
                case "int8" -> JavaType.of(Long.class);
                case "int4", "int2" -> JavaType.of(Integer.class);
                case "float4" -> JavaType.of(Float.class);
                case "float8" -> JavaType.of(Double.class);
                case "numeric" -> JavaType.of(BigDecimal.class);
                case "bool" -> JavaType.of(Boolean.class);
                case "date" -> JavaType.of(LocalDate.class);
                case "timestamptz" -> JavaType.of(OffsetDateTime.class);
                case "timestamp" -> JavaType.of(LocalDateTime.class);
                case "timetz" -> JavaType.of(OffsetTime.class);
                case "json", "jsonb", "varchar", "text", "bpchar", "uuid" -> JavaType.of(String.class);
                default -> JavaType.of(String.class);
            };
        }

        Set<String> imports() {
            if (importName == null || importName.isBlank()) {
                return Set.of();
            }
            return Set.of(importName);
        }

        String javaSimpleType() {
            int idx = javaType.lastIndexOf('.');
            return idx < 0 ? javaType : javaType.substring(idx + 1);
        }
    }

    private record JavaType(String fqcn, String importName) {

        static JavaType of(Class<?> type) {
            return new JavaType(type.getName(), type.getName());
        }
    }

    private static String snakeToCamel(String value) {
        StringBuilder builder = new StringBuilder();
        boolean upper = false;
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (current == '_') {
                upper = true;
                continue;
            }
            if (builder.isEmpty()) {
                builder.append(Character.toLowerCase(current));
            } else if (upper) {
                builder.append(Character.toUpperCase(current));
                upper = false;
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }

    private static String toSnakeCase(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char current = value.charAt(i);
            if (Character.isUpperCase(current)) {
                if (!builder.isEmpty()) {
                    builder.append('_');
                }
                builder.append(Character.toLowerCase(current));
            } else {
                builder.append(current);
            }
        }
        return builder.toString();
    }
}
