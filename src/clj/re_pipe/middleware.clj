(ns re-pipe.middleware
  (:require
    [re-pipe.env :refer [defaults]]
    [clojure.tools.logging :as log]
    [re-pipe.layout :refer [error-page]]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [re-pipe.middleware.formats :as formats]
    [muuntaja.middleware :refer [wrap-format wrap-params]]
    [re-pipe.config :refer [env]]
    [ring.middleware.oauth2 :refer [wrap-oauth2]]
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
      (wrap-oauth2 (google/oauth-config))
      (wrap-session {:cookie-attrs {:http-only true}})

      (wrap-defaults
        (-> site-defaults
            (assoc-in [:security :anti-forgery] false)
            (assoc-in [:session :cookie-attrs :same-site] :lax) ;; in order to make wrap-oauth2 work.
            (dissoc :session)))
      wrap-internal-error))
