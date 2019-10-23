package com.github.yu000hong.spring.redis.mock

import java.nio.charset.StandardCharsets

class RedisMockUtil {

    @SuppressWarnings('ReturnsNullInsteadOfEmptyArray')
    static final byte[] serialize(String value) {
        if (value == null) {
            return null
        }
        return value.getBytes(StandardCharsets.UTF_8)
    }

    static final String unserialize(byte[] value) {
        if (value == null) {
            return null
        }
        return new String(value, StandardCharsets.UTF_8)
    }

    static final String[] unserialize(byte[] ... values) {
        assert values != null
        def array = new String[values.length]
        values.eachWithIndex { value, i ->
            array[i] = unserialize(value)
        }
        return array
    }

    static MockParameter parseParameter(byte[] ... values) {
        assert values != null
        def strings = unserialize(values)
        def element = strings[0]
        def elements = []
        if (strings.length > 1) {
            (1..strings.length - 1).each { int i ->
                elements << strings[i]
            }
        }
        return new MockParameter(param: element, params: elements as String[])
    }

    static String toString(double value) {
        return String.valueOf(value)
    }

    static String toString(long value) {
        return String.valueOf(value)
    }

    static String toString(int value) {
        return String.valueOf(value)
    }

    static String toString(byte[] value) {
        return new String(value)
    }

}
