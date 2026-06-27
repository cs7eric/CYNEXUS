# Table Codegen

`TableLayerCodegenMain` can generate these files directly from PostgreSQL table metadata:

- `PO`
- `BO`
- `DTO`
- `FeignClient`
- `POConverter`
- `DTOConverter`
- `Controller`
- `AppService`
- `DomainService`
- `Repository`
- `BasicService`
- `Mapper`
- `MyBatis XML`

Generated directories follow the current Cynexus layered structure automatically.
The generated CRUD chain is:

- `Controller -> AppService -> DomainService -> Repository -> BasicService -> Mapper/XML`

Cross-service access is generated in:

- `api/feign/{Aggregate}FeignClient`

## Location

Generator main class:

- `cn.cccs7.cynexus.common.db.codegen.TableLayerCodegenMain`

## Run

From repo root:

```bash
mvn -pl cynexus-common/cynexus-common-db -am compile exec:java ^
  -Dexec.mainClass=cn.cccs7.cynexus.common.db.codegen.TableLayerCodegenMain ^
  -Dexec.args="--jdbcUrl=jdbc:postgresql://127.0.0.1:25433/cynexus --username=postgres --password=C020611. --schema=cynexus_appl_market --service=market --tables=msi_index_daily,mkt_overview_daily --repoRoot=E:/project-repo/cynexus"
```

PowerShell line-continuation can also be written as one line.

## Common Args

- `--jdbcUrl`: PostgreSQL JDBC URL
- `--username`: PostgreSQL username
- `--password`: PostgreSQL password
- `--schema`: schema name, for example `cynexus_appl_market`
- `--service`: bounded context name, for example `market`, `ai`, `core`
- `--repoRoot`: repo root path, defaults to current working directory
- `--tables`: comma-separated tables; if omitted, generate all base tables in the schema
- `--stripPrefixes`: optional table prefix stripping, comma-separated
- `--overwrite`: whether to overwrite existing files, default `false`

## Default Prefix Rules

- `market`: strips `msi_`, `mkt_`, `sec_`
- `core`: strips `scheduler_`, `sched_`

Example:

- `msi_index_daily` -> `IndexDailyPO / BO / DTO`
- `mkt_overview_daily` -> `OverviewDailyPO / BO / DTO`

If you want to keep the full prefix in class names, pass:

```bash
--stripPrefixes=
```

## Output Paths

For `--service=market`, generated files go to:

- `cynexus-market/cynexus-market-api/.../dto`
- `cynexus-market/cynexus-market-domain/.../entity`
- `cynexus-market/cynexus-market-application/cynexus-market-application-controller/.../convert`
- `cynexus-market/cynexus-market-infra/.../basic/entity`
- `cynexus-market/cynexus-market-infra/.../basic/mapper`
- `cynexus-market/cynexus-market-infra/.../convert`
- `cynexus-market/cynexus-market-infra/src/main/resources/mapper`

## Type Mapping

- `bigint` -> `Long`
- `integer` / `smallint` -> `Integer`
- `numeric` / `decimal` -> `BigDecimal`
- `boolean` -> `Boolean`
- `date` -> `LocalDate`
- `timestamp with time zone` -> `OffsetDateTime`
- `timestamp without time zone` -> `LocalDateTime`
- `json` / `jsonb` -> `String`

## Notes

- `PO` extends `BasePO`, so `id / created_at / updated_at / deleted_at` are not regenerated into `PO`.
- `BO` and `DTO` will include all table columns, including base columns.
- `jsonb` fields are generated with `JsonbTypeHandler`.
- For boolean columns like `is_manual_override`, the generated property becomes `manualOverride` and the `PO` adds `@TableField("is_manual_override")`.
