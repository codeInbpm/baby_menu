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
 * 模板：照顾宝宝任务提醒（健康管理类目）
 *
 * 字段约定：
 *  - thing1.DATA  宝宝名称（用于显示发起人昵称）
 *  - thing3.DATA  任务名称（用于显示服务内容 / 通知正文）
 *  - time4.DATA   任务时间（yyyy-MM-dd HH:mm）
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
     * @param fromNickname 发起人昵称（显示在 thing1 宝宝名称里）
     * @param itemContent  服务内容（如 "洗洗脚 + 按后背"），会自动加 "想要 " 前缀
     */
    public boolean sendRequestNotify(String toOpenid, String fromNickname, String itemContent, Long requestId) {
        String pagePath = requestId != null ? "pages/request/detail?id=" + requestId : "pages/request/list";
        return doSendGeneric(toOpenid, fromNickname, "想要 " + safeText(itemContent), pagePath);
    }

    public boolean sendActiveServiceCardNotify(String toOpenid, String fromNickname, Long cardId) {
        return doSendGeneric(
                toOpenid,
                fromNickname,
                "想被你宠一下(主动服务卡)",
                "pages/menu/index?from=princessServiceCard&cardId=" + cardId
        );
    }

    /**
     * 发送绑定成功通知（复用同一模板）
     */
    public boolean sendBindNotify(String toOpenid, String partnerNickname) {
        return doSendGeneric(
                toOpenid,
                partnerNickname,
                "和你绑定成功啦 ❤️",
                "pages/profile/index"
        );
    }

    /**
     * 发送分配给对方的积分通知
     */
    public boolean sendAllocateNotify(String toOpenid, String partnerNickname, Integer amount) {
        return doSendGeneric(
                toOpenid,
                partnerNickname,
                "给你分配了 " + amount + " 积分！",
                "pages/profile/index"
        );
    }
    
    public boolean sendSwitchRoleRequestNotify(String toOpenid, String partnerNickname) {
        return doSendGeneric(
                toOpenid,
                partnerNickname,
                "申请和你互换角色啦～ 要不要也宠TA一次呢？",
                "pages/profile/index"
        );
    }

    public boolean sendSwitchRoleAcceptNotify(String toOpenid, String partnerNickname) {
        return doSendGeneric(
                toOpenid,
                partnerNickname,
                "已同意和你互换角色，现在你是主子啦 ❤️",
                "pages/profile/index"
        );
    }

    public boolean doSendGeneric(String toOpenid, String userName, String message, String pagePath) {
        if (toOpenid == null || toOpenid.isBlank()) {
            log.warn("取消发送订阅消息：接收人 openid 为空");
            return false;
        }

        Map<String, Map<String, String>> data = new LinkedHashMap<>();
        // 模板字段严格匹配截图：thing1(宝宝名称), thing3(任务名称), time4(任务时间)
        data.put("thing1", Map.of("value", safeName(userName)));
        data.put("thing3", Map.of("value", trimThing(message)));
        data.put("time4",  Map.of("value", LocalDateTime.now().format(TIME_FMT)));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("template_id", templateId);
        body.put("touser", toOpenid);
        if (pagePath != null && !pagePath.isEmpty()) {
            body.put("page", pagePath);
        }
        body.put("data", data);
        body.put("miniprogram_state", miniprogramState);
        body.put("lang", "zh_CN");

        String url = SEND_URL + "?access_token=" + wechatService.getAccessToken();
        try {
            String resp = cn.hutool.http.HttpRequest.post(url)
                    .body(JSONUtil.toJsonStr(body))
                    .execute().body();
            JSONObject json = JSONUtil.parseObj(resp);
            int errcode = json.getInt("errcode", -1);
            if (errcode == 0) {
                log.info("订阅消息发送成功 to={}", toOpenid);
                return true;
            } else {
                log.warn("订阅消息发送失败 to={} body={} resp={}", toOpenid, JSONUtil.toJsonStr(body), resp);
                return false;
            }
        } catch (Exception e) {
            log.error("订阅消息发送异常 to={}: {}", toOpenid, e.getMessage());
            return false;
        }
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
