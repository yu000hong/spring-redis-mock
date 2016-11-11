package com.yu000hong.spring.redis.mock

import org.rarefiedredis.redis.NotImplementedException
import org.rarefiedredis.redis.RedisMock
import org.springframework.data.redis.connection.AbstractRedisConnection
import org.springframework.data.redis.connection.DataType
import org.springframework.data.redis.connection.MessageListener
import org.springframework.data.redis.connection.Pool
import org.springframework.data.redis.connection.RedisListCommands
import org.springframework.data.redis.connection.RedisNode
import org.springframework.data.redis.connection.RedisPipelineException
import org.springframework.data.redis.connection.RedisServerCommands
import org.springframework.data.redis.connection.RedisStringCommands
import org.springframework.data.redis.connection.RedisZSetCommands
import org.springframework.data.redis.connection.ReturnType
import org.springframework.data.redis.connection.SortParameters
import org.springframework.data.redis.connection.Subscription
import org.springframework.data.redis.core.Cursor
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.core.types.Expiration
import org.springframework.data.redis.core.types.RedisClientInfo
import org.springframework.util.Assert

import static com.yu000hong.spring.redis.mock.RedisMockUtil.serialize
import static com.yu000hong.spring.redis.mock.RedisMockUtil.unserialize

class RedisMockConnection extends AbstractRedisConnection {
    private final RedisMock mock
    private final Pool<RedisMock> pool

    public RedisMockConnection(RedisMock mock) {
        this(mock, null)
    }

    public RedisMockConnection(RedisMock mock, Pool<RedisMock> pool) {
        Assert.notNull(mock, 'a not-null instance required')
        this.mock = mock
        this.pool = pool
    }

    @Override
    boolean isClosed() {
        return false
    }

    @Override
    RedisMock getNativeConnection() {
        return mock
    }

    @Override
    boolean isQueueing() {
        return false
    }

    @Override
    boolean isPipelined() {
        return false
    }

    @Override
    void openPipeline() {
        throw new UnsupportedOperationException('Pipelining not supported by redis-mock-java')
    }

    @Override
    List<Object> closePipeline() throws RedisPipelineException {
        return []
    }

    @Override
    Object execute(String command, byte[] ... args) {
        return null
    }

    @Override
    Long pfAdd(byte[] key, byte[] ... values) {
        return null
    }

    @Override
    Long pfCount(byte[] ... keys) {
        return null
    }

    @Override
    void pfMerge(byte[] destinationKey, byte[] ... sourceKeys) {

    }

    @Override
    void select(int dbIndex) {

    }

    @Override
    byte[] echo(byte[] message) {
        return message
    }

    @Override
    String ping() {
        return 'PONG'
    }

    @Override
    Boolean hSet(byte[] key, byte[] field, byte[] value) {
        return mock.hset(unserialize(key), unserialize(field), unserialize(value))
    }

    @Override
    Boolean hSetNX(byte[] key, byte[] field, byte[] value) {
        return mock.hsetnx(unserialize(key), unserialize(field), unserialize(value))
    }

    @Override
    byte[] hGet(byte[] key, byte[] field) {
        return mock.hget(unserialize(key), unserialize(field))
    }

    @Override
    List<byte[]> hMGet(byte[] key, byte[] ... fields) {
//        def list = mock.hmget(unserialize(key),))
//        TODO
        throw new NotImplementedException()
    }

    @Override
    void hMSet(byte[] key, Map<byte[], byte[]> hashes) {
        //TODO
        throw new NotImplementedException()
    }

    @Override
    Long hIncrBy(byte[] key, byte[] field, long delta) {
        return mock.hincrby(unserialize(key), unserialize(field), delta)
    }

    @Override
    Double hIncrBy(byte[] key, byte[] field, double delta) {
        return Double.parseDouble(mock.hincrbyfloat(unserialize(key), unserialize(field), delta))
    }

    @Override
    Boolean hExists(byte[] key, byte[] field) {
        return mock.hexists(unserialize(key), unserialize(field))
    }

    @Override
    Long hDel(byte[] key, byte[] ... fields) {
//        return mock.hdel(unserialize(key), )
        //TODO
    }

    @Override
    Long hLen(byte[] key) {
        return mock.hlen(unserialize(key))
    }

    @Override
    Set<byte[]> hKeys(byte[] key) {
        def keys = mock.hkeys(unserialize(key))
        def set = [] as Set
        keys.each { k ->
            set << serialize(k)
        }
        return set
    }

    @Override
    List<byte[]> hVals(byte[] key) {
        return mock.hvals(unserialize(key)).collect { val ->
            return serialize(val)
        }
    }

    @Override
    Map<byte[], byte[]> hGetAll(byte[] key) {
        def map = [:]
        mock.hgetall(unserialize(key)).each { k, v ->
            map[serialize(k)] = serialize(v)
        }
        return map
    }

    @Override
    Cursor<Map.Entry<byte[], byte[]>> hScan(byte[] key, ScanOptions options) {
        return null
    }

    @Override
    Boolean exists(byte[] key) {
        return mock.exists(unserialize(key))
    }

    @Override
    Long del(byte[] ... keys) {
        return mock.del(unserialize(keys))
    }

    @Override
    DataType type(byte[] key) {
        def type = mock.type(unserialize(key))
        return null//TODO
    }

    @Override
    Set<byte[]> keys(byte[] pattern) {
        return null
    }

    @Override
    Cursor<byte[]> scan(ScanOptions options) {
        return null
    }

    @Override
    byte[] randomKey() {
        return serialize(mock.randomkey())
    }

    @Override
    void rename(byte[] oldName, byte[] newName) {
        mock.rename(unserialize(oldName), unserialize(newName))
    }

    @Override
    Boolean renameNX(byte[] oldName, byte[] newName) {
        return mock.rename(unserialize(oldName), unserialize(newName))
    }

    @Override
    Boolean expire(byte[] key, long seconds) {
        return mock.expire(unserialize(key), (int) seconds)
    }

    @Override
    Boolean pExpire(byte[] key, long millis) {
        return mock.pexpire(unserialize(key), millis)
    }

    @Override
    Boolean expireAt(byte[] key, long unixTime) {
        return mock.expireat(unserialize(key), unixTime)
    }

    @Override
    Boolean pExpireAt(byte[] key, long unixTimeInMillis) {
        //TODO what's the difference between pexpireat and expireat
        return mock.pexpireat(unserialize(key), unixTimeInMillis)
    }

    @Override
    Boolean persist(byte[] key) {
        return mock.persist(unserialize(key))
    }

    @Override
    Boolean move(byte[] key, int dbIndex) {
        return mock.move(unserialize(key), dbIndex)
    }

    @Override
    Long ttl(byte[] key) {
        return mock.ttl(unserialize(key))
    }

    @Override
    Long pTtl(byte[] key) {
        return mock.pttl(unserialize(key))
    }

    @Override
    List<byte[]> sort(byte[] key, SortParameters params) {
        return null
    }

    @Override
    Long sort(byte[] key, SortParameters params, byte[] storeKey) {
        return null
    }

    @Override
    byte[] dump(byte[] key) {
        return new byte[0]
    }

    @Override
    void restore(byte[] key, long ttlInMillis, byte[] serializedValue) {

    }

    @Override
    Long rPush(byte[] key, byte[] ... values) {
        return null
    }

    @Override
    Long lPush(byte[] key, byte[] ... values) {
        return null
    }

    @Override
    Long rPushX(byte[] key, byte[] value) {
        return null
    }

    @Override
    Long lPushX(byte[] key, byte[] value) {
        return null
    }

    @Override
    Long lLen(byte[] key) {
        return null
    }

    @Override
    List<byte[]> lRange(byte[] key, long begin, long end) {
        return null
    }

    @Override
    void lTrim(byte[] key, long begin, long end) {

    }

    @Override
    byte[] lIndex(byte[] key, long index) {
        return new byte[0]
    }

    @Override
    Long lInsert(byte[] key, RedisListCommands.Position where, byte[] pivot, byte[] value) {
        return null
    }

    @Override
    void lSet(byte[] key, long index, byte[] value) {

    }

    @Override
    Long lRem(byte[] key, long count, byte[] value) {
        return null
    }

    @Override
    byte[] lPop(byte[] key) {
        return new byte[0]
    }

    @Override
    byte[] rPop(byte[] key) {
        return new byte[0]
    }

    @Override
    List<byte[]> bLPop(int timeout, byte[] ... keys) {
        return null
    }

    @Override
    List<byte[]> bRPop(int timeout, byte[] ... keys) {
        return null
    }

    @Override
    byte[] rPopLPush(byte[] srcKey, byte[] dstKey) {
        return new byte[0]
    }

    @Override
    byte[] bRPopLPush(int timeout, byte[] srcKey, byte[] dstKey) {
        return new byte[0]
    }

    @Override
    boolean isSubscribed() {
        return false
    }

    @Override
    Subscription getSubscription() {
        return null
    }

    @Override
    Long publish(byte[] channel, byte[] message) {
        return null
    }

    @Override
    void subscribe(MessageListener listener, byte[] ... channels) {

    }

    @Override
    void pSubscribe(MessageListener listener, byte[] ... patterns) {

    }

    @Override
    void scriptFlush() {

    }

    @Override
    void scriptKill() {

    }

    @Override
    String scriptLoad(byte[] script) {
        return null
    }

    @Override
    List<Boolean> scriptExists(String... scriptShas) {
        return null
    }

    @Override
    def <T> T eval(byte[] script, ReturnType returnType, int numKeys, byte[] ... keysAndArgs) {
        return null
    }

    @Override
    def <T> T evalSha(String scriptSha, ReturnType returnType, int numKeys, byte[] ... keysAndArgs) {
        return null
    }

    @Override
    def <T> T evalSha(byte[] scriptSha, ReturnType returnType, int numKeys, byte[] ... keysAndArgs) {
        return null
    }

    @Override
    void bgWriteAof() {

    }

    @Override
    void bgReWriteAof() {

    }

    @Override
    void bgSave() {

    }

    @Override
    Long lastSave() {
        return null
    }

    @Override
    void save() {

    }

    @Override
    Long dbSize() {
        return null
    }

    @Override
    void flushDb() {

    }

    @Override
    void flushAll() {

    }

    @Override
    Properties info() {
        return null
    }

    @Override
    Properties info(String section) {
        return nu

    }

    @Override
    void shutdown() {

    }

    @Override
    void shutdown(RedisServerCommands.ShutdownOption option) {

    }

    @Override
    List<String> getConfig(String pattern) {
        return null
    }

    @Override
    void setConfig(String param, String value) {

    }

    @Override
    void resetConfigStats() {

    }

    @Override
    Long time() {
        return null
    }

    @Override
    void killClient(String host, int port) {

    }

    @Override
    void setClientName(byte[] name) {

    }

    @Override
    String getClientName() {
        return null
    }

    @Override
    List<RedisClientInfo> getClientList() {
        return null
    }

    @Override
    void slaveOf(String host, int port) {

    }

    @Override
    void slaveOfNoOne() {

    }

    @Override
    void migrate(byte[] key, RedisNode target, int dbIndex, RedisServerCommands.MigrateOption option) {

    }

    @Override
    void migrate(byte[] key, RedisNode target, int dbIndex, RedisServerCommands.MigrateOption option, long timeout) {

    }

    @Override
    Long sAdd(byte[] key, byte[] ... values) {
        return null
    }

    @Override
    Long sRem(byte[] key, byte[] ... values) {
        return null
    }

    @Override
    byte[] sPop(byte[] key) {
        return new byte[0]
    }

    @Override
    Boolean sMove(byte[] srcKey, byte[] destKey, byte[] value) {
        return null
    }

    @Override
    Long sCard(byte[] key) {
        return null
    }

    @Override
    Boolean sIsMember(byte[] key, byte[] value) {
        return null
    }

    @Override
    Set<byte[]> sInter(byte[] ... keys) {
        return null
    }

    @Override
    Long sInterStore(byte[] destKey, byte[] ... keys) {
        return null
    }

    @Override
    Set<byte[]> sUnion(byte[] ... keys) {
        return null
    }

    @Override
    Long sUnionStore(byte[] destKey, byte[] ... keys) {
        return null
    }

    @Override
    Set<byte[]> sDiff(byte[] ... keys) {
        return null
    }

    @Override
    Long sDiffStore(byte[] destKey, byte[] ... keys) {
        return null
    }

    @Override
    Set<byte[]> sMembers(byte[] key) {
        return null
    }

    @Override
    byte[] sRandMember(byte[] key) {
        return new byte[0]
    }

    @Override
    List<byte[]> sRandMember(byte[] key, long count) {
        return null
    }

    @Override
    Cursor<byte[]> sScan(byte[] key, ScanOptions options) {
        return null
    }

    @Override
    byte[] get(byte[] key) {
        return serialize(mock.get(unserialize(key)))
    }

    @Override
    byte[] getSet(byte[] key, byte[] value) {
        return serialize(mock.getset(unserialize(key), unserialize(value)))
    }

    @Override
    List<byte[]> mGet(byte[] ... keys) {
        return null
    }

    @Override
    void set(byte[] key, byte[] value) {
        mock.set(unserialize(key), unserialize(value))
    }

    @Override
    void set(byte[] key, byte[] value, Expiration expiration, RedisStringCommands.SetOption option) {

    }

    @Override
    Boolean setNX(byte[] key, byte[] value) {
        return mock.setnx()
    }

    @Override
    void setEx(byte[] key, long seconds, byte[] value) {

    }

    @Override
    void pSetEx(byte[] key, long milliseconds, byte[] value) {

    }

    @Override
    void mSet(Map<byte[], byte[]> tuple) {

    }

    @Override
    Boolean mSetNX(Map<byte[], byte[]> tuple) {
        return null
    }

    @Override
    Long incr(byte[] key) {
        return mock.incr(unserialize(key))
    }

    @Override
    Long incrBy(byte[] key, long value) {
        return mock.incrby(unserialize(key), value)
    }

    @Override
    Double incrBy(byte[] key, double value) {
        return Double.parseDouble(mock.incrbyfloat(unserialize(key), value))
    }

    @Override
    Long decr(byte[] key) {
        return mock.decr(unserialize(key))
    }

    @Override
    Long decrBy(byte[] key, long value) {
        return null
    }

    @Override
    Long append(byte[] key, byte[] value) {
        return null
    }

    @Override
    byte[] getRange(byte[] key, long begin, long end) {
        return new byte[0]
    }

    @Override
    void setRange(byte[] key, byte[] value, long offset) {

    }

    @Override
    Boolean getBit(byte[] key, long offset) {
        return null
    }

    @Override
    Boolean setBit(byte[] key, long offset, boolean value) {
        return null
    }

    @Override
    Long bitCount(byte[] key) {
        return null
    }

    @Override
    Long bitCount(byte[] key, long begin, long end) {
        return null
    }

    @Override
    Long bitOp(RedisStringCommands.BitOperation op, byte[] destination, byte[] ... keys) {
        return null
    }

    @Override
    Long strLen(byte[] key) {
        return null
    }

    @Override
    void multi() {

    }

    @Override
    List<Object> exec() {
        return null
    }

    @Override
    void discard() {

    }

    @Override
    void watch(byte[] ... keys) {

    }

    @Override
    void unwatch() {

    }

    @Override
    Boolean zAdd(byte[] key, double score, byte[] value) {
        return null
    }

    @Override
    Long zAdd(byte[] key, Set<RedisZSetCommands.Tuple> tuples) {
        return null
    }

    @Override
    Long zRem(byte[] key, byte[] ... values) {
        return null
    }

    @Override
    Double zIncrBy(byte[] key, double increment, byte[] value) {
        return null
    }

    @Override
    Long zRank(byte[] key, byte[] value) {
        return null
    }

    @Override
    Long zRevRank(byte[] key, byte[] value) {
        return null
    }

    @Override
    Set<byte[]> zRange(byte[] key, long begin, long end) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRangeWithScores(byte[] key, long begin, long end) {
        return null
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, double min, double max) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, double min, double max) {
        return null
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, double min, double max, long offset, long count) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit) {
        return null
    }

    @Override
    Set<byte[]> zRevRange(byte[] key, long begin, long end) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRevRangeWithScores(byte[] key, long begin, long end) {
        return null
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, double min, double max) {
        return null
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max) {
        return null
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, double min, double max, long offset, long count) {
        return null
    }

    @Override
    Set<byte[]> zRevRangeByScore(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, double min, double max, long offset, long count) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Set<RedisZSetCommands.Tuple> zRevRangeByScoreWithScores(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit) {
        return null
    }

    @Override
    Long zCount(byte[] key, double min, double max) {
        return null
    }

    @Override
    Long zCount(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Long zCard(byte[] key) {
        return null
    }

    @Override
    Double zScore(byte[] key, byte[] value) {
        return null
    }

    @Override
    Long zRemRange(byte[] key, long begin, long end) {
        return null
    }

    @Override
    Long zRemRangeByScore(byte[] key, double min, double max) {
        return null
    }

    @Override
    Long zRemRangeByScore(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Long zUnionStore(byte[] destKey, byte[] ... sets) {
        return null
    }

    @Override
    Long zUnionStore(byte[] destKey, RedisZSetCommands.Aggregate aggregate, int[] weights, byte[] ... sets) {
        return null
    }

    @Override
    Long zInterStore(byte[] destKey, byte[] ... sets) {
        return null
    }

    @Override
    Long zInterStore(byte[] destKey, RedisZSetCommands.Aggregate aggregate, int[] weights, byte[] ... sets) {
        return null
    }

    @Override
    Cursor<RedisZSetCommands.Tuple> zScan(byte[] key, ScanOptions options) {
        return null
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, String min, String max) {
        return null
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, String min, String max, long offset, long count) {
        return null
    }

    @Override
    Set<byte[]> zRangeByScore(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit) {
        return null
    }

    @Override
    Set<byte[]> zRangeByLex(byte[] key) {
        return null
    }

    @Override
    Set<byte[]> zRangeByLex(byte[] key, RedisZSetCommands.Range range) {
        return null
    }

    @Override
    Set<byte[]> zRangeByLex(byte[] key, RedisZSetCommands.Range range, RedisZSetCommands.Limit limit) {
        return null
    }
}
