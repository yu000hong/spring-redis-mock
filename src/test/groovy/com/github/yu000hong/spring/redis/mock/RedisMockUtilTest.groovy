package com.github.yu000hong.spring.redis.mock

import org.testng.annotations.Test

import java.nio.charset.StandardCharsets

import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertNull

class RedisMockUtilTest {

    @Test
    public void testSerialize() {
        assertNull(RedisMockUtil.serialize(null))
        def string = 'hello world~'
        def bytes = string.getBytes(StandardCharsets.UTF_8)
        assertEquals(RedisMockUtil.serialize(string), bytes)
    }

    @Test
    public void testUnserializeBytes() {
        assertNull(RedisMockUtil.unserialize((byte[]) null))
        def string = 'hello world~'
        def bytes = string.getBytes(StandardCharsets.UTF_8)
        assertEquals(RedisMockUtil.unserialize(bytes), string)
    }

    @Test
    public void testUnserializeBytesArray() {
        def string1 = 'hello'
        def string2 = 'world'
        def string3 = '~'
        def bytes1 = string1.getBytes(StandardCharsets.UTF_8)
        def bytes2 = string2.getBytes(StandardCharsets.UTF_8)
        def bytes3 = string3.getBytes(StandardCharsets.UTF_8)
        def stringArray = [string1, string2, string3] as String[]
        assertEquals(RedisMockUtil.unserialize(bytes1, bytes2, bytes3), stringArray)
    }

    @Test
    public void testParseParameter() {
        def string1 = 'hello'
        def string2 = 'world'
        def string3 = '~'
        byte[] bytes1 = string1.getBytes(StandardCharsets.UTF_8)
        def bytes2 = string2.getBytes(StandardCharsets.UTF_8)
        def bytes3 = string3.getBytes(StandardCharsets.UTF_8)
        def parameter = RedisMockUtil.parseParameter(bytes1, bytes2, bytes3)
        assertEquals(parameter.param, string1)
        assertEquals(parameter.params.size(), 2)
        assertEquals(parameter.params[0], string2)
        assertEquals(parameter.params[1], string3)
        parameter = RedisMockUtil.parseParameter([bytes1] as byte[][])
        assertEquals(parameter.param, string1)
        assertEquals(parameter.params.size(), 0)
    }

}
