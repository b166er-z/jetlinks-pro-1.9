package org.jetlinks.pro.gateway.external.socket;

import com.alibaba.fastjson.JSON;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.logger.ReactiveLogger;
import org.jetlinks.pro.gateway.external.Message;
import org.jetlinks.pro.gateway.external.MessagingManager;
import org.jetlinks.pro.gateway.external.SubscribeRequest;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.Disposable;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@AllArgsConstructor
@Slf4j
public class WebSocketMessagingHandler implements WebSocketHandler {

    private final MessagingManager messagingManager;

    private final WebSocketAuthenticationHandler filter;

    @Override
    @Nonnull
    public Mono<Void> handle(@Nonnull WebSocketSession session) {

        Map<String, Disposable> subs = new ConcurrentHashMap<>();

        return filter
            .handle(session)
            .switchIfEmpty(session
                               .send(Mono.just(session.textMessage(JSON.toJSONString(Message.authError()))))
                               .then(session.close(CloseStatus.BAD_DATA))
                               .then(Mono.empty()))
            .flatMap(auth -> session
                .receive()
                .doOnNext(message -> {
                    try {
                        MessagingRequest request = JSON.parseObject(message.getPayloadAsText(), MessagingRequest.class);
                        if (request == null || request.getType() == MessagingRequest.Type.ping) {
                            return;
                        }
                        if (StringUtils.isEmpty(request.getId())) {
                            session
                                .send(Mono.just(session.textMessage(JSON.toJSONString(
                                    Message.error(request.getType().name(), null, "id不能为空")
                                )))).subscribe();
                            return;
                        }
                        if (request.getType() == MessagingRequest.Type.sub) {
                            //重复订阅
                            Disposable old = subs.get(request.getId());
                            if (old != null && !old.isDisposed()) {
                                return;
                            }
                            Map<String, String> context = new HashMap<>();
                            context.put("userId", auth.getUser().getId());
                            context.put("userName", auth.getUser().getName());
                            Disposable sub = messagingManager
                                .subscribe(SubscribeRequest.of(request, auth))
                                .doOnEach(ReactiveLogger.onError(err -> log.error("{}", err.getMessage(), err)))
                                .onErrorResume(err -> Mono.just(Message.error(request.getId(), request.getTopic(), err)))
                                .map(msg -> session.textMessage(JSON.toJSONString(msg)))
                                .doOnComplete(() -> {
                                    log.debug("complete subscription:{}", request.getTopic());
                                    subs.remove(request.getId());
                                    Mono.just(session.textMessage(JSON.toJSONString(Message.complete(request.getId()))))
                                        .as(session::send)
                                        .subscribe();
                                })
                                .doOnCancel(() -> {
                                    log.debug("cancel subscription:{}", request.getTopic());
                                    subs.remove(request.getId());
                                })
                                .transform(session::send)
                                .subscriberContext(ReactiveLogger.start(context))
                                .subscriberContext(Context.of(Authentication.class, auth))
                                .subscribe();
                            if (!sub.isDisposed()) {
                                subs.put(request.getId(), sub);
                            }
                        } else if (request.getType() == MessagingRequest.Type.unsub) {
                            Optional.ofNullable(subs.remove(request.getId()))
                                    .ifPresent(Disposable::dispose);
                        } else {
                            session.send(Mono.just(session.textMessage(JSON.toJSONString(
                                Message.error(request.getId(), request.getTopic(), "不支持的类型:" + request.getType())
                            )))).subscribe();
                        }
                    } catch (Exception e) {
                        log.warn(e.getMessage(), e);
                        session.send(Mono.just(session.textMessage(JSON.toJSONString(
                            Message.error("illegal_argument", null, "消息格式错误")
                        )))).subscribe();
                    }
                })
                .then())
            .doFinally(r -> {
                subs.values().forEach(Disposable::dispose);
                subs.clear();
            });

    }
}
