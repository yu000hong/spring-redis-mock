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
    private RedisMockConnection connection

    @Override
    void destroy() throws Exception {

    }

    @Override
    void afterPropertiesSet() throws Exception {

    }

    @Override
    RedisConnection getConnection() {
        if(connection == null){
            connection = new RedisMockConnection()
        }
        return null
    }

    @Override
    RedisClusterConnection getClusterConnection() {
        return null
    }

    @Override
    boolean getConvertPipelineAndTxResults() {
        return false
    }

    @Override
    RedisSentinelConnection getSentinelConnection() {
        return null
    }

    @Override
    DataAccessException translateExceptionIfPossible(RuntimeException ex) {
        return null
    }
}
