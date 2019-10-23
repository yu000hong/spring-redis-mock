# spring-redis-mock

`spring-redis-mock` is an adapter of `Spring Data Redis` for `redis-mock-java`.

The purpose of this project is:

> Make your RedisTemplate-based unit tests faster and more controllable.

**redis-mock-java**

An in-memory redis-compatible implementation written in pure Java.

### Usage

**build.gradle**

```
repositories {
    maven {
        url 'https://jitpack.io'
    }
}

dependencies {
    compile group: 'com.github.yu000hong', name: 'spring-redis-mock', version: '2.0.1'
}
```



# Reference

[redis-mock-java](https://github.com/wilkenstein/redis-mock-java)

[Spring Data Redis](http://projects.spring.io/spring-data-redis/)
