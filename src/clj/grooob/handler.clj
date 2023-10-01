(ns grooob.handler
  (:require
    [grooob.middleware :as middleware]
    [grooob.layout :refer [error-page]]
    [grooob.routes.home :refer [home-routes]]
    [grooob.routes.services :refer [service-routes]]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring :as ring]
    [ring.middleware.content-type :refer [wrap-content-type]]
    [ring.middleware.webjars :refer [wrap-webjars]]
    [grooob.env :refer [defaults]]
    [mount.core :as mount]
    [ring.util.response :as response]))

(mount/defstate init-app
                :start ((or (:init defaults) (fn [])))
                :stop ((or (:stop defaults) (fn []))))

(defn- async-aware-default-handler
  ([_] nil)
  ([_ respond _] (respond nil)))


(mount/defstate app-routes
                :start
                (ring/ring-handler
                  (ring/router
                    [(home-routes)
                     (service-routes)])
                  (ring/routes
                    (swagger-ui/create-swagger-ui-handler
                      {:path   "/swagger-ui"
                       :url    "/api/swagger.json"
                       :config {:validator-url nil}})
                    (ring/create-resource-handler
                      {:path "/"})
                    (wrap-content-type
                      (wrap-webjars async-aware-default-handler))
                    (ring/create-default-handler
                      {:not-found
                       #_(response/resource-response "home.html")
                       #_(layout/render nil "home.html")
                       #_(response/redirect "/#/not-found")
                       #_(constantly (error-page {:status 404, :title "404 - Page not found"}))
                       (constantly (do #_(println (error-page {:status 404, :title "404 - Page not found"}))
                                     (error-page {:status 404, :title "404 - Page not found"})))
                       :method-not-allowed
                       (constantly (error-page {:status 405, :title "405 - Not allowed"}))
                       :not-acceptable
                       (constantly (error-page {:status 406, :title "406 - Not acceptable"}))}))))

(defn app []
  (middleware/wrap-base #'app-routes))
