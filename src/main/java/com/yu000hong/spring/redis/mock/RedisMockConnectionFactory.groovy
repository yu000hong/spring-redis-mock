package com.yu000hong.spring.redis.mock

import org.rarefiedredis.redis.RedisMock
import org.springframework.beans.factory.DisposableBean
import org.springframework.beans.factory.InitializingBean
import org.springframework.dao.DataAccessException
import org.springframework.data.redis.connection.RedisClusterConnection
import org.springframework.data.redis.connection.RedisConnection
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.connection.RedisSentinelConnection

class RedisMockConnectionFactory implements InitializingBean, DisposableBean, RedisConnectionFactory {
    private final RedisMock redisMock = new RedisMock()

    public RedisMock getRedisMock() {
        return redisMock
    }

    @Override
    void destroy() throws Exception {
        //do nothing
    }

    @Override
    void afterPropertiesSet() throws Exception {
        //do nothing
    }

    @Override
    RedisConnection getConnection() {
        return new RedisMockConnection(redisMock)
    }

    @Override
    RedisClusterConnection getClusterConnection() {
        throw new UnsupportedOperationException()
    }

    @Override
    boolean getConvertPipelineAndTxResults() {
        return false
    }

    @Override
    RedisSentinelConnection getSentinelConnection() {
        throw new UnsupportedOperationException()
    }

    @Override
    DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return new DataAccessException(ex.message, ex) {}
    }
}
