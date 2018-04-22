package com.example;

import java.util.concurrent.CompletableFuture;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.tuple.Tuple;
import reactor.core.publisher.Mono;

import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.*;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

public class HelloRouter {

	private final Database database;

	public HelloRouter(Database database) {
		this.database = database;
	}

	public RouterFunction<ServerResponse> routes() {
		return route(GET("/"), this::get) //
				.andRoute(POST("/"), this::set)//
				.andRoute(DELETE("/"), this::clear);
	}

	Mono<ServerResponse> get(ServerRequest req) {
		final String key = "hello";
		CompletableFuture<byte[]> get = this.database
				.readAsync(tx -> tx.get(Tuple.from(key).pack()));
		return Mono.fromCompletionStage(get).log("get")
				.map(result -> Tuple.fromBytes(result).getString(0)) //
				.map(v -> "Hello " + v) //
				.flatMap(body -> ServerResponse.ok().syncBody(body)) //
				.switchIfEmpty(ServerResponse.notFound().build());
	}

	Mono<ServerResponse> set(ServerRequest req) {
		final String key = "hello";
		CompletableFuture<String> set = this.database.runAsync(tx -> {
			Mono<String> body = req.bodyToMono(String.class);
			return body.doOnNext(
					value -> tx.set(Tuple.from(key).pack(), Tuple.from(value).pack()))
					.log("set").toFuture();
		});
		return ServerResponse.ok().body(Mono.fromCompletionStage(set), String.class);
	}

	Mono<ServerResponse> clear(ServerRequest req) {
		final String key = "hello";
		CompletableFuture<Void> clear = this.database.runAsync(tx -> {
			tx.clear(Tuple.from(key).pack());
			return CompletableFuture.completedFuture(null);
		});
		return ServerResponse.noContent()
				.build(Mono.fromCompletionStage(clear).log("clear"));
	}
}
