(ns grooob.routes.home
  (:require
    [grooob.layout :as layout]
    [clojure.java.io :as io]
    [grooob.middleware :as middleware]
    [ring.util.response]
    [ring.util.http-response :as response]
    [grooob.google-login :as google]))


(defn home-page [request]
  (layout/render request "home.html"))

(defn flash-message [req msg]
  (assoc-in req [:flash :layout/message] msg))

(defn home-routes []
  [""
   {:middleware [middleware/wrap-csrf
                 middleware/wrap-formats]}
   ["/" {:get home-page}]
   ["/oauth/google/done" {:get (google/callback-from-google-login)}]
   ["/oauth/facebook/done" {:get (google/callback-from-facebook-login)}]
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

