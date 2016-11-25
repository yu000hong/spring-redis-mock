package com.github.yu000hong.spring.redis.mock

import java.nio.charset.StandardCharsets

class RedisMockUtil {

    public static final byte[] serialize(String value) {
        if (value == null) {
            return null
        }
        return value.getBytes(StandardCharsets.UTF_8)
    }

    public static final String unserialize(byte[] value) {
        if (value == null) {
            return null
        }
        return new String(value, StandardCharsets.UTF_8)
    }

    public static final String[] unserialize(byte[] ... vals) {
        def array = new String[vals.length]
        def i = 0
        vals.each { val ->
            array[i++] = unserialize(val)
        }
        return array
    }

    public static MockParameter parseParameter(byte[] ... values) {
        def strings = unserialize(values)
        def element = strings[0]
        def elements = []
        (1..strings.length - 1).each { int i ->
            elements << strings[i]
        }
        return new MockParameter(param: element, params: elements as String[])
    }

}