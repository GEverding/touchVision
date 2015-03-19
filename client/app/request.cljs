(ns app.request
  ;; (:use plumbing.core)
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [ajax.core :refer [GET POST PUT DELETE]]
            [plumbing.core :refer-macros [?>]]
            [cljs.core.async :as async :refer [put! chan]]))

(def default-params {:finally identity
                     :format :json
                     :response-format :json
                     :keywords? true })

(defn build-request
  "Build Ajax by extending default-params"
  [ch data]
  (-> default-params
      (assoc :handler (fn [res] (put! ch {:status 200
                                         :body res})))
      (assoc :error-handler (fn [res] (put! ch {:status (:status res)
                                               :error (:response res)})))
      (?> #(not (empty? %)) (assoc :params data))))

(defmulti r (fn [opts _] (:type opts)))

(defmethod r :get
  ([opts] (r opts (chan)))
  ([opts ch]
   (assert (contains? opts :url) "You need to supply a :url")
   (let [params (build-request ch (:data opts)) ]
     (GET (:url opts) params)
     ch)))

(defmethod r :post
  ([opts] (r opts (chan)))
  ([opts ch]
   (assert (not (empty? (:data opts))))
   (assert (contains? opts :url))
   (POST (:url opts)
         (build-request ch (:data opts)))
   ch))

(defmethod r :put
  ([opts] (r opts (chan)))
  ([opts ch]
   (assert (not (empty? (:data opts))))
   (assert (contains? opts :url))
   (PUT (:url opts)
        (build-request ch (:data opts)))
   ch))

(defmethod r :delete
  ([opts] (r opts (chan)))
  ([opts ch]
   (assert (not (empty? (:data opts))))
   (assert (contains? opts :url))
   (DELETE (:url opts)
           (build-request ch (:data opts)))
   ch))
