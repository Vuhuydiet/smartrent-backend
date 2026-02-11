package com.smartrent.utility;

import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import lombok.extern.log4j.Log4j2;
import org.apache.commons.lang3.BooleanUtils;
import org.hibernate.QueryTimeoutException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Log4j2
public class RedisCache {

  @Autowired
  private RedisTemplate<String, String> redisString;

  public boolean acquireLockForKey(String lockKey, int lockTime, TimeUnit lockTimeUnit) {
    var isLocked = execute(() -> redisString.opsForValue()
        .setIfAbsent(lockKey, "locked", lockTime, lockTimeUnit));
    return BooleanUtils.isTrue(isLocked);
  }
  public void releaseLockForKey(String lockKey) {
    if (lockKey != null) {
      execute(() -> redisString.delete(lockKey));
    }
  }

  public boolean hasLockKey(String lockKey) {
    if (lockKey != null) {
      var hasKey = execute(() -> redisString.hasKey(lockKey));
      return  BooleanUtils.isTrue(hasKey);
    }
    return false;
  }

  private <T> T execute(Supplier<T> functions) {
    try {
      return functions.get();
    } catch (Exception exception) {
      if (exception instanceof RedisConnectionFailureException
          || exception instanceof QueryTimeoutException) {
        log.error("Exception when execute action on Redis", exception);
        return (T) Boolean.TRUE;
      }
      throw exception;
    }
  }

  public Integer getIntValue(String key) {
    if (key != null) {
      var value = redisString.opsForValue().get(key);
      if (value != null) {
        return Integer.valueOf(value);
      }
    }
    return 0;
  }

  public Optional<String> getValue(String key) {
    if (key != null) {
      return Optional.ofNullable(redisString.opsForValue().get(key));
    }
    return Optional.empty();
  }

  public void cacheValue(String key, Integer value, int lockTime, TimeUnit lockTimeUnit) {
    redisString.opsForValue().set(key, value.toString(), lockTime, lockTimeUnit);
  }

  public void cacheValue(String key, String value, long lockTime, TimeUnit lockTimeUnit) {
    redisString.opsForValue().setIfAbsent(key, value, lockTime, lockTimeUnit);
  }
}
