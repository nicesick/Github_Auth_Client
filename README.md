

# Github_Auth_Client

This Project is for making Github_Auth_Client for practicing Auth2.0



## Environment
1. Spring-Boot 2.4.1
2. Github OAuth
3. Gson(com.google.code.gson)



## How to Execute
* In Local Environment
  
    * Before executing Application, You have to fill github client information in application.properties file
    
    ```properties
    # You can receive client_id, client_secret in Github > Settings > Developer settings > OAuth Apps
    
    config.github.client.id = #{Your client id}
    config.github.client.secret = #{Your client secret}
    config.github.client.scope = user%20public_repo
    config.github.client.redirect_uri= http://localhost:8080/oauth
    
    config.github.authorize_url= https://github.com/login/oauth/authorize
    config.github.token_url= https://github.com/login/oauth/access_token
    config.github.api_url_base= https://api.github.com
    ```
    
    * And you can execute application
    
    ```powershell
    >  ./mvnw spring-boot:run
    ```



## Github OAuth Process

1. Request Authorize
   * response_type
   * client_id
   * redirect_uri
   * scope
   * state
2. Request Access Token
   * grant_type
   * client_id
   * client_secret
   * redirect_uri
   * code
3. Reqeust API
   * Authorization
   * Request URL



## Issue
1. CORS(Cross-Origin Resource Sharing) Problem
    * At First, I try to use AUTH in Template. But, There was CORS Problem
    ```javascript
    /**
     * In part of CORS Problem
     */
    function requestGithubToken() {
       headers = {
            'Accept'        : 'application/vnd.github.v3+json, application/json',
        }
        
        params = {
            'grant_type'    : 'authorization_code',
            'client_id'     : CLIENT_ID,
            'client_secret' : CLIENT_SECRET,
            'redirect_uri'  : REDIRECT_URI,
            'code'          : code
        };
        
        /**
         * Even i try to add 'Access-Control-Allow-Origin' in headers,
         * Even i try to use cors-anywhere Proxy Site in front of url,
         * 
         * There still was CORS Problem
         */
        $.ajax({
            url     : TOKEN_URL,
            method  : 'POST',
            headers : headers,
            data    : params,
            
            success : function(data) {
                /*
                 * logics ...
                 */
            },
            
            fail    : function(data) {
                /*
                 * logics ...
                 */
            }
       });
   }
   ```
   
   * So, I moved the logics in Spring-Boot Server
   ```javascript
   /**
    * In html page
    */
   /**
    * accessToken Request
    */
   function requestAccessToken() {
       $('#auth-process-msg').text('Get access token processing...');
   
       $.ajax({
           url     : HOST_URL + '/oauth/accessToken',
           method  : 'GET',
   
           success : function(data) {
               $('#auth-process-msg').text('Get access token success');
   
               console.log('accessToken success');
               console.log(data);
   
               requestSession();
           },
   
           fail    : function(data) {
               console.log(data);
           }
       });
   }
   ```
   
   ```java
   /**
    * In Spring-Boot Server
    */
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
           System.out.println("getGithubAccessToken : result     = " + result);
   
           if (!"".equals(result)) {
               JsonElement resultJson      = JsonParser.parseString(result);
               System.out.println("getGithubAccessToken : resultJson = " + resultJson);
   
               for (String jsonKey : resultJson.getAsJsonObject().keySet()) {
                   session.setAttribute(jsonKey, resultJson.getAsJsonObject().get(jsonKey).getAsString());
               }
           }
       }
   
       return null;
   }
   ```

2. Encoding data

   * When you send github authorization requesting data,
   * Because of space, There was error

   ```java
   /**
    * In Spring-Boot Server
    *
    * When I send github authorization link to template,
    * There was 400 response code(400 : Bad Request) Error
    */
   @RequestMapping("/authorize")
   @ResponseBody
   public String getGithubAuthorize(HttpSession session) {
       Map<String, String> params  = new HashMap<>();
       String              state   = String.valueOf(new SecureRandom().nextInt(1000000000));
   
       params.put("response_type"  , "code");
       params.put("client_id"      , CLIENT_ID);
       params.put("redirect_uri"   , REDIRECT_URI);
       params.put("scope"          , SCOPE);				// Error Point
       params.put("state"          , state);
   
       session.setAttribute("state", state);
       session.setMaxInactiveInterval(10000);
   
       return AUTHORIZE_URL + "?" + StreamUtils.getRequestGetParams(params);
   }
   ```

   * In application.properties file, I fix the scope config like this

   ```properties
   config.github.client.id = #{Your client id}
   config.github.client.secret = #{Your client secret}
   
   # At first,
   # config.github.client.scope = user public_repo
   # I add %20 between 'user' and 'public_repo'
   
   config.github.client.scope = user%20public_repo
   config.github.client.redirect_uri= http://localhost:8080/oauth
   
   config.github.authorize_url= https://github.com/login/oauth/authorize
   config.github.token_url= https://github.com/login/oauth/access_token
   config.github.api_url_base= https://api.github.com
   ```



## References

* [oauth.com](https://www.oauth.com/)
* [Github Docs](https://docs.github.com/en)