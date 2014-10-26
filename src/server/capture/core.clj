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

(timbre/refer-timbre)

(defn ^:private random-date [start end]
  (+ start (* (rand) (- end start))))

(defn ^:private generate-date
  "Generate random set of times (x) between start and end"
  [x start end]
  (for [low (range x)] (random-date start end)))

(defn ^:private wrap-data [data]
  {:type :post
   :data data})

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

(defonce state (atom {}))

(defrecord Capture [mode]
  component/Lifecycle
  (start [this]
    (let [conn (rmq/connect)
          ch (lch/open conn)
          stdin (chan (sliding-buffer 200))
          stdout (chan (sliding-buffer 200))
          qname (lq/declare-server-named ch :exclusive true :auto-delete true) ]
      (swap! state assoc :mode mode )
      (le/direct ch "touchvision")
      (lq/bind ch qname "touchvision" :routing-key "glove")
      (lc/subscribe ch qname (partial message-cb stdin) :auto-ack true)
      (go-loop
        [i 0]
        (if (= (:mode @state) :live)
          (let [datom (<! stdin)]
            (>! stdout (wrap-data datom)))
          (let [datom (gen-fake-data i)]
            (do
              (println datom)
              (>! stdout (wrap-data datom))
              (Thread/sleep (+ 1000 (rand-int 200))))))
        (recur (inc i)))
      (-> this
          (assoc :mode mode)
          (assoc :stdout stdout)
          (assoc :stdin stdin)
          (assoc :rmq-conn conn)
          (assoc :rmq-ch ch) )))
  (stop [this]
    (rmq/close (:rmq-conn this))
    (close! (:stdin this))
    (close! (:stdout this))
    (swap! state {})
    (-> this
        (assoc :mode nil)
        (assoc :stdin nil)
        (assoc :stdout nil)
        (assoc :rmq-conn nil)
        (assoc :rmq-ch nil))))

(defn set-mode! [this new-mode]
  (swap! state :mode new-mode))

(defn capture-start [mode]
  (map->Capture {:mode mode}))

