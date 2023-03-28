(ns re-pipe.routes.services
  (:require
    [reitit.swagger :as swagger]
    [reitit.swagger-ui :as swagger-ui]
    [reitit.ring.coercion :as coercion]
    [reitit.coercion.spec :as spec-coercion]
    [reitit.ring.middleware.muuntaja :as muuntaja]
    [reitit.ring.middleware.multipart :as multipart]
    [reitit.ring.middleware.parameters :as parameters]
    [re-pipe.middleware.formats :as formats]
    [ring.util.http-response :refer :all]
    [re-pipe.auth :refer :all]
    [clojure.java.io :as io]
    [puget.printer :refer [cprint]])


  (:import [clojure.lang ExceptionInfo]))

(defn if-authorized [data then]
  (if (-> data :session :identity)
    then
    (unauthorized {:message "not authenticated or authorized!"})))

(defn service-routes []
  ["/api"
   {:coercion   spec-coercion/coercion
    :muuntaja   formats/instance
    :swagger    {:id ::api}
    :middleware [;; query-params & form-params
                 parameters/parameters-middleware
                 ;; content-negotiation
                 muuntaja/format-negotiate-middleware
                 ;; encoding response body
                 muuntaja/format-response-middleware
                 ;; exception handling
                 coercion/coerce-exceptions-middleware
                 ;; decoding request body
                 muuntaja/format-request-middleware
                 ;; coercing response bodys
                 coercion/coerce-response-middleware
                 ;; coercing request parameters
                 coercion/coerce-request-middleware
                 ;; multipart
                 multipart/multipart-middleware]}

   ;; swagger documentation
   ["" {:no-doc  true
        :swagger {:info {:title       "my-api"
                         :description "https://cljdoc.org/d/metosin/reitit"}}}

    ["/swagger.json"
     {:get (swagger/create-swagger-handler)}]

    ["/api-docs/*"
     {:get (swagger-ui/create-swagger-ui-handler
             {:url    "/api/swagger.json"
              :config {:validator-url nil}})}]]

   ["/ping"
    {:get (constantly (ok {:message "pong"}))}]

   ["/register"
    {:post {:summary    "create a new user" ; {:name "" :email "" :pw "" :pw-repeat ""}
            :parameters {:body {:name string? :email string? :pw string? :pw-repeat string?}}
            :responses  {200 {:body {:email string? :message string?}} ; ok
                         400 {:body {:message string?}} ; bad request
                         409 {:body {:message string?}}} ; conflict
            :handler    (fn [{{:keys [name email pw pw-repeat]} :body-params}]
                          (println "going to register user, email: " email ", name: " name ", pw: " pw ", pw-repeat:" pw-repeat)
                          (if-not (= pw pw-repeat)
                            (bad-request {:message "Password and confirm do not match."})
                            (try
                              (create-user! name email pw)
                              (println "registered user: " email)
                              (ok {:email email :message "User registered. Please login."})
                              (catch ExceptionInfo e
                                (if (= (:error (ex-data e))
                                       :user-exists)
                                  (conflict {:message "Failed. User already exists."})
                                  (throw e))))))}}]

   ["/login"
    {:post {:summary    "user authentictes with email and password. auth-type = :auth (EDN) or \"auth\" (json)"
            :parameters {:body {:user string? :pw string?}}
            :handler    (fn [{session :session {:keys [user pw]} :body-params :as data}]
                          (println user pw)
                          ;(cprint data)
                          (if (authenticate-user user pw)
                            (->
                              (ok {:identity user})
                              (assoc :session (assoc session :identity user)))
                            ;(ok {:identity user :session session})
                            (unauthorized
                              {:message "Incorrect login or password."})))}}]


   ["/logout"
    {:post {:handler (fn [data] ; {{:keys [user pw]} :body-params}
                       ;(println data)
                       (if-authorized
                         data
                         (-> (ok {:message "logged out successfully"})
                             (assoc :session nil)))
                       #_{:status 200
                          :body   (str "user logged out") #_{:login (str "user: " user + ", pw: " pw)}})}}]

   ["/authorized"
    {:post {:handler (fn [{session :session :as data}]
                       ;(cprint data)
                       ;(println)
                       (println "session:")
                       (cprint session)
                       (if-authorized data
                                      (ok {:answer "something authorized"}))
                       #_(if (:identity session)
                           (ok {:answer "something authorized"})
                           (unauthorized
                             {:message "NOT AUTHORIZED!"})))}}]



   ["/math"
    {:swagger {:tags ["math"]}}

    ["/plus"
     {:get  {:summary    "plus with spec query parameters"
             :parameters {:query {:x int?, :y int?}}
             :responses  {200 {:body {:total pos-int?}}}
             :handler    (fn [{{{:keys [x y]} :query} :parameters}]
                           {:status 200
                            :body   {:total (+ x y)}})}
      :post {:summary    "plus with spec body parameters"
             :parameters {:body {:x int?, :y int?}}
             :responses  {200 {:body {:total pos-int?}}}
             :handler    (fn [{{{:keys [x y]} :body} :parameters}]
                           {:status 200
                            :body   {:total (+ x y)}})}}]]

   ["/files"
    {:swagger {:tags ["files"]}}

    ["/upload"
     {:post {:summary    "upload a file"
             :parameters {:multipart {:file multipart/temp-file-part}}
             :responses  {200 {:body {:name string?, :size int?}}}
             :handler    (fn [{{{:keys [file]} :multipart} :parameters}]
                           {:status 200
                            :body   {:name (:filename file)
                                     :size (:size file)}})}}]

    ["/download"
     {:get {:summary "downloads a file"
            :swagger {:produces ["image/png"]}
            :handler (fn [_]
                       {:status  200
                        :headers {"Content-Type" "image/png"}
                        :body    (-> "public/img/warning_clojure.png"
                                     (io/resource)
                                     (io/input-stream))})}}]]])
