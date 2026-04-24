package com.itdaie.common.handler;

import org.apache.ibatis.type.BaseTypeHandler;
import org.apache.ibatis.type.JdbcType;

import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

/**
 * PostgreSQL jsonb 与 Java String 的双向映射处理器。
 * <p>
 * 写入时通过反射构造 PostgreSQL 驱动的 PGobject 并标记类型为 jsonb，
 * 避免编译期依赖 postgresql 驱动（其 scope 为 runtime）。
 * 若反射失败则回退到 {@link Types#OTHER}。
 */
public class JsonbTypeHandler extends BaseTypeHandler<String> {

    private static final String PG_OBJECT_CLASS = "org.postgresql.util.PGobject";
    private static final String JSONB_TYPE = "jsonb";

    @Override
    public void setNonNullParameter(PreparedStatement ps, int i, String parameter, JdbcType jdbcType)
            throws SQLException {
        Object pgObject = createPgObject(parameter);
        if (pgObject != null) {
            ps.setObject(i, pgObject);
        } else {
            ps.setObject(i, parameter, Types.OTHER);
        }
    }

    @Override
    public String getNullableResult(ResultSet rs, String columnName) throws SQLException {
        return rs.getString(columnName);
    }

    @Override
    public String getNullableResult(ResultSet rs, int columnIndex) throws SQLException {
        return rs.getString(columnIndex);
    }

    @Override
    public String getNullableResult(CallableStatement cs, int columnIndex) throws SQLException {
        return cs.getString(columnIndex);
    }

    /**
     * 通过反射创建 PGobject 实例，避免编译期依赖 postgresql 驱动。
     */
    private Object createPgObject(String value) {
        try {
            Class<?> clazz = Class.forName(PG_OBJECT_CLASS);
            Object instance = clazz.getDeclaredConstructor().newInstance();
            clazz.getMethod("setType", String.class).invoke(instance, JSONB_TYPE);
            clazz.getMethod("setValue", String.class).invoke(instance, value);
            return instance;
        } catch (Exception e) {
            return null;
        }
    }
}
