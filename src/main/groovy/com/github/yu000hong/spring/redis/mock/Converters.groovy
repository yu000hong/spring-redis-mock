package com.github.yu000hong.spring.redis.mock

import org.rarefiedredis.redis.IRedisSortedSet
import org.springframework.data.redis.connection.DefaultTuple
import org.springframework.data.redis.connection.RedisZSetCommands.Tuple

import static com.github.yu000hong.spring.redis.mock.RedisMockUtil.serialize
import static com.github.yu000hong.spring.redis.mock.RedisMockUtil.unserialize

/**
 * 类型转换Closure
 */
class Converters {

    /**
     * DO NOTHING
     */
    public static final Closure DO_NOTHING = { obj ->
        return obj
    }

    /**
     * String -> Double
     */
    public static final Closure<Double> STRING_TO_DOUBLE = { String string ->
        return Double.parseDouble(string)
    }

    /**
     * byte[] -> String
     */
    public static final Closure<String> BYTES_TO_STRING = { byte[] bytes ->
        return unserialize(bytes)
    }

    /**
     * String -> byte[]
     */
    public static final Closure<byte[]> STRING_TO_BYTES = { String string ->
        return serialize(string)
    }

    /**
     * String[] -> List<byte[]>
     */
    public static final Closure<List<byte[]>> STRINGS_TO_BYTESLIST = { String[] strings ->
        if (strings == null) {
            return null
        }
        return strings.collect { string ->
            return serialize(string)
        }
    }

    /**
     * List<String> -> List<byte[]>
     */
    public static final Closure<List<byte[]>> STRINGLIST_TO_BYTESLIST = { List<String> list ->
        if (list == null) {
            return null
        }
        return list.collect { String item ->
            return serialize(item)
        }
    }

    /**
     * Set<String> -> Set<byte[]>
     */
    public static final Closure<Set<byte[]>> STRINGSET_TO_BYTESSET = { Set<String> set ->
        if (set == null) {
            return null
        }
        def results = [] as Set
        set.each { key ->
            results << serialize(key)
        }
        return results
    }

    /**
     * Map<String,String> -> Map<byte[], byte[]>
     */
    public static final Closure<Map<byte[], byte[]>> STRINGMAP_TO_BYTESMAP = { Map<String, String> map ->
        if (map == null) {
            return null
        }
        def results = [:]
        map.each { k, v ->
            results[serialize(k)] = serialize(v)
        }
        return results
    }

    public static final Closure<Set<byte[]>> PAIRSET_TO_BYTESSET = { Set<IRedisSortedSet.ZsetPair> set ->
        return set.collect { pair -> serialize(pair.member) } as Set<byte[]>
    }

    public static final Closure<Set<Tuple>> PAIRSET_TO_TUPLESET = { Set<IRedisSortedSet.ZsetPair> set ->
        return set.collect { pair -> new DefaultTuple(serialize(pair.member), pair.score) } as Set<Tuple>
    }

    /**
     * Long -> Boolean
     */
    public static final Closure<Boolean> LONG_TO_BOOLEAN = { Long value ->
        return value == null ? false : value as Boolean
    }

    /**
     * return null
     */
    public static final Closure<Void> NULL = {
        return null
    }

}
