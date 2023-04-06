(ns re-pipe.google-login
  (:require [clj-http.client :as http]
            [ring.util.response :refer [redirect]]
            [puget.printer :refer [cprint]]
            [re-pipe.db.core :as db]))


; get credentials: https://console.cloud.google.com/apis/credentials?hl=de&project=grooob&supportedpurview=project
; login: loeffler@v...

;; example from here
;; https://lambdaisland.com/episodes/passwordless-authentication-ring-oauth2

;; https://github.com/weavejester/ring-oauth2/issues/43

(def user-info-url "https://www.googleapis.com/oauth2/v1/userinfo")

(defn google-fetch-email [token]
  (-> (http/get user-info-url {:query-params {:access_token token} :as :json})
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


(defn check-google-from-client-login [req]
    ;(println "AFTER google got request and called back to server:")
    ;(cprint req)
    (let [email (-> req :session :identity)]
      (db/check-user-google email)))



(defn oauth-config []
  {:google
   {:authorize-uri    "https://accounts.google.com/o/oauth2/v2/auth"
    :access-token-uri "https://www.googleapis.com/oauth2/v4/token"
    :client-id        "607397261019-4fkbc25chgnohnlddjjql3l4vf7bu5vf.apps.googleusercontent.com"
    :client-secret    "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"
    :scopes           ["email"]
    :launch-uri       "/login-with-google"
    :redirect-uri     "/oauth/google/callback"
    :landing-uri      "/oauth/google/done"}})



;; data DOWNLOADED from google. DONT CHECK IN!
{:google {:client-id                   "607397261019-4fkbc25chgnohnlddjjql3l4vf7bu5vf.apps.googleusercontent.com"
          :project-id                  "grooob"
          :auth-uri                    "https://accounts.google.com/o/oauth2/auth"
          :token-uri                   "https://oauth2.googleapis.com/token"
          :auth-provider-x509-cert-url "https://www.googleapis.com/oauth2/v1/certs"
          :client-secret               "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"
          :redirect-uris               ["http://localhost:3000/oauth/google/callback"]
          :javascript-origins          ["http://localhost:3000"]}}
