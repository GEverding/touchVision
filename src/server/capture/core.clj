(ns server.capture.core
  (:require [com.stuartsierra.component :as component]
            [langohr.core      :as rmq]
            [langohr.channel   :as lch]
            [langohr.queue     :as lq]
            [langohr.exchange  :as le]
            [langohr.consumers :as lc]
            [langohr.basic     :as lb]
            [clojure.math.numeric-tower :as math :refer [floor round]]
            [clojure.core.async :refer [<! >! put! close! chan sliding-buffer go-loop]]
            [taoensso.timbre :as timbre]
            [cheshire.core :refer [decode encode]]))

(defn uuid [] (str (java.util.UUID/randomUUID)))

(timbre/refer-timbre)

(defn ^:private random-date [start end]
  (+ start (* (rand) (- end start))))

(defn ^:private generate-date
  "Generate random set of times (x) between start and end"
  [x start end]
  (for [low (range x)] (random-date start end)))

(defn ^:private wrap-data [data]
  {:type :post
   :data (merge  data {:id (uuid)})})

(defn ^:private gen-fake-data [i]
   {:x (rand 100)
    :y (rand 100)
    :z (rand 100)
    :pressure (floor (rand 6))
    :timestamp (+ i (rand))})

(defn ^:private message-cb [stdin ch {:keys [content-type delivery-tag type] :as meta}  ^bytes payload]
  (let [blob (decode (String. payload "UTF-8") true) ]
    (println blob)
    (put! stdin blob )))


(defrecord Capture [mode]
  component/Lifecycle
  (start [this]
    (let [state (atom {})
          conn (rmq/connect)
          ch (lch/open conn)
          stdin (chan (sliding-buffer 10))
          stdout (chan (sliding-buffer 10))
          qname (lq/declare-server-named ch { :exclusive true :auto-delete true})]
      (swap! state assoc :mode mode )
      (le/direct ch "touchvision")
      (lq/bind ch qname "touchvision" {:routing-key "glove"})
      (lc/subscribe ch qname (partial message-cb stdin) {:auto-ack true})
      (go-loop
        [i 0]
        (if (= (:mode @state) :live)
          (let [datom (<! stdin)]
              (when (>! stdout (wrap-data datom))
                (recur (inc i)) ))
          (let [datom (gen-fake-data i)]
            (when (>! stdout (wrap-data datom))
              (do
                (Thread/sleep (+ 1000 (rand-int 2000)))
                (recur (inc i)))))))
      (-> this
          (assoc :state state)
          (assoc :mode mode)
          (assoc :stdout stdout)
          (assoc :stdin stdin)
          (assoc :rmq-conn conn)
          (assoc :rmq-ch ch) )))
  (stop [this]
    (rmq/close (:rmq-conn this))
    (close! (:stdin this))
    (close! (:stdout this))
    (swap! (:state this) {})
    (-> this
        (assoc :mode nil)
        ;; (assoc :stdin nil)
        ;; (assoc :stdout nil)
        (assoc :rmq-conn nil)
        (assoc :state (atom {}))
        (assoc :rmq-ch nil))))

(defn capture-start [mode]
  (map->Capture {:mode mode}))

