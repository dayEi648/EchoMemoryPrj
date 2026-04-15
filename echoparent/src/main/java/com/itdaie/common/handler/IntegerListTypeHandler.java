package com.itdaie.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.Array;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * PostgreSQL integer[] 与 List<Integer> 的双向映射处理器。
 */
public class IntegerListTypeHandler extends BaseTypeHandler<List<Integer>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<Integer> parameter, JdbcType jdbcType)
            throws SQLException {
        Connection connection = ps.getConnection();
        Integer[] array = parameter.toArray(new Integer[0]);
        Array sqlArray = connection.createArrayOf("integer", array);
        ps.setArray(i, sqlArray);
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<Integer> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<Integer> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private List<Integer> toList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        Object arrayObj = sqlArray.getArray();
        if (arrayObj == null) {
            return Collections.emptyList();
        }
        // PostgreSQL integer[] 返回的是 Integer[] 或 int[]
        if (arrayObj instanceof Integer[]) {
            return Arrays.asList((Integer[]) arrayObj);
        } else if (arrayObj instanceof int[]) {
            int[] intArray = (int[]) arrayObj;
            return Arrays.stream(intArray).boxed().collect(Collectors.toList());
        }
        return Collections.emptyList();
    }
}
