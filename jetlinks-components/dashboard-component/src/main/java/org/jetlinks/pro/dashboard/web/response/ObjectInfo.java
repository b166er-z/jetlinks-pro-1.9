package org.jetlinks.pro.dashboard.web.response;

import lombok.Getter;
import lombok.Setter;
import org.jetlinks.pro.dashboard.Dashboard;
import org.jetlinks.pro.dashboard.DashboardObject;
import reactor.core.publisher.Mono;

import java.util.List;

@Getter
@Setter
public class ObjectInfo {

    private String id;

    private String name;


    public static ObjectInfo of(DashboardObject object){
        ObjectInfo objectInfo=new ObjectInfo();
        objectInfo.setName(object.getDefinition().getName());
        objectInfo.setId(object.getDefinition().getId());

        return objectInfo;
    }

}
