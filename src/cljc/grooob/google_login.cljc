(ns grooob.google-login
  (:require [clj-http.client :as http]
            [ring.util.response :refer [redirect]]
            [puget.printer :refer [cprint]]
            [grooob.db.core :as db]))


; get credentials: https://console.cloud.google.com/apis/credentials?hl=de&project=grooob&supportedpurview=project
; login: loeffler@v...

;; example from here
;; https://lambdaisland.com/episodes/passwordless-authentication-ring-oauth2

;; https://github.com/weavejester/ring-oauth2/issues/43

(def google-user-info-url "https://www.googleapis.com/oauth2/v1/userinfo")

(defn google-fetch-email [token]
  (-> (http/get google-user-info-url {:query-params {:access_token token} :as :json})
      (get-in [:body :email])))

(defn callback-from-google-login []
  (fn [req]
    ;(println "AFTER google got request and called back to server:")
    ;(cprint req)
    (let [token        (get-in req [:oauth2/access-tokens :google :token])
          email        (google-fetch-email token)
          next-session (-> (assoc (:sesson req) :identity email)
                           (with-meta {:recreate true}))]
      (db/check-and-add-user-google!
        :user.status/pending
        email
        (:oauth2/access-tokens req))
      (-> (redirect "/#/google-login") ; not home, to set user right...
          (assoc :session next-session)))))

#_(defn check-google-from-client-login [req]
    ;(println "AFTER google got request and called back to server:")
    ;(cprint req)
    (let [email (-> req :session :identity)]
      (db/check-user-google email)))


; see my login information

; salesforce and facebook examples
;https://github.com/DerGuteMoritz/clj-oauth2

(defn oauth-config []

  {:google   {:authorize-uri    "https://accounts.google.com/o/oauth2/v2/auth"
              :access-token-uri "https://www.googleapis.com/oauth2/v4/token"
              :client-id        "607397261019-4fkbc25chgnohnlddjjql3l4vf7bu5vf.apps.googleusercontent.com"
              :client-secret    "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"
              :scopes           ["email"]
              :launch-uri       "/login-with-google"
              :redirect-uri     "/oauth/google/callback"
              :landing-uri      "/oauth/google/done"}
   :facebook {:authorize-uri    "https://www.facebook.com/dialog/oauth"
              :access-token-uri "https://graph.facebook.com/oauth/access_token"
              :client-id        "1548946789180539"
              :client-secret    "e1ee9bcc2a1a4927f7ff4a1d7fc72283"
              :scope            ["email"]
              :launch-uri       "/login-with-facebook"
              :redirect-uri     "/oauth/facebook/callback"
              :landing-uri      "/oauth/facebook/done"}})

;; facebook
;  app-id 1548946789180539
; app-secret e1ee9bcc2a1a4927f7ff4a1d7fc72283
; client token e1ee9bcc2a1a4927f7ff4a1d7fc72283

; TODO s
; https://developers.facebook.com/docs/facebook-login/guides
; specific
; https://developers.facebook.com/apps/1548946789180539/go_live/
; callback URI
; https://developers.facebook.com/apps/1548946789180539/fb-login/settings/

(def facebook-user-info-url "https://graph.facebook.com/v12.0/me")
(def fb-test-token "EAAWAwkclJHsBO28FExj7S2i2998WZBBh72FdIBMV5V9W3zJkKE76F2Kh4sLNO3hwnJsEwOCB5sGuJjXp9ekPZAYwOcrWu83amjnrn7e9BGuLuggKEt3ZAMQa2GYEC0zpqA7Cgoe8wN886YKGLyCCNEveNXItxACV1b98GFbaTXntmBMZBbuGuqZCOQWD7xNjlasAlfNnoAEFy9CkpfyQ66vfhiDz8ccgZClcT8v9dD6tMBpGhki05A")

(comment
  (http/get facebook-user-info-url {:query-params {:access_token fb-test-token} :as :json}))

(defn facebook-fetch-email [token]
  (-> (http/get facebook-user-info-url {:query-params {:access_token token} :as :json})
      (get-in [:body :email])))

(defn callback-from-facebook-login []
  (fn [req]
    ; in req we have:
    ;; :oauth2/access-tokens {:facebook {:expires #<org.joda.time.DateTime@3144b327 2023-12-06T17:14:10.339Z>,
    ;;                                   :extra-data {:token_type "bearer"},
    ;;                                   :token "EAAWAwkclJHsBO28FExj7S2i2998WZBBh72FdIBMV5V9W3zJkKE76F2Kh4sLNO3hwnJsEwOCB5sGuJjXp9ekPZAYwOcrWu83amjnrn7e9BGuLuggKEt3ZAMQa2GYEC0zpqA7Cgoe8wN886YKGLyCCNEveNXItxACV1b98GFbaTXntmBMZBbuGuqZCOQWD7xNjlasAlfNnoAEFy9CkpfyQ66vfhiDz8ccgZClcT8v9dD6tMBpGhki05A"}},
    ;; BUT NO EMAIL
    ;(println "AFTER facebook got request and called back to server:")
    (cprint req)
    (let [r            req
          token        (get-in r [:oauth2/access-tokens :facebook :token])
          email        (facebook-fetch-email token)
          next-session (-> (assoc (:sesson req) :identity email)
                           (with-meta {:recreate true}))]
      (when-not email (throw (ex-info "got no email info from facebook!" {:email email})))
      (db/check-and-add-user-facebook!
        :user.status/pending
        email
        (:oauth2/access-tokens req))
      (-> (redirect "/#/facebook-login") ; not home, to set user right...
          (assoc :session next-session)))))


