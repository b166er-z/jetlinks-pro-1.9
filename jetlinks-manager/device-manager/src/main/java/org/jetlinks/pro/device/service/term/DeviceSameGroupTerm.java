package org.jetlinks.pro.device.service.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 查询与某个设备在同一个分组的设备或者与设备关联的数据.
 * <p>
 * <b>
 * 注意: 查询时指定列名是和设备ID关联的列或者实体类属性名.
 * 如: 查询设备列表时则使用id.
 * 此条件仅支持关系型数据库中的查询.
 * </b>
 * <p>
 * 在通用查询接口中可以使用动态查询参数中的<code>term.termType</code>来使用此功能.
 * <a href="http://doc.jetlinks.cn/interface-guide/query-param.html">查看动态查询参数说明</a>
 * <p>
 * 在内部通用条件中,可以使用DSL方式创建条件,例如:
 * <pre>
 *     createQuery()
 *     .where()
 *     .and("id","dev-latest$dev-same-group",deviceId) // dev-latest$dev-same-group$contains 表示包含被查询设备自己
 *     .fetch()
 * </pre>
 *
 * @author zhouhao
 * @since 1.6
 */
@Component
public class DeviceSameGroupTerm extends AbstractTermFragmentBuilder {

    public static final String termType = "dev-same-group";

    public DeviceSameGroupTerm() {
        super(termType, "查询同一个分组的设备");
    }


    public static String createColumn(String column, Object deviceId, boolean contains) {
        return contains ? column + "$" + termType + "$contains" : column + "$" + termType;
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        Object id = term.getValue();

        sqlFragments.addSql("exists(select 1",
            "from dev_device_group_bind _bind2",
            "where exists(select _bind.group_id from dev_device_group_bind _bind where _bind.device_id = ? and _bind2.group_id = _bind.group_id)",
            "and _bind2.device_id =", columnFullName)
            .addParameter(id);

        if (!term.getOptions().contains("contains")) {
            sqlFragments.addSql("and _bind2.device_id != ?").addParameter(id);
        }

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
