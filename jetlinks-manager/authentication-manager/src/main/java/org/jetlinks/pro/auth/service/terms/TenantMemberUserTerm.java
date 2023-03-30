package org.jetlinks.pro.auth.service.terms;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * 根据租户成员查询用户相关信息
 *
 * <pre>
 *     id$tenant-user = tenantId //查询租户ID为tenantId的租户下成员相关数据
 *
 *     id$tenant-user$not = tenantId //查询租户ID不为tenantId的租户下
 *
 *     id$tenant-user$any = 1 //查询任意租户下成员用户信息
 *
 *     id$tenant-user$not$any = 1 //查询没有和租户关联的用户信息
 * </pre>
 * @since 1.9
 * @author zhouhao
 */
@Component
public class TenantMemberUserTerm extends AbstractTermFragmentBuilder {
    public TenantMemberUserTerm() {
        super("tenant-user", "租户下成员用户信息");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {
        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        boolean any = term.getOptions().contains("any");
        boolean not = term.getOptions().contains("not");
        if(not){
            sqlFragments.addSql("not");
        }
        sqlFragments.addSql("exists(select 1 from s_tenant_member _mem where _mem.user_id = ", columnFullName);

        if (!any) {
            List<Object> tenantIds = convertList(column, term);
            String[] args = new String[tenantIds.size()];
            Arrays.fill(args, "?");
            sqlFragments.addSql("and tenant_id in (", String.join(",", args), ")")
                        .addParameter(tenantIds);
        }

        sqlFragments.addSql(")");

        return sqlFragments;
    }
}
