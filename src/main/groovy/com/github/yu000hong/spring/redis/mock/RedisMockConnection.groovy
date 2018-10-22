package com.github.yu000hong.spring.redis.mock

import org.rarefiedredis.redis.IRedisClient
import org.rarefiedredis.redis.IRedisSortedSet
import org.rarefiedredis.redis.RedisMock
import org.springframework.data.redis.connection.AbstractRedisConnection
import org.springframework.data.redis.connection.DataType
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.RedisNode
import org.springframework.data.redis.connection.RedisPipelineException
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.connection.SortParameters
import org.springframework.data.redis.connection.Subscription
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.types.Expiration
import org.springframework.data.redis.core.types.RedisClientInfo
import org.springframework.util.Assert

import static com.github.yu000hong.spring.redis.mock.Converters.DO_NOTHING
import static com.github.yu000hong.spring.redis.mock.Converters.LONG_TO_BOOLEAN
import static com.github.yu000hong.spring.redis.mock.Converters.NULL
import static com.github.yu000hong.spring.redis.mock.Converters.STRINGLIST_TO_BYTESLIST
import static com.github.yu000hong.spring.redis.mock.Converters.STRINGMAP_TO_BYTESMAP
import static com.github.yu000hong.spring.redis.mock.Converters.STRINGSET_TO_BYTESSET
import static com.github.yu000hong.spring.redis.mock.Converters.STRINGS_TO_BYTESLIST
import static com.github.yu000hong.spring.redis.mock.Converters.STRING_TO_BYTES
import static com.github.yu000hong.spring.redis.mock.Converters.STRING_TO_DOUBLE
import static com.github.yu000hong.spring.redis.mock.RedisMockUtil.parseParameter
import static com.github.yu000hong.spring.redis.mock.RedisMockUtil.serialize
import static com.github.yu000hong.spring.redis.mock.RedisMockUtil.unserialize

class RedisMockConnection extends AbstractRedisConnection {
    private final RedisMock mock
    private boolean pipelined
    private IRedisClient multiMock
    private List<Object> pipelineResults
    private List<Closure> multiResultConverts

    RedisMockConnection(RedisMock mock) {
        Assert.notNull(mock, 'a not-null instance required')
        this.mock = mock
    }

    @Override
    boolean isClosed() {
        return false
    }

    @Override
    RedisMock getNativeConnection() {
        return mock
    }

    //region RedisTxCommands

    @Override
    boolean isQueueing() {
        return multiMock != null
    }

    @Override
    void multi() {
        if (isQueueing()) {
            throw new RuntimeException('ERR MULTI calls can not be nested')
        }
        multiMock = mock.multi()
        multiResultConverts = []
    }

    @Override
    List<Object> exec() {
        if (!isQueueing()) {
            throw new RuntimeException('ERR EXEC without MULTI')
        }
        def results = []
        def objects = multiMock.exec()
        if (objects == null) {
            results << null
        } else {
            //convert type
            (0..objects.size() - 1).each { i ->
                results << multiResultConverts[i].call(objects[i])
            }
        }
        multiMock = null
        multiResultConverts = null
        if (isPipelined()) {
            pipelineResults += results
            return null
        } else {
            return results
        }
    }

    @Override
    void discard() {
        if (!isQueueing()) {
            throw new RuntimeException('ERR DISCARD without MULTI')
        }
        multiMock.discard()
        multiResultConverts = null
        multiMock = null
    }

    @Override
    void watch(byte[] ... keys) {
        if (isQueueing()) {
            throw new RuntimeException('ERR WATCH inside MULTI is not allowed')
        }
        keys.each { bytes ->
            mock.watch(unserialize(bytes))
        }
    }

    @Override
    void unwatch() {
        mock.unwatch()
    }

    //endregion

    //region pipeline

    @Override
    boolean isPipelined() {
        return pipelined
    }

    @Override
    void openPipeline() {
        if (isPipelined()) {
            return
        }
        if (isQueueing()) {
            throw new RuntimeException('ERR PIPELINE inside MULTI is not allowed')
        }
        pipelineResults = []
        pipelined = true
    }

    @Override
    List<Object> closePipeline() throws RedisPipelineException {
        pipelined = false
        if (isQueueing()) {
            exec()
        }
        def ret = pipelineResults
        pipelineResults = null
        return ret
    }

    private void pipeline(Object value, Closure converter) {
        def result
        if (converter == null) {
            result = value
        } else {
            result = converter.call(value)
        }
        pipelineResults << result
    }

    private void pipeline(Object value) {
        pipelineResults << value
    }

    //endregion

    @Override
    Object execute(String command, byte[] ... args) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    //region HyperLogLogCommands

    @Override
    Long pfAdd(byte[] key, byte[] ... values) {
        throw new UnsupportedOperationException()
    }

    @Override
    Long pfCount(byte[] ... keys) {
        throw new UnsupportedOperationException()
    }

    @Override
    void pfMerge(byte[] destinationKey, byte[] ... sourceKeys) {
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisKeyCommands

    @Override
    Boolean exists(byte[] key) {
        if (isQueueing()) {
            client.exists(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.exists(unserialize(key)))
            return null
        }
        return client.exists(unserialize(key))
    }

    @Override
    Long del(byte[] ... keys) {
        if (isQueueing()) {
            client.del(unserialize(keys))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.del(unserialize(keys)))
            return null
        }
        return client.del(unserialize(keys))
    }

    @Override
    DataType type(byte[] key) {
        def converter = { String type ->
            return DataType.fromCode(type)
        }
        if (isQueueing()) {
            client.type(unserialize(key))
            multiResultConverts << converter
            return null
        }
        if (isPipelined()) {
            pipeline(client.type(unserialize(key)), converter)
            return null
        }
        return converter.call(client.type(unserialize(key)))
    }

    @Override
    Set<byte[]> keys(byte[] pattern) {
        def converter = { String[] keys ->
            def set = [] as Set
            keys.each { key ->
                set << serialize(key)
            }
            return set
        }
        if (isQueueing()) {
            client.keys(unserialize(pattern))
            multiResultConverts << converter
            return null
        }
        if (isPipelined()) {
            pipeline(client.keys(unserialize(pattern)), converter)
            return null
        }
        return converter.call(client.keys(unserialize(pattern)))
    }

    @Override
    Cursor<byte[]> scan(ScanOptions options) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    byte[] randomKey() {
        if (isQueueing()) {
            client.randomkey()
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.randomkey(), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.randomkey())
    }

    @Override
    void rename(byte[] oldName, byte[] newName) {
        if (isQueueing()) {
            client.rename(unserialize(oldName), unserialize(newName))
            multiResultConverts << DO_NOTHING
            return
        }
        if (isPipelined()) {
            pipeline(client.rename(unserialize(oldName), unserialize(newName)))
            return
        }
        client.rename(unserialize(oldName), unserialize(newName))
    }

    @Override
    Boolean renameNX(byte[] oldName, byte[] newName) {
        if (isQueueing()) {
            client.renamenx(unserialize(oldName), unserialize(newName))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.renamenx(unserialize(oldName), unserialize(newName)))
            return null
        }
        return client.rename(unserialize(oldName), unserialize(newName))
    }

    @Override
    Boolean expire(byte[] key, long seconds) {
        if (isQueueing()) {
            client.expire(unserialize(key), (int) seconds)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.expire(unserialize(key), (int) seconds))
        }
        return client.expire(unserialize(key), (int) seconds)
    }

    @Override
    Boolean pExpire(byte[] key, long millis) {
        if (isQueueing()) {
            client.pexpire(unserialize(key), millis)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.pexpire(unserialize(key), millis))
            return null
        }
        return client.pexpire(unserialize(key), millis)
    }

    @Override
    Boolean expireAt(byte[] key, long unixTime) {
        if (isQueueing()) {
            client.expireat(unserialize(key), unixTime)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.expireat(unserialize(key), unixTime))
            return null
        }
        return client.expireat(unserialize(key), unixTime)
    }

    @Override
    Boolean pExpireAt(byte[] key, long unixTimeInMillis) {
        if (isQueueing()) {
            client.pexpireat(unserialize(key), unixTimeInMillis)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.pexpireat(unserialize(key), unixTimeInMillis))
            return null
        }
        return client.pexpireat(unserialize(key), unixTimeInMillis)
    }

    @Override
    Boolean persist(byte[] key) {
        if (isQueueing()) {
            client.persist(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.persist(unserialize(key)))
            return null
        }
        return client.persist(unserialize(key))
    }

    @Override
    Boolean move(byte[] key, int dbIndex) {
        def converter = { Long value ->
            if (value) {
                return true
            } else {
                return false
            }
        }
        if (isQueueing()) {
            client.move(unserialize(key), dbIndex)
            multiResultConverts << converter
            return null
        }
        if (isPipelined()) {
            pipeline(client.move(unserialize(key), dbIndex), converter)
            return null
        }
        return client.move(unserialize(key), dbIndex)
    }

    @Override
    Long ttl(byte[] key) {
        if (isQueueing()) {
            client.ttl(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.ttl(unserialize(key)))
            return null
        }
        return client.ttl(unserialize(key))
    }

    @Override
    Long pTtl(byte[] key) {
        if (isQueueing()) {
            client.pttl(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.pttl(unserialize(key)))
            return null
        }
        return client.pttl(unserialize(key))
    }

    @Override
    List<byte[]> sort(byte[] key, SortParameters params) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long sort(byte[] key, SortParameters params, byte[] storeKey) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    byte[] dump(byte[] key) {
        if (isQueueing()) {
            client.dump(unserialize(key))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.dump(unserialize(key)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.dump(unserialize(key)))
    }

    @Override
    void restore(byte[] key, long ttlInMillis, byte[] serializedValue) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisConnectionCommands

    @Override
    byte[] echo(byte[] message) {
        //TODO unimplemented: because IRedisClient does not implement echo() method
        throw new UnsupportedOperationException()
//        return message
    }

    @Override
    String ping() {
        //TODO unimplemented: because IRedisClient does not implement ping() method
        throw new UnsupportedOperationException()
//        return 'PONG'
    }

    @Override
    void select(int dbIndex) {
        if (dbIndex != 0) {
            //RedisMock does not support multi database
            throw new UnsupportedOperationException()
        }
    }

    //endregion

    //region RedisHashCommands

    @Override
    Boolean hSet(byte[] key, byte[] field, byte[] value) {
        if (isQueueing()) {
            client.hset(unserialize(key), unserialize(field), unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.hset(unserialize(key), unserialize(field), unserialize(value)))
            return null
        }
        return client.hset(unserialize(key), unserialize(field), unserialize(value))
    }

    @Override
    Boolean hSetNX(byte[] key, byte[] field, byte[] value) {
        if (isQueueing()) {
            client.hsetnx(unserialize(key), unserialize(field), unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.hsetnx(unserialize(key), unserialize(field), unserialize(value)))
            return null
        }
        return client.hsetnx(unserialize(key), unserialize(field), unserialize(value))
    }

    @Override
    byte[] hGet(byte[] key, byte[] field) {
        if (isQueueing()) {
            client.hget(unserialize(key), unserialize(field))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.hget(unserialize(key), unserialize(field)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.hget(unserialize(key), unserialize(field)))
    }

    @Override
    List<byte[]> hMGet(byte[] key, byte[] ... fields) {
        def param = parseParameter(fields)
        if (isQueueing()) {
            client.hmget(unserialize(key), param.param, param.params)
            multiResultConverts << STRINGLIST_TO_BYTESLIST
            return null
        }
        if (isPipelined()) {
            pipeline(client.hmget(unserialize(key), param.param, param.params), STRINGLIST_TO_BYTESLIST)
            return null
        }
        return STRINGLIST_TO_BYTESLIST.call(client.hmget(unserialize(key), param.param, param.params))
    }

    @Override
    void hMSet(byte[] key, Map<byte[], byte[]> hashes) {
        def field = null
        def value = null
        def fieldsValues = []
        hashes.each { k, v ->
            if (field && value) {
                fieldsValues << unserialize(k)
                fieldsValues << unserialize(v)
            } else {
                field = unserialize(k)
                value = unserialize(v)
            }
        }
        if (isQueueing()) {
            client.hmset(unserialize(key), field, value, fieldsValues as String[])
            multiResultConverts << NULL
            return
        }
        if (isPipelined()) {
            pipeline(client.hmset(unserialize(key), field, value, fieldsValues as String[]), NULL)
            return
        }
        client.hmset(unserialize(key), field, value, fieldsValues as String[])
    }

    @Override
    Long hIncrBy(byte[] key, byte[] field, long delta) {
        if (isQueueing()) {
            client.hincrby(unserialize(key), unserialize(field), delta)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.hincrby(unserialize(key), unserialize(field), delta))
            return null
        }
        return client.hincrby(unserialize(key), unserialize(field), delta)
    }

    @Override
    Double hIncrBy(byte[] key, byte[] field, double delta) {
        if (isQueueing()) {
            client.hincrbyfloat(unserialize(key), unserialize(field), delta)
            multiResultConverts << STRING_TO_DOUBLE
            return null
        }
        if (isPipelined()) {
            pipeline(client.hincrbyfloat(unserialize(key), unserialize(field), delta), STRING_TO_DOUBLE)
            return null
        }
        return STRING_TO_DOUBLE.call(client.hincrbyfloat(unserialize(key), unserialize(field), delta))
    }

    @Override
    Boolean hExists(byte[] key, byte[] field) {
        if (isQueueing()) {
            client.hexists(unserialize(key), unserialize(field))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.hexists(unserialize(key), unserialize(field)))
            return null
        }
        return client.hexists(unserialize(key), unserialize(field))
    }

    @Override
    Long hDel(byte[] key, byte[] ... fields) {
        def param = parseParameter(fields)
        if (isQueueing()) {
            client.hdel(unserialize(key), param.param, param.params)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.hdel(unserialize(key), param.param, param.params))
            return null
        }
        return client.hdel(unserialize(key), param.param, param.params)
    }

    @Override
    Long hLen(byte[] key) {
        if (isQueueing()) {
            client.hlen(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.hlen(unserialize(key)))
            return null
        }
        return client.hlen(unserialize(key))
    }

    @Override
    Set<byte[]> hKeys(byte[] key) {
        if (isQueueing()) {
            client.hkeys(unserialize(key))
            multiResultConverts << STRINGSET_TO_BYTESSET
            return null
        }
        if (isPipelined()) {
            pipeline(client.hkeys(unserialize(key)), STRINGSET_TO_BYTESSET)
            return null
        }
        return STRINGSET_TO_BYTESSET.call(client.hkeys(unserialize(key)))
    }

    @Override
    List<byte[]> hVals(byte[] key) {
        if (isQueueing()) {
            client.hvals(unserialize(key))
            multiResultConverts << STRINGLIST_TO_BYTESLIST
            return null
        }
        if (isPipelined()) {
            pipeline(client.hvals(unserialize(key)), STRINGLIST_TO_BYTESLIST)
            return null
        }
        return STRINGLIST_TO_BYTESLIST.call(client.hvals(unserialize(key)))
    }

    @Override
    Map<byte[], byte[]> hGetAll(byte[] key) {
        if (isQueueing()) {
            client.hgetall(unserialize(key))
            multiResultConverts << STRINGMAP_TO_BYTESMAP
            return null
        }
        if (isPipelined()) {
            pipeline(client.hgetall(unserialize(key)), STRINGMAP_TO_BYTESMAP)
            return null
        }
        return STRINGMAP_TO_BYTESMAP.call(client.hgetall(unserialize(key)))
    }

    @Override
    Cursor<Map.Entry<byte[], byte[]>> hScan(byte[] key, ScanOptions options) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisListCommands

    @Override
    Long rPush(byte[] key, byte[] ... values) {
        def param = parseParameter(values)
        if (isQueueing()) {
            client.rpush(unserialize(key), param.param, param.params)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.rpush(unserialize(key), param.param, param.params))
            return null
        }
        return client.rpush(unserialize(key), param.param, param.params)
    }

    @Override
    Long lPush(byte[] key, byte[] ... values) {
        def param = parseParameter(values)
        if (isQueueing()) {
            client.lpush(unserialize(key), param.param, param.params)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.lpush(unserialize(key), param.param, param.params))
            return null
        }
        return client.lpush(unserialize(key), param.param, param.params)
    }

    @Override
    Long rPushX(byte[] key, byte[] value) {
        if (isQueueing()) {
            client.rpushx(unserialize(key), unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.rpushx(unserialize(key), unserialize(value)))
            return null
        }
        return client.rpushx(unserialize(key), unserialize(value))
    }

    @Override
    Long lPushX(byte[] key, byte[] value) {
        if (isQueueing()) {
            client.lpushx(unserialize(key), unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.lpushx(unserialize(key), unserialize(value)))
            return null
        }
        return client.lpushx(unserialize(key), unserialize(value))
    }

    @Override
    Long lLen(byte[] key) {
        if (isQueueing()) {
            client.llen(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.llen(unserialize(key)))
            return null
        }
        return client.llen(unserialize(key))
    }

    @Override
    List<byte[]> lRange(byte[] key, long begin, long end) {
        if (isQueueing()) {
            client.lrange(unserialize(key), begin, end)
            multiResultConverts << STRINGLIST_TO_BYTESLIST
            return null
        }
        if (isPipelined()) {
            pipeline(client.lrange(unserialize(key), begin, end), STRINGLIST_TO_BYTESLIST)
            return null
        }
        return STRINGLIST_TO_BYTESLIST.call(client.lrange(unserialize(key), begin, end))
    }

    @Override
    void lTrim(byte[] key, long begin, long end) {
        if (isQueueing()) {
            client.ltrim(unserialize(key), begin, end)
            multiResultConverts << NULL
            return
        }
        if (isPipelined()) {
            pipeline(client.ltrim(unserialize(key), begin, end), NULL)
        }
        client.ltrim(unserialize(key), begin, end)
    }

    @Override
    byte[] lIndex(byte[] key, long index) {
        if (isQueueing()) {
            client.lindex(unserialize(key), index)
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.lindex(unserialize(key), index), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.lindex(unserialize(key), index))
    }

    @Override
    Long lInsert(byte[] key, Position where, byte[] pivot, byte[] value) {
        //TODO
        throw new UnsupportedOperationException()
    }

    @Override
    void lSet(byte[] key, long index, byte[] value) {
        if (isQueueing()) {
            client.lset(unserialize(key), index, unserialize(value))
            multiResultConverts << NULL
            return
        }
        if (isPipelined()) {
            pipeline(client.lset(unserialize(key), index, unserialize(value)), NULL)
            return
        }
        client.lset(unserialize(key), index, unserialize(value))
    }

    @Override
    Long lRem(byte[] key, long count, byte[] value) {
        if (isQueueing()) {
            client.lrem(unserialize(key), count, unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.lrem(unserialize(key), count, unserialize(value)))
            return null
        }
        return client.lrem(unserialize(key), count, unserialize(value))
    }

    @Override
    byte[] lPop(byte[] key) {
        if (isQueueing()) {
            client.lpop(unserialize(key))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.lpop(unserialize(key)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.lpop(unserialize(key)))
    }

    @Override
    byte[] rPop(byte[] key) {
        if (isQueueing()) {
            client.rpop(unserialize(key))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.rpop(unserialize(key)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.rpop(unserialize(key)))
    }

    @Override
    List<byte[]> bLPop(int timeout, byte[] ... keys) {
        //TODO RedisMock does not support blpop()
        throw new UnsupportedOperationException()
    }

    @Override
    List<byte[]> bRPop(int timeout, byte[] ... keys) {
        //TODO RedisMock does not support brpop()
        throw new UnsupportedOperationException()
    }

    @Override
    byte[] rPopLPush(byte[] srcKey, byte[] dstKey) {
        if (isQueueing()) {
            client.rpoplpush(unserialize(srcKey), unserialize(dstKey))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.rpoplpush(unserialize(srcKey), unserialize(dstKey)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.rpoplpush(unserialize(srcKey), unserialize(dstKey)))
    }

    @Override
    byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey) {
        //TODO RedisMock does not support brpoplpush()
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisSetCommands

    @Override
    Long sAdd(byte[] key, byte[] ... values) {
        def param = parseParameter(values)
        if (isQueueing()) {
            client.sadd(unserialize(key), param.param, param.params)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.sadd(unserialize(key), param.param, param.params))
            return null
        }
        return client.sadd(unserialize(key), param.param, param.params)
    }

    @Override
    Long sRem(byte[] key, byte[] ... values) {
        def param = parseParameter(values)
        if (isQueueing()) {
            client.srem(unserialize(key), param.param, param.params)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.srem(unserialize(key), param.param, param.params))
            return null
        }
        return client.srem(unserialize(key), param.param, param.params)
    }

    @Override
    byte[] sPop(byte[] key) {
        if (isQueueing()) {
            client.spop(unserialize(key))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.spop(unserialize(key)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.spop(unserialize(key)))
    }

    @Override
    Boolean sMove(byte[] srcKey, byte[] destKey, byte[] value) {
        if (isQueueing()) {
            client.smove(unserialize(srcKey), unserialize(destKey), unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.smove(unserialize(srcKey), unserialize(destKey), unserialize(value)))
            return null
        }
        return client.smove(unserialize(srcKey), unserialize(destKey), unserialize(value))
    }

    @Override
    Long sCard(byte[] key) {
        if (isQueueing()) {
            client.scard(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.scard(unserialize(key)))
            return null
        }
        return client.scard(unserialize(key))
    }

    @Override
    Boolean sIsMember(byte[] key, byte[] value) {
        return doing(DO_NOTHING) {
            return client.sismember(unserialize(key), unserialize(value))
        }
    }

    @Override
    Set<byte[]> sInter(byte[] ... keys) {
        return doing(STRINGSET_TO_BYTESSET) {
            def param = parseParameter(keys)
            return client.sinter(param.param, param.params)
        }
    }

    @Override
    Long sInterStore(byte[] destKey, byte[] ... keys) {
        return doing() {
            def param = parseParameter(keys)
            def dest = unserialize(destKey)
            return client.sinterstore(dest, param.param, param.params)
        }
    }

    @Override
    Set<byte[]> sUnion(byte[] ... keys) {
        return doing(STRINGSET_TO_BYTESSET) {
            def param = parseParameter(keys)
            return client.sunion(param.param, param.params)
        }
    }

    @Override
    Long sUnionStore(byte[] destKey, byte[] ... keys) {
        return doing() {
            def param = parseParameter(keys)
            def dest = unserialize(destKey)
            return client.sunionstore(dest, param.param, param.params)
        }
    }

    @Override
    Set<byte[]> sDiff(byte[] ... keys) {
        return doing(STRINGSET_TO_BYTESSET) {
            def param = parseParameter(keys)
            return client.sdiff(param.param, param.params)
        }
    }

    @Override
    Long sDiffStore(byte[] destKey, byte[] ... keys) {
        return doing() {
            def param = parseParameter(keys)
            def dest = unserialize(destKey)
            return client.sdiffstore(dest, param.param, param.params)
        }
    }

    @Override
    Set<byte[]> sMembers(byte[] key) {
        return doing(STRINGSET_TO_BYTESSET) {
            return client.smembers(unserialize(key))
        }
    }

    @Override
    byte[] sRandMember(byte[] key) {
        return doing(STRING_TO_BYTES) {
            return client.srandmember(unserialize(key))
        }
    }

    @Override
    List<byte[]> sRandMember(byte[] key, long count) {
        return doing(STRINGLIST_TO_BYTESLIST) {
            return client.srandmember(unserialize(key), count)
        }
    }

    @Override
    Cursor<byte[]> sScan(byte[] key, ScanOptions options) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisStringCommands

    @Override
    byte[] get(byte[] key) {
        if (isQueueing()) {
            client.get(unserialize(key))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.get(unserialize(key)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.get(unserialize(key)))
    }

    @Override
    byte[] getSet(byte[] key, byte[] value) {
        if (isQueueing()) {
            client.getset(unserialize(key), unserialize(value))
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.getset(unserialize(key), unserialize(value)), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.getset(unserialize(key), unserialize(value)))
    }

    @Override
    List<byte[]> mGet(byte[] ... keys) {
        if (isQueueing()) {
            client.mget(unserialize(keys))
            multiResultConverts << STRINGS_TO_BYTESLIST
            return null
        }
        if (isPipelined()) {
            pipeline(client.mget(unserialize(keys)), STRINGS_TO_BYTESLIST)
            return null
        }
        return STRINGS_TO_BYTESLIST.call(client.mget(unserialize(keys)))
    }

    @Override
    Boolean set(byte[] key, byte[] value) {
        if (isQueueing()) {
            client.set(unserialize(key), unserialize(value))
            multiResultConverts << NULL
            return null
        }
        if (isPipelined()) {
            pipeline(client.set(unserialize(key), unserialize(value)), NULL)
            return null
        }
        return Boolean.valueOf(client.set(unserialize(key), unserialize(value)))
    }

    @Override
    Boolean set(byte[] key, byte[] value, Expiration expiration, SetOption option) {
        def options = []
        if (expiration) {
            options << 'px'
            options << String.valueOf(expiration.expirationTimeInMilliseconds)
        }
        if (option) {
            switch (option) {
                case SetOption.UPSERT:
                    //do nothing
                    break
                case SetOption.SET_IF_ABSENT:
                    options << 'nx'
                    break
                case SetOption.SET_IF_PRESENT:
                    options << 'xx'
                    break
                default:
                    throw new RuntimeException("invalid option: $option")
            }
        }
        if (isQueueing()) {
            client.set(unserialize(key), unserialize(value), options as String[])
            multiResultConverts << NULL
            return null
        }
        if (isPipelined()) {
            pipeline(client.set(unserialize(key), unserialize(value), options as String[]), NULL)
            return null
        }
        return Boolean.valueOf(client.set(unserialize(key), unserialize(value), options as String[]))
    }

    @Override
    Boolean setNX(byte[] key, byte[] value) {
        if (isQueueing()) {
            client.setnx(unserialize(key), unserialize(value))
            multiResultConverts << LONG_TO_BOOLEAN
            return null
        }
        if (isPipelined()) {
            pipeline(client.setnx(unserialize(key), unserialize(value)), LONG_TO_BOOLEAN)
            return null
        }
        return LONG_TO_BOOLEAN.call(client.setnx(unserialize(key), unserialize(value)))
    }

    @Override
    Boolean setEx(byte[] key, long seconds, byte[] value) {
        if (isQueueing()) {
            client.setex(unserialize(key), (int) seconds, unserialize(value))
            multiResultConverts << NULL
            return null
        }
        if (isPipelined()) {
            pipeline(client.setex(unserialize(key), (int) seconds, unserialize(value)), NULL)
            return null
        }
        return Boolean.valueOf(client.setex(unserialize(key), (int) seconds, unserialize(value)))
    }

    @Override
    Boolean pSetEx(byte[] key, long milliseconds, byte[] value) {
        if (isQueueing()) {
            client.psetex(unserialize(key), milliseconds, unserialize(value))
            multiResultConverts << NULL
            return null
        }
        if (isPipelined()) {
            pipeline(client.psetex(unserialize(key), milliseconds, unserialize(value)), NULL)
            return null
        }
        return Boolean.valueOf(client.psetex(unserialize(key), milliseconds, unserialize(value)))
    }

    @Override
    Boolean mSet(Map<byte[], byte[]> tuple) {
        def keysValues = []
        tuple.each { k, v ->
            keysValues << unserialize(k)
            keysValues << unserialize(v)
        }
        String[] keysValuesArray = keysValues.toArray(new String[0])
        if (isQueueing()) {
            client.mset(keysValuesArray)
            multiResultConverts << NULL
            return null
        }
        if (isPipelined()) {
            pipeline(client.mset(keysValuesArray), NULL)
            return null
        }
        return Boolean.valueOf(client.mset(keysValuesArray))
    }

    @Override
    Boolean mSetNX(Map<byte[], byte[]> tuple) {
        def keysValues = []
        tuple.each { k, v ->
            keysValues << unserialize(k)
            keysValues << unserialize(v)
        }
        String[] keysValuesArray = keysValues.toArray(new String[0])
        if (isQueueing()) {
            client.msetnx(keysValuesArray)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.msetnx(keysValuesArray))
            return null
        }
        return client.msetnx(keysValuesArray)
    }

    @Override
    Long incr(byte[] key) {
        if (isQueueing()) {
            client.incr(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.incr(unserialize(key)))
            return null
        }
        return client.incr(unserialize(key))
    }

    @Override
    Long incrBy(byte[] key, long value) {
        if (isQueueing()) {
            client.incrby(unserialize(key), value)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.incrby(unserialize(key), value))
            return null
        }
        return client.incrby(unserialize(key), value)
    }

    @Override
    Double incrBy(byte[] key, double value) {
        def converter = { String string ->
            return Double.parseDouble(string)
        }
        if (isQueueing()) {
            client.incrbyfloat(unserialize(key), value)
            multiResultConverts << converter
            return null
        }
        if (isPipelined()) {
            pipeline(client.incrbyfloat(unserialize(key), value), converter)
            return null
        }
        return converter.call(client.incrbyfloat(unserialize(key), value))
    }

    @Override
    Long decr(byte[] key) {
        if (isQueueing()) {
            client.decr(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.decr(unserialize(key)))
            return null
        }
        return client.decr(unserialize(key))
    }

    @Override
    Long decrBy(byte[] key, long value) {
        if (isQueueing()) {
            client.decrby(unserialize(key), value)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.decrby(unserialize(key), value))
            return null
        }
        return client.decrby(unserialize(key), value)
    }

    @Override
    Long append(byte[] key, byte[] value) {
        if (isQueueing()) {
            client.append(unserialize(key), unserialize(value))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.append(unserialize(key), unserialize(value)))
            return null
        }
        return client.append(unserialize(key), unserialize(value))
    }

    @Override
    byte[] getRange(byte[] key, long begin, long end) {
        if (isQueueing()) {
            client.getrange(unserialize(key), begin, end)
            multiResultConverts << STRING_TO_BYTES
            return null
        }
        if (isPipelined()) {
            pipeline(client.getrange(unserialize(key), begin, end), STRING_TO_BYTES)
            return null
        }
        return STRING_TO_BYTES.call(client.getrange(unserialize(key), begin, end))
    }

    @Override
    void setRange(byte[] key, byte[] value, long offset) {
        if (isQueueing()) {
            client.setrange(unserialize(key), offset, unserialize(value))
            multiResultConverts << NULL
            return
        }
        if (isPipelined()) {
            pipeline(client.setrange(unserialize(key), offset, unserialize(value)), NULL)
            return
        }
        client.setrange(unserialize(key), offset, unserialize(value))
    }

    @Override
    Boolean getBit(byte[] key, long offset) {
        if (isQueueing()) {
            client.getbit(unserialize(key), offset)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.getbit(unserialize(key), offset))
            return null
        }
        return client.getbit(unserialize(key), offset)
    }

    @Override
    Boolean setBit(byte[] key, long offset, boolean value) {
        if (isQueueing()) {
            client.setbit(unserialize(key), offset, value)
            multiResultConverts << LONG_TO_BOOLEAN
            return null
        }
        if (isPipelined()) {
            pipeline(client.setbit(unserialize(key), offset, value), LONG_TO_BOOLEAN)
            return null
        }
        return LONG_TO_BOOLEAN.call(client.setbit(unserialize(key), offset, value))
    }

    @Override
    Long bitCount(byte[] key) {
        if (isQueueing()) {
            client.bitcount(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.bitcount(unserialize(key)))
            return null
        }
        return client.bitcount(unserialize(key))
    }

    @Override
    Long bitCount(byte[] key, long begin, long end) {
        if (isQueueing()) {
            client.bitcount(unserialize(key), begin, end)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.bitcount(unserialize(key), begin, end))
            return null
        }
        return client.bitcount(unserialize(key), begin, end)
    }

    @Override
    Long bitOp(BitOperation op, byte[] destination, byte[] ... keys) {
        String operation = op.toString().toLowerCase()
        String destKey = unserialize(destination)
        String opKeys = unserialize(keys)
        if (isQueueing()) {
            client.bitop(operation, destKey, opKeys)
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.bitop(operation, destKey, opKeys))
            return null
        }
        return client.bitop(operation, destKey, opKeys)
    }

    @Override
    Long strLen(byte[] key) {
        if (isQueueing()) {
            client.strlen(unserialize(key))
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(client.strlen(unserialize(key)))
            return null
        }
        return client.strlen(unserialize(key))
    }

    //endregion

    //region RedisZSetCommands

    @Override
    Boolean zAdd(byte[] key, double score, byte[] value) {
        return doing(LONG_TO_BOOLEAN) {
            def scoreMember = new IRedisSortedSet.ZsetPair(unserialize(value), score)
            return client.zadd(unserialize(key), scoreMember)
        }
    }

    @Override
    Long zAdd(byte[] key, Set<Tuple> tuples) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zRem(byte[] key, byte[] ... values) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Double zIncrBy(byte[] key, double increment, byte[] value) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zRank(byte[] key, byte[] value) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zRevRank(byte[] key, byte[] value) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRange(byte[] key, long begin, long end) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRangeWithScores(byte[] key, long begin, long end) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, double min, double max) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRangeByScoreWithScores(byte[] key, double min, double max) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, double min, double max, long offset, long count) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRevRange(byte[] key, long begin, long end) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRevRangeWithScores(byte[] key, long begin, long end) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, double min, double max) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, double min, double max, long offset, long count) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, Range range, Limit limit) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<Tuple> zRevRangeByScoreWithScores(byte[] key, Range range, Limit limit) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zCount(byte[] key, double min, double max) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zCount(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zCard(byte[] key) {
        return doing() {
            return client.zcard(unserialize(key))
        }
    }

    @Override
    Double zScore(byte[] key, byte[] value) {
        return doing() {
            return client.zscore(unserialize(key), unserialize(value))
        }
    }

    @Override
    Long zRemRange(byte[] key, long begin, long end) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zRemRangeByScore(byte[] key, double min, double max) {
        return doing() {
            return client.zremrangebyscore(unserialize(key), String.valueOf(min), String.valueOf(max))
        }
    }

    @Override
    Long zRemRangeByScore(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zUnionStore(byte[] destKey, byte[] ... sets) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zUnionStore(byte[] destKey, Aggregate aggregate, int[] weights, byte[] ... sets) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zInterStore(byte[] destKey, byte[] ... sets) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Long zInterStore(byte[] destKey, Aggregate aggregate, int[] weights, byte[] ... sets) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Cursor<Tuple> zScan(byte[] key, ScanOptions options) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, String min, String max) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, Range range, Limit limit) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByLex(byte[] key) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByLex(byte[] key, Range range) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    @Override
    Set<byte[]> zRangeByLex(byte[] key, Range range, Limit limit) {
        //TODO unimplemented
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisPubSubCommands

    @Override
    boolean isSubscribed() {
        throw new UnsupportedOperationException()
    }

    @Override
    Subscription getSubscription() {
        throw new UnsupportedOperationException()
    }

    @Override
    Long publish(byte[] channel, byte[] message) {
        throw new UnsupportedOperationException()
    }

    @Override
    void subscribe(MessageListener listener, byte[] ... channels) {
        throw new UnsupportedOperationException()
    }

    @Override
    void pSubscribe(MessageListener listener, byte[] ... patterns) {
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisScriptingCommands

    @Override
    void scriptFlush() {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    @Override
    void scriptKill() {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    @Override
    String scriptLoad(byte[] script) {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    @Override
    List<Boolean> scriptExists(String... scriptShas) {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    @Override
    <T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[] ... keysAndArgs) {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    @Override
    <T> T evalSha(String scriptSha, ReturnType returnType, int numKeys, byte[] ... keysAndArgs) {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    @Override
    <T> T evalSha(byte[] scriptSha, ReturnType returnType, int numKeys, byte[] ... keysAndArgs) {
        //TODO RedisMock support lua script
        throw new UnsupportedOperationException()
    }

    //endregion

    //region RedisServerCommands

    @Override
    void bgWriteAof() {
        throw new UnsupportedOperationException()
    }

    @Override
    void bgReWriteAof() {
        throw new UnsupportedOperationException()
    }

    @Override
    void bgSave() {
        throw new UnsupportedOperationException()
    }

    @Override
    Long lastSave() {
        throw new UnsupportedOperationException()
    }

    @Override
    void save() {
        throw new UnsupportedOperationException()
    }

    @Override
    Long dbSize() {
        throw new UnsupportedOperationException()
    }

    @Override
    void flushDb() {
        throw new UnsupportedOperationException()
    }

    @Override
    void flushAll() {
        throw new UnsupportedOperationException()
    }

    @Override
    Properties info() {
        throw new UnsupportedOperationException()
    }

    @Override
    Properties info(String section) {
        throw new UnsupportedOperationException()
    }

    @Override
    void shutdown() {
        throw new UnsupportedOperationException()
    }

    @Override
    void shutdown(ShutdownOption option) {
        throw new UnsupportedOperationException()
    }

    @Override
    Properties getConfig(String pattern) {
        throw new UnsupportedOperationException()
    }

    @Override
    void setConfig(String param, String value) {
        throw new UnsupportedOperationException()
    }

    @Override
    void resetConfigStats() {
        throw new UnsupportedOperationException()
    }

    @Override
    Long time() {
        throw new UnsupportedOperationException()
    }

    @Override
    void killClient(String host, int port) {
        throw new UnsupportedOperationException()
    }

    @Override
    void setClientName(byte[] name) {
        throw new UnsupportedOperationException()
    }

    @Override
    String getClientName() {
        throw new UnsupportedOperationException()
    }

    @Override
    List<RedisClientInfo> getClientList() {
        throw new UnsupportedOperationException()
    }

    @Override
    void slaveOf(String host, int port) {
        throw new UnsupportedOperationException()
    }

    @Override
    void slaveOfNoOne() {
        throw new UnsupportedOperationException()
    }

    @Override
    void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option) {
        throw new UnsupportedOperationException()
    }

    @Override
    void migrate(byte[] key, RedisNode target, int dbIndex, MigrateOption option, long timeout) {
        throw new UnsupportedOperationException()
    }

    //endregion

    private IRedisClient getClient() {
        return isQueueing() ? multiMock : mock
    }

    private <T, S> T doing(Closure<T> converter, Closure<S> work) {
        if (isQueueing()) {
            work.call()
            multiResultConverts << converter
            return null
        }
        if (isPipelined()) {
            pipeline(work.call(), converter)
            return null
        }
        return converter.call(work.call())
    }

    private <T> T doing(Closure<T> work) {
        if (isQueueing()) {
            work.call()
            multiResultConverts << DO_NOTHING
            return null
        }
        if (isPipelined()) {
            pipeline(work.call())
            return null
        }
        return work.call()
    }

}
