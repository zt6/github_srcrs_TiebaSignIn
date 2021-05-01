package top.srcrs;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.TypeReference;
import org.apache.http.HttpException;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import top.srcrs.domain.Cookie;
import top.srcrs.util.Encryption;
import top.srcrs.util.Request;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 程序运行开始的地方
 *
 * @author srcrs
 * @Time 2020-10-31
 */
public class Run {
    // 获取日志记录器对象
    private static final Logger LOGGER = LoggerFactory.getLogger(Run.class);

    /**
     * 获取用户所有关注贴吧
     */
    String LIKE_URL = "https://tieba.baidu.com/mo/q/newmoindex";
    /**
     * 获取用户的tbs
     */
    String TBS_URL = "http://tieba.baidu.com/dc/common/tbs";
    /**
     * 贴吧签到接口
     */
    String SIGN_URL = "http://c.tieba.baidu.com/c/c/forum/sign";

    /**
     * 存储用户所关注的贴吧
     */
    private List<String> follow = new ArrayList<>();
    /**
     * 签到成功的贴吧列表
     */
    private static List<String> success = new ArrayList<>();
    /**
     * 用户的tbs
     */
    private String tbs = "";
    /**
     * 用户所关注的贴吧数量
     */
    private static Integer followNum = 201;

    public static void main(String[] args) {
        Cookie cookie = Cookie.getInstance();
        // 存入Cookie，以备使用
        if (args.length == 0) {
            LOGGER.warn("请在Secrets中填写BDUSS");
        }
        cookie.setBDUSS(args[0]);
        Run run = new Run();
        run.getTbs();
        run.getFollow();
        run.runSign();
        LOGGER.info("共 {} 个贴吧 - 成功: {} - 失败: {}", followNum, success.size(), followNum - success.size());
        if (args.length == 2) {
            run.send(args[1]);
        }
    }

    /**
     * 进行登录，获得 tbs ，签到的时候需要用到这个参数
     *
     * @author srcrs
     * @Time 2020-10-31
     */
    public void getTbs() {
        try {
            JSONObject jsonObject = Request.get(TBS_URL);
            if ("1".equals(jsonObject.getString("is_login"))) {
                LOGGER.info("获取tbs成功");
                tbs = jsonObject.getString("tbs");
            } else {
                LOGGER.warn("获取tbs失败 -- " + jsonObject);
            }
        } catch (Exception e) {
            LOGGER.error("获取tbs部分出现错误 -- " + e);
        }
    }

    /**
     * 获取用户所关注的贴吧列表
     *
     * @author srcrs
     * @Time 2020-10-31
     */
    public void getFollow() {
        try {
            JSONObject jsonObject = Request.get(LIKE_URL);
            LOGGER.info("获取贴吧列表成功");
            JSONArray jsonArray = jsonObject.getJSONObject("data").getJSONArray("like_forum");
            followNum = jsonArray.size();
            // 获取用户所有关注的贴吧
            for (Object array : jsonArray) {
                if ("0".equals(((JSONObject) array).getString("is_sign"))) {
                    // 将为签到的贴吧加入到 follow 中，待签到
                    follow.add(((JSONObject) array).getString("forum_name").replace("+", "%2B"));
                } else {
                    // 将已经成功签到的贴吧，加入到 success
                    success.add(((JSONObject) array).getString("forum_name"));
                }
            }
        } catch (Exception e) {
            LOGGER.error("获取贴吧列表部分出现错误 -- " + e);
        }
    }

    /**
     * 开始进行签到，每一轮性将所有未签到的贴吧进行签到，一共进行5轮，如果还未签到完就立即结束
     * 一般一次只会有少数的贴吧未能完成签到，为了减少接口访问次数，每一轮签到完等待1分钟，如果在过程中所有贴吧签到完则结束。
     *
     * @author srcrs
     * @Time 2020-10-31
     */
    public void runSign() {
        // 当执行 5 轮所有贴吧还未签到成功就结束操作
        Integer flag = 5;
        try {
            while (success.size() < followNum && flag > 0) {
                LOGGER.info("-----第 {} 轮签到开始-----", 5 - flag + 1);
                LOGGER.info("还剩 {} 贴吧需要签到", followNum - success.size());
                Iterator<String> iterator = follow.iterator();
                while (iterator.hasNext()) {
                    String s = iterator.next();
                    String rotation = s.replace("%2B", "+");
                    String body = "kw=" + s + "&tbs=" + tbs + "&sign=" + Encryption.enCodeMd5("kw=" + rotation + "tbs=" + tbs + "tiebaclient!!!");
                    JSONObject post = Request.post(SIGN_URL, body);
                    if ("0".equals(post.getString("error_code"))) {
                        iterator.remove();
                        success.add(rotation);
                        LOGGER.info(rotation + ": " + "签到成功");
                    } else {
                        LOGGER.warn(rotation + ": " + "签到失败");
                    }
                }
                if (success.size() != followNum) {
                    // 为防止短时间内多次请求接口，触发风控，设置每一轮签到完等待 5 分钟
                    Thread.sleep(1000 * 60 * 5);
                    /**
                     * 重新获取 tbs
                     * 尝试解决以前第 1 次签到失败，剩余 4 次循环都会失败的错误。
                     */
                    getTbs();
                }
                flag--;
            }
        } catch (Exception e) {
            LOGGER.error("签到部分出现错误 -- " + e);
        }
    }

    /**
     * get方法
     *
     * @param client HttpClient
     * @param uri    要请求的地址
     * @return HttpResponse
     */
    public HttpResponse doGet(HttpClient client, URI uri) {
        try {
            HttpResponse response = client.execute(new HttpGet(uri));
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "GET失败：" + uri);
            }
            return response;
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
        return null;
    }

    /**
     * post方法
     *
     * @param client  HttpClient
     * @param uri     要请求的地址
     * @param postMap 要发送的数据
     * @return HttpResponse
     */
    public HttpResponse doPost(HttpClient client, URI uri, Map<String, Object> postMap) {
        HttpPost httpPost = new HttpPost(uri);
        StringEntity entityBody = new StringEntity(JSON.toJSONString(postMap), ContentType.APPLICATION_JSON);
        httpPost.setEntity(entityBody);
        try {
            HttpResponse response = client.execute(httpPost);
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new HttpResponseException(response.getStatusLine().getStatusCode(), "POST失败：" + uri);
            }
            return response;
        } catch (Exception e) {
            LOGGER.error(e.toString());
        }
        return null;
    }

    /**
     * 发送运行结果到微信，通过 server 酱
     *
     * @param pushInfo 推送渠道:推送所需信息
     * @author srcrs
     * @Time 2020-10-31
     *
     * 发送运行结果到Telegram、企业微信
     * @author Lava-Swimmer
     * @Time 2021-03-21
     */
    public void send(String pushInfo) {
        // 将要推送的数据
        String text = "共有：" + followNum + " - ";
        text += "成功：" + success.size() + " 失败：" + (followNum - success.size());
        String desp = "TiebaSignIn 运行结果：\n\n" + text;

        // 推送的方式和所需信息
        String pushType = pushInfo.split("=")[0];
        String pushKey = pushInfo.split("=")[1];

        HttpClient httpClient = HttpClients.createDefault();

        switch (pushType) {
            case "pushplus": {
                // pushplus
                try {
                    URIBuilder builder = new URIBuilder("http://www.pushplus.plus/send/"+pushKey);
                    builder.addParameter("title", text);
                    builder.addParameter("content", desp);

                    HttpResponse response = doGet(httpClient, builder.build());
                    String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Map<String, String> jsonMap = JSON.parseObject(responseJson, new TypeReference<Map<String, String>>() {
                    });
                    if (Integer.parseInt(jsonMap.get("code")) != 200) {
                        throw new HttpException("code:" + jsonMap.get("code"));
                    }
                    LOGGER.info("pushplus 推送成功");
                } catch (Exception e) {
                    LOGGER.error("pushplus 推送失败：" + e.toString());
                }
                break;
            }
            case "ft": {
                // 方糖
                try {
                    URIBuilder builder = new URIBuilder("https://sc.ftqq.com/" + pushKey + ".send");
                    builder.addParameter("text", text);
                    builder.addParameter("desp", desp);

                    HttpResponse response = doGet(httpClient, builder.build());
                    String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Map<String, String> jsonMap = JSON.parseObject(responseJson, new TypeReference<Map<String, String>>() {
                    });
                    if (Integer.parseInt(jsonMap.get("errno")) != 0) {
                        throw new HttpException("errno:" + jsonMap.get("errno"));
                    }
                    LOGGER.info("方糖推送正常");
                } catch (Exception e) {
                    LOGGER.error("方糖推送失败：" + e.toString());
                }
                break;
            }
            case "tg": {
                // Telegram
                String[] splitResults = pushKey.split(",");
                String chatID = splitResults[0];
                String botToken = splitResults[1];

                try {
                    URIBuilder builder = new URIBuilder("https://api.telegram.org/bot" + botToken + "/sendMessage");

                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("chat_id", chatID);
                    postMap.put("text", desp);

                    HttpResponse response = doPost(httpClient, builder.build(), postMap);
                    String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Map<String, Object> jsonMap = JSON.parseObject(responseJson, new TypeReference<Map<String, Object>>() {
                    });
                    if (!jsonMap.get("ok").toString().equals("true")) {
                        throw new HttpException("ok:" + jsonMap.get("ok"));
                    }
                    LOGGER.info("Telegram推送正常");
                } catch (Exception e) {
                    LOGGER.error("Telegram推送失败：" + e);
                }
                break;
            }
            case "qywx": {
                // 企业微信
                String[] splitResults = pushKey.split(",");
                String corpID = splitResults[0];
                String corpSecret = splitResults[1];
                String toUser = splitResults[2];
                String agentID = splitResults[3];

                // 获取access_token
                String accessToken = "";
                try {
                    URIBuilder builder = new URIBuilder("https://qyapi.weixin.qq.com/cgi-bin/gettoken");
                    builder.addParameter("corpid", corpID);
                    builder.addParameter("corpsecret", corpSecret);

                    HttpResponse response = doGet(httpClient, builder.build());
                    String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Map<String, String> jsonMap = JSON.parseObject(responseJson, new TypeReference<Map<String, String>>() {
                    });
                    if (Integer.parseInt(jsonMap.get("errcode")) != 0) {
                        throw new HttpException("errcode:" + jsonMap.get("errcode"));
                    }
                    accessToken = jsonMap.get("access_token");
                    LOGGER.info("企业微信获取access_token正常");
                } catch (Exception e) {
                    LOGGER.error("企业微信获取access_token失败：" + e.toString());
                }

                // 发送企业微信应用消息
                try {
                    URIBuilder builder = new URIBuilder("https://qyapi.weixin.qq.com/cgi-bin/message/send");
                    builder.addParameter("access_token", accessToken);

                    Map<String, Object> postMap = new HashMap<>();
                    postMap.put("msgtype", "text");
                    postMap.put("touser", toUser);
                    postMap.put("agentid", agentID);
                    Map<String, String> textMap = new HashMap<>();
                    textMap.put("content", desp);
                    postMap.put("text", textMap);

                    HttpResponse response = doPost(httpClient, builder.build(), postMap);
                    String responseJson = EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
                    Map<String, String> jsonMap = JSON.parseObject(responseJson, new TypeReference<Map<String, String>>() {
                    });
                    if (Integer.parseInt(jsonMap.get("errcode")) != 0) {
                        throw new HttpException("errcode:" + jsonMap.get("errcode"));
                    }
                    LOGGER.info("企业微信推送应用消息正常");
                } catch (Exception e) {
                    LOGGER.error("企业微信推送应用消息失败：" + e.toString());
                }
                break;
            }
            default:
                LOGGER.error("未知的推送服务类型：" + pushType);
                break;
        }
    }
}
