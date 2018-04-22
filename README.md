# Demo using FoundationDB + Spring WebFlux.fn


Install fdb-client locally until [it's published to Maven Central](https://github.com/apple/foundationdb/issues/219)

```
mvn validate -P install-fdb
```

[Install](https://apple.github.io/foundationdb/downloads.html) and [start](https://apple.github.io/foundationdb/administration.html#starting-and-stopping) FoundationDB


```
$ curl -v localhost:8080
> GET / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 404 Not Found
< content-length: 0
< 
```

```
$ curl localhost:8080 -v -H "Content-Type: text/plain" -d World
> POST / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> Content-Type: text/plain
> Content-Length: 5
> 
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8
< 
```

```
$ curl -v localhost:8080
> GET / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8
< 
Hello World
```

```
$ curl -v -XDELETE localhost:8080
> DELETE / HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 204 No Content
< 
```

## Class Scheduling in Spring WebFlux

https://apple.github.io/foundationdb/class-scheduling-java.html


```
$ curl -v localhost:8080/availableClasses
> GET /availableClasses HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: text/event-stream
< 
data:10:00 alg 101

data:10:00 alg 201

data:10:00 alg 301

data:10:00 alg for dummies

...
```

```
$ curl -v -XPOST localhost:8080/signup/foo/2:00%20bio%20for%20dummies
> POST /signup/foo/2:00%20bio%20for%20dummies HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8
< 
99 seats left

$ curl -v -XPOST localhost:8080/signup/foo/2:00%20bio%20for%20dummies
> POST /signup/foo/2:00%20bio%20for%20dummies HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 400 Bad Request
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8
< 
already signed up
```

```
$ curl -v -XPOST localhost:8080/drop/foo/2:00%20bio%20for%20dummies
> POST /drop/foo/2:00%20bio%20for%20dummies HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 200 OK
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8
< 
100 seats left

$ curl -v -XPOST localhost:8080/drop/foo/2:00%20bio%20for%20dummies
> POST /drop/foo/2:00%20bio%20for%20dummies HTTP/1.1
> Host: localhost:8080
> User-Agent: curl/7.54.0
> Accept: */*
> 
< HTTP/1.1 400 Bad Request
< transfer-encoding: chunked
< Content-Type: text/plain;charset=UTF-8
< 
not taking this class
```