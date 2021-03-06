package com.gala.tvapi.vrs.p031a;

import com.alibaba.fastjson.JSON;
import com.gala.tvapi.log.TVApiRecordLog;
import com.gala.tvapi.p008b.C0213b;
import com.gala.tvapi.p008b.C0218c;
import com.gala.tvapi.tools.TVApiTool;
import com.gala.tvapi.tv2.TVApiBase;
import com.gala.tvapi.tv2.p026c.C0289c;
import com.gala.tvapi.type.PlatformType;
import com.gala.video.api.ApiResult;
import java.util.List;

public class C0336k<T extends ApiResult> extends C0289c<T> {
    private static C0213b f1211a;
    private static PlatformType f1212a;
    protected String f1213b = "";

    public C0336k() {
        PlatformType platform = TVApiBase.getTVApiProperty().getPlatform();
        f1212a = platform;
        f1211a = C0218c.m605a(platform);
    }

    public final void mo855a(boolean z, String str, String str2, String str3) {
        if (!TVApiBase.getTVApiProperty().isSendLogRecord()) {
            return;
        }
        if (z && str != null && (str.contains("services/ck.action") || str.contains("/tmts/") || str.contains("getalbumrc"))) {
            TVApiRecordLog.addTVApiLogRecordLog(str, C0289c.mo865b() + "-" + str2 + " " + str3);
        } else if (!z) {
            TVApiRecordLog.addTVApiLogRecordLog(str, C0289c.mo865b() + "-" + str2 + " " + str3);
        }
    }

    public T mo850a(String str, Class<T> cls) {
        if (!(this.a == null || str == null)) {
            this.a.mo848a(str);
        }
        if (str == null) {
            str = this.a.mo846a();
        }
        int indexOf = str.indexOf("[");
        int indexOf2 = str.indexOf("{");
        if (indexOf == 0) {
            return (ApiResult) JSON.parseObject(str, (Class) cls);
        }
        if (indexOf2 == 0) {
            return (ApiResult) JSON.parseObject(str, (Class) cls);
        }
        if (indexOf > 0 && indexOf2 > 0) {
            if (indexOf < indexOf2) {
                return (ApiResult) JSON.parseObject(str.substring(indexOf), (Class) cls);
            }
            if (indexOf > indexOf2) {
                return (ApiResult) JSON.parseObject(str.substring(indexOf2), (Class) cls);
            }
        }
        return (ApiResult) JSON.parseObject(str, (Class) cls);
    }

    public final List<String> mo853a(List<String> list) {
        return list;
    }

    public String mo852a(String str) {
        String parseLicenceUrl = TVApiTool.parseLicenceUrl(str);
        if (parseLicenceUrl.contains("m=AUTHID") || parseLicenceUrl.contains("deviceId=AUTHID")) {
            if (!TVApiBase.getTVApiProperty().checkAuthIdAndApiKeyAvailable()) {
                C0289c.mo865b();
            }
            if (parseLicenceUrl.contains("m=AUTHID")) {
                parseLicenceUrl = parseLicenceUrl.replace("m=AUTHID", "m=" + TVApiBase.getTVApiProperty().getAuthId());
            }
            if (parseLicenceUrl.contains("deviceId=AUTHID")) {
                parseLicenceUrl = parseLicenceUrl.replace("deviceId=AUTHID", "deviceId=" + TVApiBase.getTVApiProperty().getAuthId());
            }
        }
        if (parseLicenceUrl.contains("nolimit=0") && TVApiBase.getTVApiProperty().isOpenOverSea()) {
            parseLicenceUrl = parseLicenceUrl.replace("nolimit=0", "nolimit=1");
        }
        if (f1212a != PlatformType.NORMAL) {
            if (f1212a == PlatformType.TAIWAN) {
                String domain = TVApiBase.getTVApiProperty().getDomain();
                if (!parseLicenceUrl.contains("qd_sc")) {
                    if (parseLicenceUrl.contains("agenttype=28")) {
                        parseLicenceUrl = parseLicenceUrl.replace("agenttype=28", "agenttype=18");
                    }
                    if (parseLicenceUrl.contains("lang=zh_CN")) {
                        parseLicenceUrl = parseLicenceUrl.replace("lang=zh_CN", "lang=zh_TW");
                    }
                    if (parseLicenceUrl.contains("l.rcd.i" + domain + "/apis/")) {
                        if (parseLicenceUrl.contains("terminalId=52")) {
                            parseLicenceUrl = parseLicenceUrl.replace("terminalId=52", "terminalId=53");
                        }
                        if (!parseLicenceUrl.contains("&agent_type=28")) {
                            parseLicenceUrl = parseLicenceUrl + "&agent_type=28";
                        }
                        if (parseLicenceUrl.contains("setrc") || parseLicenceUrl.contains("getrc") || parseLicenceUrl.contains("getallrc")) {
                            parseLicenceUrl = parseLicenceUrl + "&locale=zh_TW";
                        }
                    }
                    if (parseLicenceUrl.contains("watchlater/list.action")) {
                        parseLicenceUrl = parseLicenceUrl.replace("watchlater/list.action", "watchlater/zh_tw/list.action");
                    }
                    if (parseLicenceUrl.contains("/itv/ichannel/") || parseLicenceUrl.contains("/itv/plst/") || parseLicenceUrl.contains("http://mixer.video.i" + domain + "/vms?") || parseLicenceUrl.contains("/itv/groupdetail") || parseLicenceUrl.contains("/itv/group/")) {
                        parseLicenceUrl = parseLicenceUrl + "&locale=zh_tw";
                    }
                    if (parseLicenceUrl.contains("qrcode/") || parseLicenceUrl.contains("/reglogin/") || parseLicenceUrl.contains("send_cellphone_authcode_vcode")) {
                        parseLicenceUrl = parseLicenceUrl + "&lang=zh_TW";
                        if (parseLicenceUrl.contains("tv_cellphone_reg.action") || parseLicenceUrl.contains("send_cellphone_authcode_vcode.action")) {
                            parseLicenceUrl = parseLicenceUrl + "&area_code=886";
                        }
                    }
                    if (parseLicenceUrl.contains("/recommend/videos")) {
                        parseLicenceUrl = parseLicenceUrl + "&locale=ZH_TW&locationMode=2";
                    }
                    if (parseLicenceUrl.contains("ptid=")) {
                        parseLicenceUrl = parseLicenceUrl + "&app_version=" + TVApiBase.getTVApiProperty().getVersion();
                    }
                    if (parseLicenceUrl.contains("services/queryVodInfo.action?")) {
                        parseLicenceUrl = parseLicenceUrl + "&area=tw";
                    }
                }
            }
            parseLicenceUrl = parseLicenceUrl.replace("04022001010000000000", f1211a.mo829a()).replace("890dbe91fbadca03", f1211a.mo831c());
        }
        this.f1213b = parseLicenceUrl;
        return parseLicenceUrl;
    }
}
