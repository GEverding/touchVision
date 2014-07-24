(ns server.handlers.api
  (:require [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clojure.java.io :as io]
            [clojure.core.match :refer [match]]
            [chord.http-kit :refer [with-channel]]
            [clj-time.core :as t]
            [clj-time.coerce :as c]
            [clojure.math.numeric-tower :as math :refer [floor round]]
            [clojure.core.async :refer [<!  >! take! put! close! chan sliding-buffer dropping-buffer go-loop]]
            [liberator.core :refer [defresource]]
            [taoensso.timbre :as timbre]
            [clojure.core.match :refer [match]]
            [cheshire.core :as json :refer [decode encode generate-string]]
            [server.handlers.util :refer :all]
            [server.config :refer [cfg]]))

(timbre/refer-timbre)
(defonce conn  (rmq/connect))
(defonce ch    (lch/open conn))

(defonce mode (atom {:stream :fake
                     :is-running? false}))

(defn ^:private random-date [start end]
  (+ start (* (rand) (- end start))))

(defn ^:private generate-date
  "Generate random set of times (x) between start and end"
  [x start end]
  (for [low (range x )] (random-date start end)))

(defn ^:private wrap-data [data]
  {:type :post
   :data data })

(defn ^:private gen-fake-data [i]
  (wrap-data
   {:x (rand 100)
    :y (rand 100)
    :z (rand 100)
    :pressure (floor (rand 5))
    :timestamp (+ i (rand))}))

(defn ^:private message-handler
  [ws-chan ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (let [blob (String. payload "UTF-8")]
    (debug ws-chan)
    (put! ws-chan (decode blob true))
    (debug (format "[rabbit] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                  (decode blob true) delivery-tag content-type type))))


(defresource switch-stream
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :known-content-types #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::data)
  :handle-created (fn [_] ( encode {:data "success"}))
  :post! (fn [ctx]
           (let [stream (-> ctx ::data :stream) ]
             (println (::data ctx))
             (println @mode)
             (swap! mode assoc :stream (keyword stream))
             (println @mode)
             stream)))

(defresource switch-running
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :known-content-types #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::data)
  :handle-created (fn [_] ( encode {:data "success"}) )
  :post! (fn [ctx]
           (let [is-running? (-> ctx ::data :toggle) ]
             (println (::data ctx))
             (println @mode)
             (swap! mode assoc :is-running? is-running?)
             (println @mode)
             )))

(defn cap [data]
  (let [quantized (- (* (-> (str data) read-string round) 2000) 1000)]
    (if (< quantized 0)
      0
      quantized)))

(defresource start-playback
  :available-media-types ["application/json"]
  :allowed-methods [:post]
  :known-content-types #(check-content-type % ["application/json"])
  :malformed? #(parse-json % ::data)
  :handle-created (fn [_] ( encode {:data "success"}) )
  :post! (fn [ctx]
           (   let [qname (lq/declare-server-named ch :exclusive true)
                    data (-> ctx ::data :pressure cap)
                    blob (encode { :pressure data }) ]
             (le/direct ch "touchvision")
             (info "[playback]: " blob)
             (lb/publish ch "touchvision" "playback"  blob :content-type "text/plain")
             data
             )))

(defn data-feed [req]
  (let [rmq-chan (chan (sliding-buffer 100))
        qname (lq/declare-server-named ch :exclusive true :auto-delete true) ]
    (le/direct ch "touchvision")
    (lq/bind ch qname "touchvision" :routing-key "glove")
    (lc/subscribe ch qname (partial message-handler rmq-chan) :auto-ack true)
    (with-channel req ws-ch
      {:read-ch (chan (dropping-buffer 10))
       :format :edn} ; again, :edn is default
      (go-loop [i 25]
               (let [is-running? (:is-running? @mode)
                     which-stream? (:stream @mode) ]
                 (condp = which-stream?
                   :fake  (let [new-data (gen-fake-data i)]
                            (if is-running?
                              (do
                                (debug new-data)
                                (>! ws-ch new-data)
                                (Thread/sleep (+ 1000 (rand-int 200))))
                              ()))
                   :live (let [new-data (<! rmq-chan)]
                          (debug new-data)
                           (if is-running?
                             (>! ws-ch (wrap-data new-data))
                             ()))
                   (error "not valid stream type" which-stream?))
                 (recur (+ i (rand))))))))

