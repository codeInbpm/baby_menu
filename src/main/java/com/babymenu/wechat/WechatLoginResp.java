package com.babymenu.wechat;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class WechatLoginResp {
    private String openid;
    @JsonProperty("session_key")
    private String sessionKey;
    private String unionid;
    private Integer errcode;
    private String errmsg;
}
