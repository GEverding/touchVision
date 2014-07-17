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
            [clojure.contrib.math :refer [floor]]
            [clojure.core.async :refer [<! >!! >! put! close! chan sliding-buffer dropping-buffer go-loop]]
            [liberator.core :refer [defresource]]
            [taoensso.timbre :as timbre]
            [clojure.core.match :refer [match]]
            [cheshire.core :as json :refer [decode encode]]
            [server.config :refer [cfg]]))

(timbre/refer-timbre)

(defonce mode (atom {:stream :fake
                     :is-running? false}))

(defn ^:private random-date [start end]
  (+ start (* (rand) (- end start))))

(defn ^:private generate-date
  "Generate random set of times (x) between start and end"
  [x start end]
  (for [low (range x )] (random-date start end)))

(defn ^:private gen-fake-data []
  {:type :post
   :data
   {:x (rand 100)
    :y (rand 100)
    :z (rand 100)
    :pressure (floor (rand 5))
    :timestamp (floor (random-date
                        (c/to-long (t/now))
                        (c/to-long
                          (t/plus (t/now) (t/seconds 100)))))}})
(def ^{:const true}
  default-exchange-name "")

(defn ^:private message-handler
  [ws-chan ch {:keys [content-type delivery-tag type] :as meta} ^bytes payload]
  (let [blob (String. payload "UTF-8")]
    (debug(format "[rabbit] Received a message: %s, delivery tag: %d, content type: %s, type: %s"
                  (String. payload "UTF-8") delivery-tag content-type type))
    (>!! ws-chan (decode blob))))

;; convert the body to a reader. Useful for testing in the repl
;; where setting the body to a string is much simpler.
(defn body-as-string [ctx]
  (if-let [body (get-in ctx [:request :body])]
    (condp instance? body
      java.lang.String body
      (do
        print body
      (slurp (io/reader body))))))

;; For PUT and POST parse the body as json and store in the context
;; under the given key.
(defn parse-json [context key]
  (when (#{:put :post} (get-in context [:request :request-method]))
    (try
      (if-let [body (body-as-string context)]
        (let [data (decode body true)]
          [false {key data}])
        {:message "No body"})
      (catch Exception e
        (.printStackTrace e)
        {:message (format "IOException: %s" (.getMessage e))}))))

;; For PUT and POST check if the content type is json.
(defn check-content-type [ctx content-types]
  (if (#{:put :post} (get-in ctx [:request :request-method]))
    (or
     (some #{(get-in ctx [:request :headers "content-type"])}
           content-types)
     [false {:message "Unsupported Content-Type"}])
    true))

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

(defn data-feed [req]
  (let [conn  (rmq/connect)
        ch    (lch/open conn)
        qname (cfg :capture-queue)
        rmq-chan (sliding-buffer 100) ;; sanity and memory
        ]
    (lq/declare ch qname :exclusive false :auto-delete true)
    (lc/subscribe ch qname (partial message-handler rmq-chan) :auto-ack true)
    (with-channel req ws-ch
      {:read-ch (chan (dropping-buffer 10))
       :format :edn} ; again, :edn is default
      (go-loop []
               (let [is-running? (:is-running? @mode)
                     which-stream? (:stream @mode) ]
                 (condp = which-stream?
                   :fake  (let [new-data (gen-fake-data)]
                            (if is-running?
                              (do
                                (debug new-data)
                                (>! ws-ch new-data)
                                (Thread/sleep 500))
                              ))
                   :live (let [new-data (<! rmq-chan)]
                           (if is-running?
                             (do
                               (debug new-data)
                               (>! ws-ch new-data))
                             (identity)))
                   (error "not valid stream type" which-stream?))
                 (recur))))))

