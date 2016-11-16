package com.yu000hong.spring.redis.mock

class Converters {

    public static final Closure DO_NOTHING = { obj ->
        return obj
    }

    public static final Closure<Double> STRING_TO_DOUBLE = { String string ->
        return Double.parseDouble(string)
    }

    public static final Closure<String> BYTES_TO_STRING = { byte[] bytes ->
        return RedisMockUtil.unserialize(bytes)
    }

    public static final Closure<byte[]> STRING_TO_BYTES = { String string ->
        return RedisMockUtil.serialize(string)
    }

    public static final Closure<List<byte[]>> STRINGS_TO_BYTESLIST = { String[] strings ->
        if (strings == null) {
            return null
        }
        return strings.collect { string ->
            return RedisMockUtil.serialize(string)
        }
    }

    public static final Closure<List<byte[]>> STRINGLIST_TO_BYTESLIST = { List<String> list ->
        if (list == null) {
            return null
        }
        return list.collect { String item ->
            return RedisMockUtil.serialize(item)
        }
    }

    public static final Closure<Set<byte[]>> STRINGSET_TO_BYTESSET = { Set<String> set ->
        if (set == null) {
            return null
        }
        def results = [] as Set
        set.each { key ->
            results << RedisMockUtil.serialize(key)
        }
        return results
    }

    public static final Closure<Map<byte[], byte[]>> STRINGMAP_TO_BYTESMAP = { Map<String, String> map ->
        if (map == null) {
            return null
        }
        def results = [:]
        map.each { k, v ->
            results[RedisMockUtil.serialize(k)] = RedisMockUtil.serialize(v)
        }
        return results
    }

    public static final Closure<Boolean> LONG_TO_BOOLEAN = { Long value ->
        if (value == null) {
            return false
        } else {
            return value as Boolean
        }
    }

    public static final Closure<Void> NULL = {
        return null
    }

}
