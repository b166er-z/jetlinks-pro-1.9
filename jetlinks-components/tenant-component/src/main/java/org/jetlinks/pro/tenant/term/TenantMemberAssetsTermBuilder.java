package org.jetlinks.pro.tenant.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class TenantMemberAssetsTermBuilder extends AbstractTermFragmentBuilder {

    public TenantMemberAssetsTermBuilder() {
        super(AssetsTerm.ID, "资产");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {


        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        AssetsTerm assetsTerm = AssetsTerm.of(term);
        if(assetsTerm.isNot()){
            sqlFragments.addSql("not");
        }
        if (StringUtils.isEmpty(assetsTerm.getMemberId())) {
            //忽略成员,只判断同一租户类的数据
            sqlFragments.addSql("exists(select 1 from s_member_assets ass where "
                , "ass.tenant_id = ?"
                , "and ass.asset_type = ?"
                , "and ass.asset_id = ", columnFullName, ")")
                .addParameter(assetsTerm.getTenantId(), assetsTerm.getAssetType());
        } else {
            sqlFragments.addSql("exists(select 1 from s_member_assets ass where "
                , "ass.tenant_id = ?"
                , "and ass.user_id = ?"
                , "and ass.asset_type = ?"
                , "and ass.asset_id = ", columnFullName, ")")
                .addParameter(assetsTerm.getTenantId(), assetsTerm.getMemberId(), assetsTerm.getAssetType());
        }


        return sqlFragments;
    }
}
