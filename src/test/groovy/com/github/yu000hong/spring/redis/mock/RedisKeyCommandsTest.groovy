package com.github.yu000hong.spring.redis.mock

import org.rarefiedredis.redis.NotImplementedException
import org.rarefiedredis.redis.RedisMock
import org.springframework.data.redis.connection.DataType
import org.testng.annotations.BeforeMethod
import org.testng.annotations.Test

import static com.github.yu000hong.spring.redis.mock.RedisMockUtil.serialize
import static org.testng.Assert.assertEquals
import static org.testng.Assert.assertFalse
import static org.testng.Assert.assertNull
import static org.testng.Assert.assertTrue

/**
 * Part of the RedisConnection implementation: RedisKeyCommands
 */
class RedisKeyCommandsTest {
    private RedisMock redisMock
    private RedisMockConnection connection
    private static final String KEY = 'name'
    private static final String VALUE = 'kitty'

    @BeforeMethod
    public void prepare() {
        redisMock = new RedisMock()
        connection = new RedisMockConnection(redisMock)
    }

    @Test
    public void testExists() {
        assertFalse(connection.exists(serialize(KEY)))
        redisMock.set(KEY, VALUE)
        assertTrue(connection.exists(serialize(KEY)))
    }

    @Test
    public void testExistsOfPipeline() {
        connection.openPipeline()
        assertNull(connection.exists(serialize(KEY)))
        def results = connection.closePipeline()
        assertEquals(results[0], false)
        redisMock.set(KEY, VALUE)
        connection.openPipeline()
        assertNull(connection.exists(serialize(KEY)))
        results = connection.closePipeline()
        assertEquals(results[0], true)
    }

    @Test
    public void testExistsOfTransaction() {
        connection.multi()
        assertNull(connection.exists(serialize(KEY)))
        def results = connection.exec()
        assertEquals(results[0], false)
        redisMock.set(KEY, VALUE)
        connection.multi()
        assertNull(connection.exists(serialize(KEY)))
        results = connection.exec()
        assertEquals(results[0], true)
    }

    @Test
    public void testDel() {
        redisMock.set('k1', VALUE)
        redisMock.set('k2', VALUE)
        redisMock.set('k3', VALUE)
        assertEquals(connection.del(wrapArray(serialize('k0'))), 0)
        assertEquals(connection.del(serialize('k0'), serialize('k1')), 1)
        assertEquals(connection.del(serialize('k0'), serialize('k2'), serialize('k3')), 2)
    }

    @Test
    public void testDelOfPipeline() {
        redisMock.set('k1', VALUE)
        redisMock.set('k2', VALUE)
        redisMock.set('k3', VALUE)
        connection.openPipeline()
        assertNull(connection.del(wrapArray(serialize('k0'))))
        assertNull(connection.del(serialize('k0'), serialize('k1')))
        assertNull(connection.del(serialize('k0'), serialize('k2'), serialize('k3')))
        def results = connection.closePipeline()
        assertEquals(results.size(), 3)
        assertEquals(results[0], 0)
        assertEquals(results[1], 1)
        assertEquals(results[2], 2)
    }

    @Test
    public void testDelOfTransaction() {
        redisMock.set('k1', VALUE)
        redisMock.set('k2', VALUE)
        redisMock.set('k3', VALUE)
        connection.multi()
        assertNull(connection.del(wrapArray(serialize('k0'))))
        assertNull(connection.del(serialize('k0'), serialize('k1')))
        assertNull(connection.del(serialize('k0'), serialize('k2'), serialize('k3')))
        def results = connection.exec()
        assertEquals(results.size(), 3)
        assertEquals(results[0], 0)
        assertEquals(results[1], 1)
        assertEquals(results[2], 2)
    }

    @Test
    public void testType() {
        redisMock.set('k1', VALUE)
        redisMock.hset('k2', 'hashKey', 'hashValue')
        redisMock.incr('k3')
        redisMock.lpush('k4', 'hi')
        redisMock.zadd('k5', 1.0D, 'member')
        assertEquals(connection.type(serialize('k1')), DataType.STRING)
        assertEquals(connection.type(serialize('k2')), DataType.HASH)
        assertEquals(connection.type(serialize('k3')), DataType.STRING)
        assertEquals(connection.type(serialize('k4')), DataType.LIST)
        assertEquals(connection.type(serialize('k5')), DataType.ZSET)
        assertEquals(connection.type(serialize('k0')), DataType.NONE)
    }

    @Test
    public void testTypeOfPipeline() {
        redisMock.set('k1', VALUE)
        redisMock.hset('k2', 'hashKey', 'hashValue')
        redisMock.incr('k3')
        redisMock.lpush('k4', 'hi')
        redisMock.zadd('k5', 1.0D, 'member')
        connection.openPipeline()
        assertNull(connection.type(serialize('k1')))
        assertNull(connection.type(serialize('k2')))
        assertNull(connection.type(serialize('k3')))
        assertNull(connection.type(serialize('k4')))
        assertNull(connection.type(serialize('k5')))
        assertNull(connection.type(serialize('k0')))
        def results = connection.closePipeline()
        assertEquals(results.size(), 6)
        assertEquals(results[0], DataType.STRING)
        assertEquals(results[1], DataType.HASH)
        assertEquals(results[2], DataType.STRING)
        assertEquals(results[3], DataType.LIST)
        assertEquals(results[4], DataType.ZSET)
        assertEquals(results[5], DataType.NONE)
    }

    @Test
    public void testTypeOfTransaction() {
        redisMock.set('k1', VALUE)
        redisMock.hset('k2', 'hashKey', 'hashValue')
        redisMock.incr('k3')
        redisMock.lpush('k4', 'hi')
        redisMock.zadd('k5', 1.0D, 'member')
        connection.multi()
        assertNull(connection.type(serialize('k1')))
        assertNull(connection.type(serialize('k2')))
        assertNull(connection.type(serialize('k3')))
        assertNull(connection.type(serialize('k4')))
        assertNull(connection.type(serialize('k5')))
        assertNull(connection.type(serialize('k0')))
        def results = connection.exec()
        assertEquals(results.size(), 6)
        assertEquals(results[0], DataType.STRING)
        assertEquals(results[1], DataType.HASH)
        assertEquals(results[2], DataType.STRING)
        assertEquals(results[3], DataType.LIST)
        assertEquals(results[4], DataType.ZSET)
        assertEquals(results[5], DataType.NONE)
    }

    @Test(expectedExceptions = NotImplementedException)
    public void testKeys() {
        connection.keys(serialize('*'))
    }

    @Test
    public void testScan() {
//        connection.scan(ScanOptions.NONE)
//        TODO
    }

    @Test(expectedExceptions = NotImplementedException)
    public void testRandomKey() {
        connection.randomKey()
    }

    @Test(expectedExceptions = NotImplementedException)
    public void testRename() {
        def oldKey = 'KEY_OLD'
        def newKey = 'KEY_NEW'
        connection.rename(serialize(oldKey), serialize(newKey))
    }

    @Test(expectedExceptions = NotImplementedException)
    public void testRenameNX() {
        def oldKey = 'KEY_OLD'
        def newKey = 'KEY_NEW'
        connection.renameNX(serialize(oldKey), serialize(newKey))
    }

    private static byte[][] wrapArray(byte[] bytes) {
        return [bytes] as byte[][]
    }

}
