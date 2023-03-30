package org.jetlinks.pro.rule.engine.executor;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SwitchTaskExecutorProviderTest {


    List<SwitchTaskExecutorProvider.SwitchCondition> doSwitch(String property,
                                                              Map<String, Object> context,
                                                              String... rules) {
        SwitchTaskExecutorProvider.SwitchConfig config = new SwitchTaskExecutorProvider.SwitchConfig();
        config.setProperty(property);
        config.setPropertyType("jsonata");
        config.setCheckAll(true);

        config.setConditions(new ArrayList<>());

        for (String ruleStr : rules) {
            String[] arr = ruleStr.split(",");
            SwitchTaskExecutorProvider.SwitchCondition rule = new SwitchTaskExecutorProvider.SwitchCondition();
            rule.setType(arr[0]);
            rule.setValue(arr[1]);
            rule.setValueType(arr[2]);
            config.getConditions().add(rule);
        }

        config.prepare();

         return config.match(context);
    }

    @Test
    void testEq() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("success"
            ,Collections.singletonMap("success", true)
            ,"eq,false,str","eq,true,str"
            );

        assertEquals(matched.size(), 1);
        assertEquals(matched.get(0).getValue(), "true");
    }

    @Test
    void testGt() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("val"
            ,Collections.singletonMap("val", 10)
            ,"gt,5,str","gte,10,str"
        );

        assertEquals(matched.size(), 2);

    }

    @Test
    void testElseNoMatch() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("val"
            ,Collections.singletonMap("val", 10)
            ,"lt,11,str","lte,10,str","else,,str"
        );

        assertEquals(matched.size(), 2);
    }

    @Test
    void testElseMatch() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("val"
            ,Collections.singletonMap("val", 10)
            ,"lt,1,str","else,,str"
        );

        assertEquals(matched.size(), 1);
        assertEquals(matched.get(0).getType(), "else");
    }

    @Test
    void testLt() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("val"
            ,Collections.singletonMap("val", 10)
            ,"lt,11,str","lte,10,str"
        );

        assertEquals(matched.size(), 2);

    }

    @Test
    void testHasKey() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("val"
            ,Collections.singletonMap("val", Collections.singletonMap("test","value"))
            ,"hask,test,str"
        );

        assertEquals(matched.size(), 1);

    }

    @Test
    void testJsonata() {

        List<SwitchTaskExecutorProvider.SwitchCondition> matched = doSwitch("val"
            ,Collections.singletonMap("val", 12)
            ,"jsonata_exp,val<=12,jsonata"
        );

        assertEquals(matched.size(), 1);

    }
}