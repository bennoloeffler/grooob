(ns re-pipe.routes.home
  (:require
    [re-pipe.layout :as layout]
    [clojure.java.io :as io]
    [re-pipe.middleware :as middleware]
    [ring.util.response]
    [ring.util.http-response :as response]
    [re-pipe.google-login :as google]))


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
   ["/docs" {:get (fn [_]
                    (-> (response/ok (-> "docs/docs.md" io/resource slurp))
                        (response/header "Content-Type" "text/plain; charset=utf-8")))}]])

