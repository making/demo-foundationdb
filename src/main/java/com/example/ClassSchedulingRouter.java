package com.example;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import com.apple.foundationdb.Database;
import com.apple.foundationdb.KeyValue;
import com.apple.foundationdb.TransactionContext;
import com.apple.foundationdb.async.AsyncIterator;
import com.apple.foundationdb.tuple.Tuple;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;

import static org.springframework.web.reactive.function.server.RequestPredicates.GET;
import static org.springframework.web.reactive.function.server.RequestPredicates.POST;
import static org.springframework.web.reactive.function.server.RouterFunctions.route;

/**
 * https://apple.github.io/foundationdb/class-scheduling-java.html
 */
public class ClassSchedulingRouter {
	private final Database database;
	private final List<String> classNames;
	private final Logger log = LoggerFactory.getLogger(ClassSchedulingRouter.class);

	// Generate 1,620 classes like '9:00 chem for dummies'
	private static final List<String> levels = Arrays.asList("intro", "for dummies",
			"remedial", "101", "201", "301", "mastery", "lab", "seminar");
	private static final List<String> types = Arrays.asList("chem", "bio", "cs",
			"geometry", "calc", "alg", "film", "music", "art", "dance");
	private static final List<String> times = Arrays.asList("2:00", "3:00", "4:00",
			"5:00", "6:00", "7:00", "8:00", "9:00", "10:00", "11:00", "12:00", "13:00",
			"14:00", "15:00", "16:00", "17:00", "18:00", "19:00");

	public ClassSchedulingRouter(Database database) {
		this.database = database;
		this.classNames = initClassNames();
		this.init();
	}

	public RouterFunction<ServerResponse> routes() {
		return route(GET("/availableClasses"), this::availableClasses)
				.andRoute(POST("/signup/{student}/{class}"), this::signup)
				.andRoute(POST("/drop/{student}/{class}"), this::drop);
	}

	/**
	 * Listing available classes
	 */
	Mono<ServerResponse> availableClasses(ServerRequest req) {
		Flux<KeyValue> flux = Flux.create(emitter -> this.database.readAsync(tx -> {
			AsyncIterator<KeyValue> iterator = tx.getRange(Tuple.from("class").range())
					.iterator();
			AtomicBoolean repeat = new AtomicBoolean(true);
			Mono<Boolean> onHasNext = Mono.fromCompletionStage(iterator.onHasNext()) //
					.doOnCancel(() -> {
						emitter.complete();
						repeat.set(false);
					}).map(hasNext -> {
						emitter.next(iterator.next());
						return hasNext;
					});
			emitter.onCancel(iterator::cancel);
			return onHasNext.repeat(repeat::get).collectList().toFuture();
		}));
		Flux<String> body = flux.filter(kv -> decodeInt(kv.getValue()) > 0) //
				.map(kv -> Tuple.fromBytes(kv.getKey()).getString(1)) //
				.log("availableClasses:body");
		return ServerResponse.ok().contentType(MediaType.TEXT_EVENT_STREAM) //
				.body(body, String.class);
	}

	/**
	 * Signing up for a class
	 */
	Mono<ServerResponse> signup(ServerRequest req) {
		String student = req.pathVariable("student");
		String className = req.pathVariable("class");
		CompletableFuture<ServerResponse> res = this.database.runAsync(tx -> {
			byte[] rec = Tuple.from("attends", student, className).pack();
			return Mono.fromCompletionStage(tx.get(rec)) //
					.log("signup:attends")
					.flatMap(x -> ServerResponse.badRequest()
							.syncBody("already signed up")) //
					.switchIfEmpty(Mono.defer(() -> {
						byte[] classKey = Tuple.from("class", className).pack();
						Mono<Integer> seatsLeft = Mono
								.fromCompletionStage(tx.get(classKey))
								.map(this::decodeInt) //
								.log("signup:seats");
						return seatsLeft.filter(seat -> seat > 0) //
								.log("signup:filter") //
								.map(seat -> seat - 1) //
								.flatMap(seat -> {
									tx.set(classKey, encodeInt(seat));
									tx.set(rec, Tuple.from("").pack());
									return ServerResponse.ok()
											.syncBody(seat + " seats left");
								}) //
								.switchIfEmpty(Mono.defer(() -> ServerResponse
										.badRequest().syncBody("No remaining seats")));
					})).toFuture();
		});
		return Mono.fromCompletionStage(res);
	}

	/**
	 * Dropping a class
	 */
	Mono<ServerResponse> drop(ServerRequest req) {
		String student = req.pathVariable("student");
		String className = req.pathVariable("class");
		CompletableFuture<ServerResponse> res = this.database.runAsync(tx -> {
			byte[] rec = Tuple.from("attends", student, className).pack();
			return Mono.fromCompletionStage(tx.get(rec)) //
					.log("drop:attends") //
					.flatMap(x -> {
						byte[] classKey = Tuple.from("class", className).pack();
						return Mono.fromCompletionStage(tx.get(classKey)) ///
								.map(this::decodeInt) //
								.map(seat -> seat + 1) //
								.log("drop:seats") //
								.flatMap(seat -> {
									tx.set(classKey, encodeInt(seat));
									tx.clear(rec);
									return ServerResponse.ok()
											.syncBody(seat + " seats left");
								});
					}) //
					.switchIfEmpty(Mono.defer(() -> ServerResponse.badRequest()
							.syncBody("not taking this class")))
					.toFuture();
		});
		return Mono.fromCompletionStage(res);
	}

	/**
	 * Initializing the database
	 */
	private void init() {
		log.info("Initializing db...");
		this.database.run(tx -> {
			tx.clear(Tuple.from("attends").range());
			tx.clear(Tuple.from("class").range());
			for (String className : this.classNames) {
				this.addClass(tx, className);
			}
			return null;
		});
	}

	private void addClass(TransactionContext db, final String c) {
		db.run(tx -> {
			tx.set(Tuple.from("class", c).pack(), encodeInt(100));
			return null;
		});
	}

	private static List<String> initClassNames() {
		List<String> classNames = new ArrayList<>();
		for (String level : levels) {
			for (String type : types) {
				for (String time : times) {
					classNames.add(time + " " + type + " " + level);
				}
			}
		}
		return classNames;
	}

	private byte[] encodeInt(int value) {
		byte[] output = new byte[4];
		ByteBuffer.wrap(output).putInt(value);
		return output;
	}

	private int decodeInt(byte[] value) {
		if (value.length != 4)
			throw new IllegalArgumentException("Array must be of size 4");
		return ByteBuffer.wrap(value).getInt();
	}
}
