package com.jihun.study.githubOAuth20.controller;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jihun.study.githubOAuth20.utils.StreamUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpSession;
import java.security.SecureRandom;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/oauth")
public class OAuthController {
    private final Logger logger = LoggerFactory.getLogger(this.getClass().getSimpleName());

    private String CLIENT_ID        = "";
    private String CLIENT_SECRET    = "";
    private String SCOPE            = "";
    private String REDIRECT_URI     = "";

    private String AUTHORIZE_URL    = "";
    private String TOKEN_URL        = "";
    private String API_URL_BASE     = "";

    @Autowired
    OAuthController(Environment environment) {
        CLIENT_ID       = environment.getProperty("config.github.client.id");
        CLIENT_SECRET   = environment.getProperty("config.github.client.secret");
        SCOPE           = environment.getProperty("config.github.client.scope");
        REDIRECT_URI    = environment.getProperty("config.github.client.redirect_uri");

        AUTHORIZE_URL   = environment.getProperty("config.github.authorize_url");
        TOKEN_URL       = environment.getProperty("config.github.token_url");
        API_URL_BASE    = environment.getProperty("config.github.api_url_base");
    }

    @RequestMapping("")
    public String getOAuth(HttpSession session, String code, String state) {
        String template = "/templates/github_OAuth2.0_page.html";

        logger.info("getOAuth : code  = " + code);
        logger.info("getOAuth : state = " + state);

        if (state != null && session.getAttribute("state") != null) {
            String sessionState = (String) session.getAttribute("state");
            logger.info("getOAuth : sessionState = " + sessionState);

            if (sessionState.equals(state)) {
                session.setAttribute("code", code);
            } else {
                session.invalidate();
            }
        } else {
            session.invalidate();
        }

        return template;
    }

    @RequestMapping("/authorize")
    @ResponseBody
    public String getGithubAuthorize(HttpSession session) {
        Map<String, String> params  = new HashMap<>();
        String              state   = String.valueOf(new SecureRandom().nextInt(1000000000));

        params.put("response_type"  , "code");
        params.put("client_id"      , CLIENT_ID);
        params.put("redirect_uri"   , REDIRECT_URI);
        params.put("scope"          , SCOPE);
        params.put("state"          , state);

        session.setAttribute("state", state);
        session.setMaxInactiveInterval(10000);

        return AUTHORIZE_URL + "?" + StreamUtils.getRequestGetParams(params);
    }

    @RequestMapping("/session")
    @ResponseBody
    public Map<String, String> getSession(HttpSession session) {
        Map<String, String> result = new HashMap<>();

        result.put("code"           , (String) session.getAttribute("code"));
        result.put("state"          , (String) session.getAttribute("state"));
        result.put("access_token"   , (String) session.getAttribute("access_token"));
        result.put("token_type"     , (String) session.getAttribute("token_type"));
        result.put("scope"          , (String) session.getAttribute("scope"));

        return result;
    }

    @RequestMapping("/accessToken")
    @ResponseBody
    public String getGithubAccessToken(HttpSession session) {
        String code     = (String) session.getAttribute("code");
        String state    = (String) session.getAttribute("state");

        logger.info("getGithubAccessToken : code  = " + code);
        logger.info("getGithubAccessToken : state = " + state);

        if (code != null && state != null) {
            Map<String, String> headers = new HashMap<>();
            Map<String, String> params  = new HashMap<>();

            headers.put("Accept"        , "application/vnd.github.v3+json, application/json");

            params.put("grant_type"     , "authorization_code");
            params.put("client_id"      , CLIENT_ID);
            params.put("client_secret"  , CLIENT_SECRET);
            params.put("redirect_uri"   , REDIRECT_URI);
            params.put("code"           , code);

            String result = StreamUtils.getStream(TOKEN_URL, StreamUtils.METHOD_POST, headers, params);
            logger.info("getGithubAccessToken : result     = " + result);

            if (!"".equals(result)) {
                JsonElement resultJson      = JsonParser.parseString(result);

                for (String jsonKey : resultJson.getAsJsonObject().keySet()) {
                    session.setAttribute(jsonKey, resultJson.getAsJsonObject().get(jsonKey).getAsString());
                }
            }
        }

        return null;
    }

    @RequestMapping("/api")
    @ResponseBody
    public String getGithubApi(HttpSession session) {
        String code         = (String) session.getAttribute("code");
        String state        = (String) session.getAttribute("state");
        String access_token = (String) session.getAttribute("access_token");
        String token_type   = (String) session.getAttribute("token_type");
        String scope        = (String) session.getAttribute("scope");

        logger.info("getGithubApi : code         = " + code);
        logger.info("getGithubApi : state        = " + state);
        logger.info("getGithubApi : access_token = " + access_token);
        logger.info("getGithubApi : token_type   = " + token_type);
        logger.info("getGithubApi : scope        = " + scope);

        if (code != null && state != null && access_token != null && token_type != null && scope != null) {
            Map<String, String> headers = new HashMap<>();
            Map<String, String> params  = new HashMap<>();

            headers.put("Accept"        , "application/vnd.github.v3+json, application/json");
            headers.put("Authorization" , token_type + " " + access_token);

            params.put("sort"           , "created");
            params.put("direction"      , "desc");

            String result = StreamUtils.getStream(API_URL_BASE + "/user/repos", StreamUtils.METHOD_GET, headers, params);
            return result;
        }

        return null;
    }
}
