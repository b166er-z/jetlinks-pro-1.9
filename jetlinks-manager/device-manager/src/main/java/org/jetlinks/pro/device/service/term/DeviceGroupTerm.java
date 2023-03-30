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
 * 根据设备分组查询设备或者与设备关联的数据,如: 查询某个分组下的设备列表.
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
 *     .and("id","dev-group",groupId)
 *     .fetch()
 * </pre>
 *
 * @author zhouhao
 * @since 1.4
 */
@Component
public class DeviceGroupTerm extends AbstractTermFragmentBuilder {

    public static final String termType = "dev-group";

    public DeviceGroupTerm() {
        super(termType, "按设备分组查询设备");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        List<Object> idList = convertList(column, term);

        if (idList.isEmpty()) {
            return sqlFragments.addSql("1=2");
        }
        if(term.getOptions().contains("not")){
            sqlFragments.addSql("not");
        }
        sqlFragments.addSql("exists(select 1 from dev_device_group_bind _bind where _bind.device_id =", columnFullName);

        sqlFragments.addSql(
            "and _bind.group_id in (",
            idList.stream().map(r -> "?").collect(Collectors.joining(","))
            , ")").addParameter(idList);

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
