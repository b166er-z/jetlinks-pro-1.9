package org.jetlinks.pro.tenant.term;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.core.param.Term;
import org.jetlinks.pro.tenant.TenantMember;
import org.jetlinks.pro.tenant.supports.MultiTenantMember;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
public class MultiAssetsTerm {

    public static final String ID = "multi-assets";

    private List<AssetsTerm> terms = new ArrayList<>();

    public void addTerm(AssetsTerm term) {

        terms.add(term);

    }

    public void addTerms(Collection<AssetsTerm> term) {

        terms.addAll(term);

    }

    public static MultiAssetsTerm from(Term term) {
        Object termValue = term.getValue();
        if (termValue instanceof MultiAssetsTerm) {
            return ((MultiAssetsTerm) termValue);
        }
        // TODO: 2020/6/1 支持更多类型转换
        throw new UnsupportedOperationException();
    }

    public static MultiAssetsTerm from(String type, TenantMember member) {
        MultiAssetsTerm term = new MultiAssetsTerm();

        if (member instanceof MultiTenantMember) {
            MultiTenantMember multiTenantMember = ((MultiTenantMember) member);
            term.addTerms(multiTenantMember.getMembers()
                .stream()
                .map(_member -> AssetsTerm.from(type, _member))
                .collect(Collectors.toList()));
        } else {
            term.addTerm(AssetsTerm.from(type, member));
        }

        return term;
    }

    public static MultiAssetsTerm from(String type, List<String> tenantId) {
        MultiAssetsTerm term = new MultiAssetsTerm();

        if (tenantId.size() > 1) {
            term.addTerms(tenantId
                .stream()
                .map(_member -> AssetsTerm.from(type, _member))
                .collect(Collectors.toList()));
        } else {
            term.addTerm(AssetsTerm.from(type, tenantId.get(0)));
        }

        return term;
    }
}
