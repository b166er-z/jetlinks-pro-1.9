package org.jetlinks.pro.tenant.term;

import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.ezorm.rdb.metadata.RDBColumnMetadata;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.PrepareSqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.SqlFragments;
import org.hswebframework.ezorm.rdb.operator.builder.fragments.term.AbstractTermFragmentBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class MultiTenantMemberAssetsTermBuilder extends AbstractTermFragmentBuilder {

    public MultiTenantMemberAssetsTermBuilder() {
        super(MultiAssetsTerm.ID, "资产");
    }

    @Override
    public SqlFragments createFragments(String columnFullName, RDBColumnMetadata column, Term term) {


        PrepareSqlFragments sqlFragments = PrepareSqlFragments.of();

        MultiAssetsTerm assetsTerms = MultiAssetsTerm.from(term);

        sqlFragments.addSql("exists(select 1 from s_member_assets ass where"
            , "ass.asset_id =", columnFullName
            , "and ass.asset_type = ?")
            .addParameter(assetsTerms.getTerms().get(0).getAssetType())
            .addSql("and (");

        for (int i = 0, size = assetsTerms.getTerms().size(); i < size; i++) {
            if (i != 0) {
                sqlFragments.addSql("or");
            }
            AssetsTerm assetsTerm = assetsTerms.getTerms().get(i);
            sqlFragments.addSql("( ass.tenant_id = ?")
                .addParameter(assetsTerm.getTenantId());
            //全部成员数据
            if (!StringUtils.isEmpty(assetsTerm.getMemberId())) {
                sqlFragments.addSql("and ass.user_id = ?")
                    .addParameter(assetsTerm.getMemberId());
            }
            sqlFragments.addSql(")");

        }
        sqlFragments.addSql(") )");

        return sqlFragments;
    }
}
