package org.jetlinks.pro.tenant.supports;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.jetlinks.pro.tenant.Tenant;

import java.io.Serializable;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class DefaultTenant implements Tenant, Serializable {
    private static final long serialVersionUID = -6849794470754667710L;

    private String id;

    private String name;


}
