package org.jetlinks.pro.network.manager.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.crud.web.reactive.ReactiveSaveController;
import org.hswebframework.web.exception.NotFoundException;
import org.jetlinks.pro.network.manager.entity.SimulatorEntity;
import org.jetlinks.pro.network.manager.enums.SimulatorStatus;
import org.jetlinks.pro.simulator.core.Session;
import org.jetlinks.pro.simulator.core.Simulator;
import org.jetlinks.pro.simulator.core.SimulatorManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/network/simulator")
@Resource(id = "network-simulator", name = "网络模拟器")
@Authorize
@Tag(name = "网络模拟器")
public class SimulatorController implements ReactiveSaveController<SimulatorEntity, String> {

    @Autowired
    private ReactiveRepository<SimulatorEntity, String> reactiveRepository;

    @Autowired
    private SimulatorManager simulatorManager;

    @Override
    public ReactiveRepository<SimulatorEntity, String> getRepository() {
        return reactiveRepository;
    }

    @GetMapping("/_query")
    @QueryAction
    @QueryOperation(summary = "查询全部模拟器")
    public Flux<SimulatorEntity> querySimulator(@Parameter(hidden = true) QueryParamEntity query) {

        return reactiveRepository
            .createQuery()
            .setParam(query)
            .fetch()
            .flatMap(entity -> simulatorManager
                .getSimulator(entity.getId())
                .doOnNext(simulator -> entity.setStatus(simulator.isRunning() ? SimulatorStatus.running : SimulatorStatus.stop))
                .thenReturn(entity));
    }

    @DeleteMapping("/{id}")
    @DeleteAction
    @Operation(summary = "删除模拟器")
    public Mono<Void> deleteSimulator(@PathVariable String id) {

        return simulatorManager.getSimulator(id)
            .flatMap(Simulator::shutdown)
            .then(reactiveRepository.deleteById(id))
            .then();
    }

    @GetMapping("/{id}/sessions/{offset}/{total}")
    @QueryAction
    @Operation(summary = "获取模拟器中的会话")
    public Flux<SimulatorSessionInfo> getSessions(@PathVariable
                                                  @Parameter(description = "模拟器ID") String id,
                                                  @PathVariable
                                                  @Parameter(description = "偏移量") int offset,
                                                  @PathVariable
                                                  @Parameter(description = "总数") int total) {
        return simulatorManager
            .getSimulator(id)
            .flatMapMany(simulator -> simulator.getSessions(offset, total)
                .map(session -> SimulatorSessionInfo.of(simulator.getType(), session)));
    }

    @PostMapping("/{id}/{sessionId}")
    @SaveAction
    @Operation(summary = "发送消息给会话")
    public Mono<Void> publishMessage(@PathVariable
                                     @Parameter(description = "模拟器ID") String id,
                                     @PathVariable
                                     @Parameter(description = "会话ID") String sessionId,
                                     @RequestBody
                                     @Parameter(description = "消息内容") String body) {
        return simulatorManager
            .getSimulator(id)
            .flatMap(simulator -> simulator.getSession(sessionId))
            .flatMap(session -> session.publishAsync(body));
    }

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimulatorSessionInfo {
        private String id;

        private String type;

        private int index;

        private boolean connected;

        private long connectTime;

        public static SimulatorSessionInfo of(String type, Session session) {
            return new SimulatorSessionInfo(
                session.getId(),
                type,
                session.getIndex(),
                session.isConnected(),
                session.getConnectTime());
        }

    }

    @PostMapping("/{id}/stop")
    @ResourceAction(id = "stop", name = "停止")
    @Operation(summary = "停止运行")
    public Mono<Void> stop(@PathVariable
                           @Parameter(description = "模拟器ID") String id) {
        return simulatorManager
            .getSimulator(id)
            .flatMap(Simulator::shutdown)
            .then(simulatorManager.remove(id));
    }

    @PostMapping("/{id}/start")
    @ResourceAction(id = "start", name = "启动")
    @Operation(summary = "启动模拟器")
    public Mono<Void> start(@PathVariable
                            @Parameter(description = "模拟器ID") String id) {
        return simulatorManager
            .getSimulator(id)
            .switchIfEmpty(Mono.defer(() -> reactiveRepository
                .findById(id)
                .flatMap(entity -> simulatorManager.createSimulator(entity.toConfig()))))
            .switchIfEmpty(Mono.error(() -> new NotFoundException("模拟器不存在")))
            .flatMap(Simulator::start);
    }

    @GetMapping("/{id}/state")
    @QueryAction
    @Operation(summary = "获取模拟器当前状态")
    public Mono<Simulator.State> getState(@PathVariable
                                          @Parameter(description = "模拟器ID") String id) {
        return simulatorManager
            .getSimulator(id)
            .flatMap(Simulator::state);
    }

}
