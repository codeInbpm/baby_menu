package com.babymenu.wechat;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.babymenu.common.BizException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WechatService {

    @Value("${wechat.appid}")
    private String appid;

    @Value("${wechat.secret}")
    private String secret;

    private final StringRedisTemplate redis;

    public WechatService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    private static final String JSCODE_URL = "https://api.weixin.qq.com/sns/jscode2session";
    private static final String TOKEN_URL = "https://api.weixin.qq.com/cgi-bin/token";
    private static final String TOKEN_CACHE_KEY = "wx:access_token";

    /** code 换 openid */
    public WechatLoginResp code2Session(String jsCode) {
        String url = JSCODE_URL
                + "?appid=" + appid
                + "&secret=" + secret
                + "&js_code=" + jsCode
                + "&grant_type=authorization_code";
        String body = HttpUtil.get(url);
        log.info("code2Session: {}", body);
        WechatLoginResp resp = JSONUtil.toBean(body, WechatLoginResp.class);
        if (resp.getOpenid() == null) {
            throw new BizException("微信登录失败: " + resp.getErrmsg());
        }
        return resp;
    }

    /** 获取 access_token，带 Redis 缓存 */
    public String getAccessToken() {
        try {
            String cached = redis.opsForValue().get(TOKEN_CACHE_KEY);
            if (cached != null && !cached.isBlank()) return cached;
        } catch (Exception e) {
            log.warn("Redis 不可用，直接调微信接口");
        }
        String url = TOKEN_URL + "?grant_type=client_credential&appid=" + appid + "&secret=" + secret;
        String body = HttpUtil.get(url);
        log.info("get access_token: {}", body);
        cn.hutool.json.JSONObject json = JSONUtil.parseObj(body);
        String token = json.getStr("access_token");
        Integer expiresIn = json.getInt("expires_in", 7200);
        if (token == null) {
            throw new BizException("获取微信 access_token 失败: " + body);
        }
        try {
            redis.opsForValue().set(TOKEN_CACHE_KEY, token, expiresIn - 200, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return token;
    }
}
