# grooob.com

## Vision
Planning as rough as possible.
Less Details. Less data. Less time.
Together simultaneously as Team.
View everything in Realtime.

## TODOs / Features / Releases
- save auth token in browser?
- flash?
- checkbox for terms and conditions
- validation locally with local error messages
- page for terms and conditions
- terms and conditions - validate
- page for forget password?
- state "logged out" - and different page
- errors and validation with register and login


- site admin functions? Just hardcode benno.loeffler@gmx.de
- need a company admin
- try to deploy to heroku / replit / or ?
- a data model for user management, company management
- a data model for domain-model: project, task, capacity, ...
- a functional core for all business logic
- authorization and admin role
- invitation
- user administration (pending, guest, ...)
- resend password

## technical decisions
- clojure
- server with datahike
- Client: re-frame
- model, logic and spec in cljc in order to use at client and server
- EITHER: drawing in a canvas: reactive client with some rx-tool
- OR: drawing svg in re-frame


## development mode
```
npm i react
npm i react-dom
npm i shadow-cljs --save-dev
npm install node-sass --save-dev
npm install bulma --save-dev
```

package.json should look like this
```
{
  "devDependencies": {
    "bulma": "^0.9.4",
    "node-sass": "^8.0.0",
    "shadow-cljs": "^2.16.5"
  },
  "dependencies": {
    "react": "^17.0.2",
    "react-dom": "^17.0.2"
  },
  "scripts": {
    "css-build": "node-sass --omit-source-map-url resources/scss/screen.scss resources/public/css/screen.css",
    "css-watch": "npm run css-build -- --watch",
    "start": "npm run css-watch"
  }
}
```

## Running

watch and compile sass
```
npm run start
```

watch and compile shadow-cljs
```
shadow-cljs watch app
```

run server in repl
```
(start)
(stop)
(restart)
```

run client in browser
```
localhost:3000
```

connect vscode/calva to cljs repl in browser
```
click the repl symbol in the status line. Then choose:
1 connect to a running repl in your project
2 shadow-cljs
3 :app
```

test api in swagger
```
localhost:3000/swagger-ui
```

## google oath2 login

1. I decided to go with reitit-oauth2, based on https://lambdaisland.com/episodes/passwordless-authentication-ring-oauth2
1. I got the data from google
2. thought about the endpoints in my app
```
(defn oauth-config []
  {:google
   {:authorize-uri    "https://accounts.google.com/o/oauth2/v2/auth"
    :access-token-uri "https://www.googleapis.com/oauth2/v4/token"
    :client-id        "abc"
    :client-secret    "def"
    :scopes           ["email"]
    :launch-uri       "/login-with-google"     ; starts the process
    :redirect-uri     "/oauth/google/callback" ; told by google
    :landing-uri      "/oauth/google/done"}})  ; hit, when google finished 
```

4. so the middleware on the serverside needs to be changed.
```
(ns re-pipe.middleware
  (:require
    [re-pipe.env :refer [defaults]]
    [clojure.tools.logging :as log]
    [re-pipe.layout :refer [error-page]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [re-pipe.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [re-pipe.config :refer [env]]
      ;;
      ;; ----------------------------------- START CHANGE -------------------
      ;;
    [ring.middleware.oauth2 :refer [wrap-oauth2]]
      ;;
      ;;  ----------------------------------- END -------------------
      ;;
    [ring.middleware.flash :refer [wrap-flash]]
    [ring.adapter.undertow.middleware.session :refer [wrap-session]]
    [ring.middleware.defaults :refer [site-defaults wrap-defaults]]
    [puget.printer :refer [cprint]]
    [re-pipe.google-login :as google]))

(defn wrap-internal-error [handler]
  (fn [req]
    (try
      (handler req)
      (catch Throwable t
        (log/error t (.getMessage t))
        (error-page {:status 500
                     :title "Something very bad has happened!"
                     :message "We've dispatched a team of highly trained gnomes to take care of the problem."})))))

(defn wrap-csrf [handler]
  (wrap-anti-forgery
    handler
    {:error-response
     (error-page
       {:status 403
        :title "Invalid anti-forgery token"})}))

(defn wrap-formats [handler]
  (let [wrapped (-> handler wrap-params (wrap-format formats/instance))]
    (fn [request]
      ;; disable wrap-formats for websockets
      ;; since they're not compatible with this middleware
      ((if (:websocket? request) handler wrapped) request))))

;; middleware which prints the request
(defn wrap-print-request [handler]
  (fn [request]
    (cprint request)
    (handler request)))


(defn wrap-base [handler]
  (-> ((:middleware defaults) handler)
      ;wrap-print-request
      wrap-flash
      ;;
      ;; ----------------------------------- START CHANGE -------------------
      ;;
      (wrap-oauth2 (google/oauth-config))
      (wrap-session {:cookie-attrs {:http-only true}})

      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in [:session :cookie-attrs :same-site] :lax) ;; in order to make wrap-oauth2 work.
            ;;
            ;;  ----------------------------------- END -------------------
            ;;
            (dissoc :session)))
      wrap-internal-error))

```

5. this button starts the process by triggering /login-with-google and this starts google login with the middleware 

```
(defn login-with-google-button []
  [:a.button.mr-1
   {:href "/login-with-google"}
   [:span.icon.is-large>i (cs "fas" "fa-1x" "fa-brands" "fa-google")]
   [:span "login with google"]])
```
6. this is the router on server side
```
(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/oauth/google/done" {:get (google/callback-from-google-login)}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])
```
7. the session is changed, after the router get's /oauth/google/done  
Until here, everything works fine
The redirect points to a client route...  
***TODO remember the user/token***
```
(defn callback-from-google-login []
  (fn [req]
    ;(cprint req)
    (let [token        (get-in req [:oauth2/access-tokens :google :token])
          email        (google-fetch-email token)
          next-session (-> (assoc (:sesson req) :identity email)
                           (with-meta {:recreate true}))]
      (-> (redirect "/#/google-login") ; not home, to set user right...
          (assoc :session next-session)))))
```
8. here is the route
```
(def router
  (reitit/router
    [["/" {:name        :home
           :view        #'home-page
           :controllers [{:start (fn [_] (rf/dispatch [:page/init-home]))}]}]
     ["/about" {:name :about
                :view #'about-page}]
     ["/logout" {:name :logout
                 :view #'logout-page}]
     ["/register" {:name :register
                   :view #'register-page}]
     ["/ex" {:name :experiments
             :view #'ex/experiments}]

     ["/google-login" {:name        :google-login
                       :view        #'home-page-from-google
                       :controllers [{:start (fn [req] (rf/dispatch [:login-google req]))}]}]]))

```
9. there is a page #'home-page-from-google
```
(defn home-page-from-google []
      [:<>
       [:div "welcome back from google login..."]
       [:br][:br]
       [home-page]])
```
10. the controller from 8. triggers (rf/dispatch [:login-google req] 
```
(rf/reg-event-fx
  :login-google
  (fn [cofx [_ data]]
    {:http-xhrio {:method          :post
                  :params          data
                  :uri             "/api/login-google"
                  :format          (ajax/json-request-format)
                  :response-format (ajax/transit-response-format {:keywords? true})
                  :on-success      [:login-success]
                  :on-failure      [:login-failure]}}))

```
11. the code behind the api looks like this  
it just gives back the :identity  
***TODO check the user/token***
```
   ["/login-google"
    {:post {:summary    "user authentictes with :session :identity"
            :handler    (fn [{session :session oauth2 :oauth2/access-tokens :as data}]
                          ;(println session)
                          ;(cprint oauth2)
                          (if (-> data :session :identity)
                            (->
                              (ok {:identity (-> data :session :identity)})
                              (assoc :session (assoc session :identity (-> data :session :identity))))
                            ;(ok {:identity user :session session})
                            (unauthorized
                              {:message "Incorrect login or password."})))}}]

```

12. and in the server, I use that Identity
```
(rf/reg-event-db
  :login-success
  (fn [db [_ data]]
    (println "login success: " data)
    (-> db
        (assoc :user data)
        (dissoc :user-tmp))))
```

