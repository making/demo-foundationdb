```
mvn validate -P install-fdb
```

Start [FoundationDB](https://apple.github.io/foundationdb/administration.html#starting-and-stopping)


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