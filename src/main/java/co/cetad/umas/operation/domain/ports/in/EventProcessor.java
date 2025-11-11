package co.cetad.umas.operation.domain.ports.in;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface EventProcessor<T, I> {

    CompletableFuture<T> process(I event);

}