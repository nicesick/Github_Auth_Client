package com.jihun.study.githubOAuth20.controller;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.jihun.study.githubOAuth20.utils.StreamUtils;
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

        System.out.println("getOAuth : code  = " + code);
        System.out.println("getOAuth : state = " + state);

        if (state != null && session.getAttribute("state") != null) {
            String sessionState = (String) session.getAttribute("state");
            System.out.println("getOAuth : sessionState = " + sessionState);

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
        result.put("accessToken"    , (String) session.getAttribute("accessToken"));
        result.put("tokenType"      , (String) session.getAttribute("tokenType"));
        result.put("scope"          , (String) session.getAttribute("scope"));

        return result;
    }

    @RequestMapping("/accessToken")
    @ResponseBody
    public String getGithubAccessToken(HttpSession session) {
        String code     = (String) session.getAttribute("code");
        String state    = (String) session.getAttribute("state");

        System.out.println("getGithubAccessToken : code  = " + code);
        System.out.println("getGithubAccessToken : state = " + state);

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
            System.out.println("getGithubAccessToken : result = " + result);

            if (!"".equals(result)) {
                JsonElement resultJson      = JsonParser.parseString(result);
                System.out.println("getGithubAccessToken : resultJson = " + resultJson);

                for (String jsonKey : resultJson.getAsJsonObject().keySet()) {
                    session.setAttribute(jsonKey, resultJson.getAsJsonObject().get(jsonKey));
                }
            }
        }

        return null;
    }
}
