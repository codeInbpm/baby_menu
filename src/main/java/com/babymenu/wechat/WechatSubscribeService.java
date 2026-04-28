package com.babymenu.wechat;

import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信小程序订阅消息推送
 * 模板：留言提醒（公共模板库 · 餐厅排队类目）
 *
 * 字段约定：
 *  - thing1.DATA  用户名称  ≤ 20 字符（用于显示发起人昵称）
 *  - thing2.DATA  备注消息  ≤ 20 字符（用于显示服务内容 / 通知正文）
 *  - time3.DATA   留言日期  yyyy-MM-dd HH:mm
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WechatSubscribeService {

    private final WechatService wechatService;

    @Value("${wechat.template-id}")
    private String templateId;

    @Value("${wechat.miniprogram-state}")
    private String miniprogramState;

    private static final String SEND_URL = "https://api.weixin.qq.com/cgi-bin/message/subscribe/send";
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 发送服务请求订阅消息
     *
     * @param toOpenid     接收人（伴侣）openid
     * @param fromNickname 发起人昵称（显示在 thing1 用户名称里）
     * @param itemContent  服务内容（如 "洗洗脚 + 按后背"），会自动加 "想要 " 前缀
     */
    public boolean sendRequestNotify(String toOpenid, String fromNickname, String itemContent) {
        return doSend(
                toOpenid,
                safeName(fromNickname),
                trimThing("想要 " + safeText(itemContent))
        );
    }

    /**
     * 发送绑定成功通知（复用同一模板）
     */
    public boolean sendBindNotify(String toOpenid, String partnerNickname) {
        return doSend(
                toOpenid,
                safeName(partnerNickname),
                trimThing("和你绑定成功啦 ❤️")
        );
    }

    private boolean doSend(String toOpenid, String userName, String message) {
        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        // 模板字段顺序必须与微信公共模板「留言提醒」一致
        data.put("thing1", Map.of("value", userName));                                  // 用户名称
        data.put("thing2", Map.of("value", message));                                   // 备注消息
        data.put("time3",  Map.of("value", LocalDateTime.now().format(TIME_FMT)));      // 留言日期

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("template_id", templateId);
        body.put("touser", toOpenid);
        body.put("data", data);
        body.put("miniprogram_state", miniprogramState);
        body.put("lang", "zh_CN");

        String url = SEND_URL + "?access_token=" + wechatService.getAccessToken();
        String resp = HttpUtil.post(url, JSONUtil.toJsonStr(body));
        log.info("订阅消息发送 to={} body={} resp={}", toOpenid, JSONUtil.toJsonStr(body), resp);
        JSONObject json = JSONUtil.parseObj(resp);
        return json.getInt("errcode", -1) == 0;
    }

    /** thing 类型最多 20 个字符 */
    private String trimThing(String s) {
        if (s == null) return "";
        return s.length() > 20 ? s.substring(0, 20) : s;
    }

    private String safeText(String s) {
        return s == null ? "" : s;
    }

    /** 用户名称做兜底 + 截断（≤ 20 字符以保安全） */
    private String safeName(String n) {
        if (n == null || n.isBlank()) return "宝贝";
        return n.length() > 20 ? n.substring(0, 20) : n;
    }
}
