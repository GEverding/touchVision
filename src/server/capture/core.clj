(ns server.capture.core
  (:require [com.stuartsierra.component :as component]
            [clojure.math.numeric-tower :as math :refer [floor round]]
            [clojure.core.async :refer [<! >! put! close! chan sliding-buffer go-loop pub sub]]
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

(defrecord Capture [mode rabbit]
  component/Lifecycle
  (start [this]
    (let [state (atom {})
          stdin (chan (sliding-buffer 10))
          out (chan (sliding-buffer 10))
          stdout (pub out :type)]
      (sub (:stdout rabbit) :glove stdin)
      (swap! state assoc :mode mode)
      (swap! state assoc :demo true)
      (go-loop
        [i 0]
        (if (= (:mode @state) :live)
          (let [datom (<! stdin)]
            ;; extract from aysnc pub wrapper
            (when (>! out (wrap-data (:data datom)))
              (recur (inc i)) ))
          (let [datom (gen-fake-data i)]
            (when (>! out (wrap-data datom))
              (do
                (Thread/sleep (+ 3000 (rand-int 2000)))
                (recur (inc i)))))))
      (-> this
          (assoc :state state)
          (assoc :mode mode)
          (assoc :stdout stdout)
          (assoc :stdin stdin)
          (assoc :out out)
          )))
  (stop [this]
    (close! (:stdin this))
    (close! (:out this))
    (swap! (:state this) {})
    (-> this
        (assoc :mode nil)
        ;; (assoc :stdin nil)
        ;; (assoc :stdout nil)
        (assoc :state (atom {})))))

(defn capture-start [mode]
  (map->Capture {:mode mode}))
