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

/**
 * PostgreSQL text[] 与 List<String> 的双向映射处理器。
 */
public class StringListTypeHandler extends BaseTypeHandler<List<String>> {

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, List<String> parameter, JdbcType jdbcType)
            throws SQLException {
        Connection connection = ps.getConnection();
        Array sqlArray = connection.createArrayOf("text", parameter.toArray(new String[0]));
        ps.setArray(i, sqlArray);
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return toList(rs.getArray(columnName));
    }

    @Override
    public List<String> getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return toList(rs.getArray(columnIndex));
    }

    @Override
    public List<String> getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return toList(cs.getArray(columnIndex));
    }

    private List<String> toList(Array sqlArray) throws SQLException {
        if (sqlArray == null) {
            return null;
        }
        Object arrayObj = sqlArray.getArray();
        if (arrayObj == null) {
            return Collections.emptyList();
        }
        String[] values = (String[]) arrayObj;
        return Arrays.asList(values);
    }
}
