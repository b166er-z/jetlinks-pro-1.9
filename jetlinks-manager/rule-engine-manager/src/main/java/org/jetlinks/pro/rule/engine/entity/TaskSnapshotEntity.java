package org.jetlinks.pro.rule.engine.entity;

import lombok.Getter;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.ezorm.rdb.mapping.annotation.EnumCodec;
import org.hswebframework.ezorm.rdb.mapping.annotation.JsonCodec;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.generator.Generators;
import org.jetlinks.rule.engine.api.scheduler.ScheduleJob;
import org.jetlinks.rule.engine.api.task.Task;
import org.jetlinks.rule.engine.api.task.TaskSnapshot;

import javax.persistence.Column;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "rule_task_snapshot")
public class TaskSnapshotEntity extends GenericEntity<String> {

    @Column(length = 64, nullable = false)
    private String instanceId;

    @Column(length = 128, nullable = false)
    private String schedulerId;

    @Column(length = 128, nullable = false)
    private String workerId;

    @Column(length = 64, nullable = false)
    private String nodeId;

    @Column(length = 64, nullable = false)
    private String executor;

    @Column
    private String name;

    @Column
    @JsonCodec
    @ColumnType(jdbcType = JDBCType.CLOB, javaType = String.class)
    private ScheduleJob job;

    @Column(nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long startTime;

    @Column(nullable = false)
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long lastStateTime;

    @Column(nullable = false)
    @EnumCodec
    @ColumnType(javaType = String.class)
    @DefaultValue("shutdown")
    private Task.State state;

    public TaskSnapshot toSnapshot(){
        return FastBeanCopier.copy(this,new TaskSnapshot());
    }

    public static TaskSnapshotEntity of(TaskSnapshot snapshot) {
        TaskSnapshotEntity entity = FastBeanCopier.copy(snapshot, new TaskSnapshotEntity());
        entity.setExecutor(snapshot.getJob().getExecutor());
        entity.setNodeId(snapshot.getJob().getNodeId());

        return entity;
    }
}
