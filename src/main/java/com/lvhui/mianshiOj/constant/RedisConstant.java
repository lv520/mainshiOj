package com.lvhui.mianshiOj.constant;

/**
 * Redis 常量
 */
public interface RedisConstant {
    /**
     * 用户签到 Redis Key 前缀
     */
    String USER_SIGN_IN_REDIS_KEY_PREFIX = "user:signins";

    /**
     * 获取用户签到 Redis Key
     * @param year
     * @param userid
     * @return
     */
    static String getUserSignInRedisKey(int year, long userid){
        return String.format("%s%s%s", USER_SIGN_IN_REDIS_KEY_PREFIX, year, userid);
    }
}
