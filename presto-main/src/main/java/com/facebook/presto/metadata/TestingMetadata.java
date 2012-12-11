package com.facebook.presto.metadata;

import com.facebook.presto.sql.tree.QualifiedName;
import com.facebook.presto.tuple.TupleInfo;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.facebook.presto.metadata.MetadataUtil.checkCatalogName;
import static com.facebook.presto.metadata.MetadataUtil.checkSchemaName;
import static com.facebook.presto.metadata.MetadataUtil.checkTableName;
import static com.google.common.base.Preconditions.checkArgument;

public class TestingMetadata
        implements Metadata
{
    private final Map<List<String>, TableMetadata> tables = new HashMap<>();
    private final FunctionRegistry functions = new FunctionRegistry();

    @Override
    public FunctionInfo getFunction(QualifiedName name, List<TupleInfo.Type> parameterTypes)
    {
        return functions.get(name, parameterTypes);
    }

    @Override
    public FunctionInfo getFunction(FunctionHandle handle)
    {
        return functions.get(handle);
    }

    @Override
    public TableMetadata getTable(String catalogName, String schemaName, String tableName)
    {
        checkTableName(catalogName, schemaName, tableName);
        return tables.get(tableKey(catalogName, schemaName, tableName));
    }

    @Override
    public List<QualifiedTableName> listTables(String catalogName)
    {
        Iterable<TableMetadata> values = Maps.filterKeys(tables, catalogMatches(catalogName)).values();
        return ImmutableList.copyOf(Iterables.transform(values, toQualifiedTableName()));
    }

    @Override
    public List<QualifiedTableName> listTables(String catalogName, String schemaName)
    {
        Iterable<TableMetadata> values = Maps.filterKeys(tables, schemaMatches(catalogName, schemaName)).values();
        return ImmutableList.copyOf(Iterables.transform(values, toQualifiedTableName()));
    }

    @Override
    public List<TableColumn> listTableColumns(String catalogName)
    {
        Iterable<TableMetadata> values = Maps.filterKeys(tables, catalogMatches(catalogName)).values();
        return ImmutableList.copyOf(Iterables.concat(Iterables.transform(values, toTableColumns())));
    }

    @Override
    public synchronized void createTable(TableMetadata table)
    {
        List<String> key = tableKey(table);
        checkArgument(!tables.containsKey(key), "Table '%s.%s.%s' already defined",
                table.getSchemaName(), table.getCatalogName(), table.getTableName());
        tables.put(key, table);
    }

    private static Predicate<List<String>> catalogMatches(final String catalogName)
    {
        checkCatalogName(catalogName);
        return new Predicate<List<String>>()
        {
            @Override
            public boolean apply(List<String> key)
            {
                return key.get(0).equals(catalogName);
            }
        };
    }

    private static Predicate<List<String>> schemaMatches(final String catalogName, final String schemaName)
    {
        checkSchemaName(catalogName, schemaName);
        return new Predicate<List<String>>()
        {
            @Override
            public boolean apply(List<String> key)
            {
                return key.get(0).equals(catalogName) && key.get(1).equals(schemaName);
            }
        };
    }

    private static List<String> tableKey(TableMetadata table)
    {
        return tableKey(table.getCatalogName(), table.getSchemaName(), table.getTableName());
    }

    private static List<String> tableKey(String catalogName, String schemaName, String tableName)
    {
        return ImmutableList.of(catalogName, schemaName, tableName);
    }

    private static Function<TableMetadata, QualifiedTableName> toQualifiedTableName()
    {
        return new Function<TableMetadata, QualifiedTableName>()
        {
            @Override
            public QualifiedTableName apply(TableMetadata input)
            {
                return new QualifiedTableName(input.getCatalogName(), input.getSchemaName(), input.getTableName());
            }
        };
    }

    private static Function<TableMetadata, List<TableColumn>> toTableColumns()
    {
        return new Function<TableMetadata, List<TableColumn>>()
        {
            @Override
            public List<TableColumn> apply(TableMetadata input)
            {
                ImmutableList.Builder<TableColumn> columns = ImmutableList.builder();
                int position = 1;
                for (ColumnMetadata column : input.getColumns()) {
                    columns.add(new TableColumn(
                            input.getCatalogName(), input.getSchemaName(), input.getTableName(),
                            column.getName(), position, column.getType()));
                    position++;
                }
                return columns.build();
            }
        };
    }
}