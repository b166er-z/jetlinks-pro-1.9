package org.jetlinks.pro.utils.math;

import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.util.Comparator;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class MathFluxTest {

    @Test
    void testSkewness() {

        MathFlux
            .skewness(Flux.just(1, 2, 3, 4, 5), Number::doubleValue)
            .as(StepVerifier::create)
            .expectNext(0D)
            .verifyComplete();

    }

    @Test
    void testSlope() {

        MathFlux
            .slope(Flux.just(1, 2, 3, 4, 5), Number::doubleValue)
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNext(1D)
            .verifyComplete();


        MathFlux
            .slope(Flux.just(5, 4, 3, 2, 1), Number::doubleValue)
            .doOnNext(System.out::println)
            .as(StepVerifier::create)
            .expectNext(-1D)
            .verifyComplete();

    }

    @Test
    void testRange() {
        MathFlux
            .rangeNumber(Flux.just(1, 2, 3, 4, 5), Function.identity())
            .as(StepVerifier::create)
            .expectNext(4)
            .verifyComplete();
    }
}