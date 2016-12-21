package com.github.yu000hong.spring.redis.mock

/**
 * RedisMock参数
 * 有的方法接受一个参数带一个可变参数组, 而Spring-Data-Redis提供的是一个数组, 需要进行适配
 */
class MockParameter {
    String param //第一个固定参数
    String[] params//第二个可变参数
}
