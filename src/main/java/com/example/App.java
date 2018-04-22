package com.example;

import java.util.Optional;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.FDB;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.ipc.netty.http.server.HttpServer;

import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ReactorHttpHandlerAdapter;
import org.springframework.web.reactive.function.server.HandlerStrategies;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerResponse;

public class App {

	static RouterFunction<ServerResponse> routes(Database database) {
		return new HelloRouter(database).routes()
				.and(new ClassSchedulingRouter(database).routes());
	}

	public static void main(String[] args) throws Exception {
		long begin = System.currentTimeMillis();
		int port = Optional.ofNullable(System.getenv("PORT")) //
				.map(Integer::parseInt) //
				.orElse(8080);
		HttpServer httpServer = HttpServer.create("0.0.0.0", port);
		FDB fdb = FDB.selectAPIVersion(510);
		try (Database database = fdb.open()) {
			Logger logger = LoggerFactory.getLogger(App.class);
			httpServer.startRouterAndAwait(routes -> {
				HttpHandler httpHandler = RouterFunctions.toHttpHandler(
						App.routes(database), HandlerStrategies.builder().build());
				routes.route(x -> true, new ReactorHttpHandlerAdapter(httpHandler));
			}, context -> {
				long elapsed = System.currentTimeMillis() - begin;
				logger.info("Started in {} seconds", elapsed / 1000.0);
			});
			logger.info("Closing...");
		}
	}
}
