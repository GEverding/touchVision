(ns server.io.writer
  (:require [com.stuartsierra.component :refer (Lifecycle)]
            [clojure.core.async :refer [go-loop put! close! chan sliding-buffer <!]]
            [taoensso.timbre :as timbre]
            [server.db.queries :as q]))

(timbre/refer-timbre)

(def ^:private current_rocording (atom nil))

(defn set-recording-id! [id]
  (if id
    (do
      (reset! current_rocording id)
      true)
    false))

(defrecord Writer [capture db]
  Lifecycle
  (start [this]
    (let [conn (:conn db)
          stdin (:stdout capture)
          stdout (chan (sliding-buffer 100)) ]
      (go-loop
        [datom (<! stdin)]
        (when datom
          (let [{:keys [pressure timestamp x y z]} (:data datom) ]
            (when @current_rocording
              (let [row (q/append<! conn @current_rocording pressure x y z timestamp)]
                (debug "saved: " row)
                (when row
                  (put! stdout {:type :post :data row}))))
            (recur (<! stdin)))))
      (-> this
          (assoc :stdout stdout))
      ))
  (stop [this]
    (close! (:stdout this))
    this))

(defn start-writer []
  (map->Writer {}))
