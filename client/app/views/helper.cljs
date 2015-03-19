(ns app.views.helper
  (:require [cljs.core.async :as async :refer [put! <! >! chan merge]]
            [om.core :as om :include-macros true]))

(defn clear-screen [owner]
  (let [event-bus (om/get-shared owner [:event-bus :chan])]
        (put! event-bus :reset)))

(defn hide? [bounds d]
  (let [{:keys [low high]} bounds
        t (get-in d [:timestamp])]
    (and (>= t low) (< t high))))
