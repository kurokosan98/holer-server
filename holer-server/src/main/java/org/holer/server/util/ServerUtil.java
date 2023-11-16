//
// Source code recreated from a .class file by IntelliJ IDEA
// (powered by FernFlower decompiler)
//

package org.holer.server.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.holer.common.util.HolerUtil;
import org.holer.server.constant.HolerCode;
import org.holer.server.db.DBClientService;
import org.holer.server.db.DBPortService;
import org.holer.server.db.DBUserService;
import org.holer.server.model.HolerLicense;
import org.holer.server.model.HolerResult;
import org.holer.server.model.HolerStatus;
import org.holer.server.model.HolerUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class ServerUtil {
    private static final Logger log = LoggerFactory.getLogger(ServerUtil.class);
    @Autowired
    private DBUserService userService;
    @Autowired
    private DBPortService portService;
    @Autowired
    private DBClientService clientService;
    private OkHttpClient httpClient;
    private static Map<Integer, HolerStatus> msg = null;
    private static ApplicationContext appCtx = null;
    private static ServerUtil util;

    public ServerUtil() {
    }

    @PostConstruct
    public void init() {
        util = this;
        util.userService = this.userService;
        util.clientService = this.clientService;
        util.portService = this.portService;
        util.httpClient = new OkHttpClient();
    }

    public static ServerUtil getInstance() {
        return util;
    }

    public DBUserService getUserService() {
        return this.userService;
    }

    public DBClientService getClientService() {
        return this.clientService;
    }

    public DBPortService getPortService() {
        return this.portService;
    }

    public static void setAppCtx(ApplicationContext appCtx) {
        ServerUtil.appCtx = appCtx;
    }

    public static String property(String key) {
        return null == appCtx ? "" : appCtx.getEnvironment().getProperty(key);
    }

    public static HolerStatus status(Integer code) {
        if (null == msg) {
            String json = "";

            try {
                InputStream is = HolerStatus.class.getClassLoader().getResourceAsStream("conf/message.json");
                json = IOUtils.toString(is, "UTF-8");
                HolerUtil.close(is);
            } catch (IOException var3) {
                log.error("Failed to read file: conf/message.json", var3);
                return null;
            }

            msg = (Map)HolerUtil.toObject(json, new TypeToken<Map<Integer, HolerStatus>>() {
            });
        }

        if (null != msg && msg.containsKey(code)) {
            HolerStatus status = (HolerStatus)msg.get(code);
            status.setCode(code);
            status.setMsg(status.getMsgZhCN());
            return status;
        } else {
            return null;
        }
    }

    public static boolean isValid(String str, String regex) {
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(str);
        return matcher.matches();
    }

    public static HolerCode isValidServer(String server) {
        if (StringUtils.isBlank(server)) {
            return HolerCode.BAD_SERVER;
        } else if (!isValid(server, "^[0-9a-zA-Z.-:]{3,64}$")) {
            return HolerCode.BAD_SERVER;
        } else {
            String portStr = StringUtils.substringAfter(server, ":");
            if (StringUtils.isBlank(portStr)) {
                return HolerCode.BAD_SERVER;
            } else if (!StringUtils.isNumeric(portStr)) {
                return HolerCode.BAD_SERVER;
            } else {
                int portNum = Integer.parseInt(portStr);
                return portNum >= 1 && portNum <= 65535 ? HolerCode.OK : HolerCode.BAD_PRIVATE_PORT;
            }
        }
    }

    public static <T> HolerResult<T> success() {
        return new HolerResult(HolerCode.OK);
    }

    public static <T> HolerResult<T> success(T data) {
        return new HolerResult(data, HolerCode.OK);
    }

    public static <T> HolerResult<T> failure(HolerCode code) {
        return new HolerResult(code);
    }

    public static void write(HttpServletResponse rep, HolerResult<?> result) throws IOException {
        rep.setCharacterEncoding("UTF-8");
        rep.setContentType("application/json;charset=utf-8");
        rep.getWriter().write(HolerUtil.toJson(result));
    }

    public static boolean isValidToken(HttpServletRequest req) {
        String ctoken = req.getHeader("HOLER-AUTH-TOKEN");
        if (StringUtils.isBlank(ctoken)) {
            return false;
        } else {
            HttpSession session = req.getSession();
            String stoken = (String)session.getAttribute("HOLER-AUTH-TOKEN");
            if (StringUtils.isBlank(stoken)) {
                HolerUser dbUser = util.userService.findByToken(ctoken);
                if (null != dbUser && !StringUtils.isEmpty(dbUser.getToken())) {
                    session.setAttribute("HOLER-AUTH-TOKEN", dbUser.getToken());
                    return true;
                } else {
                    return false;
                }
            } else {
                return ctoken.equals(stoken);
            }
        }
    }

    public static String randomID() {
        String randomID = UUID.randomUUID().toString();
        randomID = StringUtils.replace(randomID, "-", "");
        return randomID.toLowerCase();
    }

    public static String defaultDomain(int portNum) {
        String portStr = "";
        if (portNum >= 1 && portNum <= 65535) {
            portStr = String.valueOf(portNum);
        }

        return String.format("holer%s.", portStr) + ServerMgr.getDomainName();
    }

    public static <T> T clone(T obj) {
        try {
            T destObj = (T) Class.forName(obj.getClass().getName()).newInstance();
            BeanUtils.copyProperties(destObj, obj);
            return destObj;
        } catch (Exception var2) {
            return null;
        }
    }

    public static <T> T toOject(String content, Class<T> clas) {
        ObjectMapper mapper = new ObjectMapper();

        try {
            return mapper.readValue(content, clas);
        } catch (Exception var4) {
            return null;
        }
    }

    public static long divide(long a, long b) {
        return BigDecimal.valueOf(a).divide(BigDecimal.valueOf(b), 0, 4).longValue();
    }

    public static boolean isWin() {
        String os = System.getProperty("os.name").toLowerCase();
        return os.startsWith("win");
    }

    public static HolerResult<?> verifyLicense() {
        String serialNo = ServerMgr.getSerialNo();
        if (StringUtils.isBlank(serialNo)) {
            return new HolerResult(HolerCode.ERR_DEFAULT_MAPPING);
        } else {
            StringBuilder url = new StringBuilder();
            url.append(String.format("http://holer.%s/holer-api/", ServerMgr.getServerDomain())).append("license").append("/").append(serialNo);

            try {
                Request req = (new Request.Builder()).url(url.toString()).get().build();
                Response rep = util.httpClient.newCall(req).execute();
                return (HolerResult)HolerUtil.toObject(rep.body().string(), new TypeToken<HolerResult<HolerLicense>>() {
                });
            } catch (IOException var4) {
                log.error("Failed to verify license. {}", var4.getMessage());
                return new HolerResult(HolerCode.ERR_VERIFY_LICENSE);
            }
        }
    }

    public static HolerResult<?> totalNum() {

        HolerResult<?> result = new HolerResult(HolerCode.ERR_DEFAULT_MAPPING);
        result.setTotal(99999L);
        return result;

//        HolerResult<?> result = verifyLicense();
//        if (null != result && null != result.getData()) {
//            HolerLicense license = (HolerLicense)result.getData();
//            if (license.getValidDay() <= 0L) {
//                result = new HolerResult(HolerCode.ERR_EXPIRED_LICENSE);
//                result.setTotal(1L);
//                return result;
//            } else {
//                result.setTotal(license.getPortNum() + 1L);
//                return result;
//            }
//        } else {
//            result = new HolerResult(HolerCode.ERR_DEFAULT_MAPPING);
//            result.setTotal(99999L);
//            return result;
//        }
    }
}
