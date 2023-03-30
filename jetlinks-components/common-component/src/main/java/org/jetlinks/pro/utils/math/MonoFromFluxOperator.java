package org.jetlinks.pro.utils.math;

import java.util.Objects;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.reactivestreams.Publisher;
import reactor.core.Scannable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

abstract class MonoFromFluxOperator<I, O> extends Mono<O> implements Scannable {

	protected final Flux<? extends I> source;

	protected MonoFromFluxOperator(Flux<? extends I> source) {
		this.source = Objects.requireNonNull(source);
	}

	@Override
	@Nullable
	public Object scanUnsafe(@Nonnull Attr key) {
		if (key == Attr.PREFETCH) return Integer.MAX_VALUE;
		if (key == Attr.PARENT) return source;
		return null;
	}

}