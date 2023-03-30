package org.jetlinks.pro.device.service.term;

import com.alibaba.excel.util.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.jetlinks.pro.device.entity.DeviceBindEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 根据第三方平台接入情况来查询设备相关数据，如: 查询接入了<code>ctwing</code>的设备列表.
 * <p>
 * <b>
 * 注意: 查询时指定列名是和设备ID关联的列或者实体类属性名.
 * 如: 查询设备列表时则使用id,查询和设备关联的数据时则为关联的字段.
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
 *     .and("deviceId","dev-bind$ctwing",ctwingId)
 *     .fetch()
 * </pre>
 * <p>
 * 例1: 查询与类型为<code>ctwing</code>,第三方平台id为<code>ctwing-id</code>绑定的设备
 * <pre>
 *     {
 *         "column":"id",
 *         "termType":"dev-bind$ctwing",
 *         "value":"ctwing-id"
 *     }
 * </pre>
 * <p>
 * 例2: 查询与类型为<code>ctwing</code>绑定的设备
 * <pre>
 *     {
 *         "column":"id",
 *         "termType":"dev-bind$any",
 *         "value":"ctwing"
 *     }
 * </pre>
 *
 * @author zhouhao
 * @see org.jetlinks.core.device.manager.DeviceBindManager
 * @since 1.7
 */
@Component
public class DeviceBindTerm extends AbstractTermFragmentBuilder {
    public DeviceBindTerm() {
        super("dev-bind", "第三方平台设备绑定");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {

        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();
        if (term.getOptions().contains("not")) {
            sqlFragments.addSql("not");
        }
        sqlFragments
            .addSql("exists(select 1 from dev_third_bind _bind where _bind.device_id = ", columnFullName);
        //没有指定any类型
        if (!term.getOptions().contains("any")) {
            if (CollectionUtils.isEmpty(term.getOptions())) {
                throw new IllegalArgumentException("查询条件未指定选项,正确例子:" + column.getName() + "$dev-bind$type");
            }
            String type = term.getOptions().get(0);
            List<Object> idList = convertList(column, term);
            sqlFragments
                .addSql(
                    " and _bind.id in (",
                    idList.stream().map(r -> "?").collect(Collectors.joining(",")),
                    ")")
                .addParameter(idList
                                  .stream()
                                  .map(id -> DeviceBindEntity.generateId(type, String.valueOf(id)))
                                  .collect(Collectors.toList()));
        } else {
            //指定了any,term.value则为type值
            List<Object> types = convertList(column, term);
            sqlFragments
                .addSql(
                    " and _bind.type in (",
                    types.stream().map(r -> "?").collect(Collectors.joining(",")),
                    ")")
                .addParameter(types);
        }
        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
