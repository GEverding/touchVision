(ns client.views.visualizer
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [cljs.core.async :as async :refer [<! put! chan sub sliding-buffer]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(def ^:private l (log/get-logger "viz"))

(defn print-data "" [data]
  (str "{ " (map (fn [[k v]] (str k " " v "\n")) data) " }"))

(defn ^:private filter-range [x low high]
  (println (<= x high) (>= x low))
  (and  (<= x high) (>= x low)) )

(defn hide? [bounds d]
  (let [{:keys [low high]} bounds
        t (get-in d [:data :timestamp])]
    (log/fine l bounds)
    (log/fine l (< t high))
    (and (>= t low) (< t high))))

(defn ^:private update-selected [data owner e]
  (let [c (om/get-state owner :chan ) ]
    (put! c data)))

(defcomponent data-row-view [data owner]
  (render
    [_]
    ( html
      [:code {:class "row js-row-data data-row"
              ;; :style {:display (if is-hidden? "none" "block")}
              :on-click #(update-selected data owner %)
              } (print-data data)])))

(defcomponent visualizer-view [app owner]
  (init-state
    [this]
    {:datoms []
     :time-bound {:low (.-MIN_VALUE js/Number) :high (.-MAX_VALUE js/Number)}})
  (will-mount
    [_]
    (let [{:keys [ws-chan select-chan]} (om/get-shared owner)
          ws-sub (chan (sliding-buffer 25))]
      (sub ws-chan :post ws-sub)
      (go-loop
        [[m c] (alts! [ws-sub select-chan])]
        (when m
          (condp = c
            ws-sub (om/update-state! owner :datoms #(conj % m))
            select-chan (do
                          (om/set-state! owner :time-bound m)))
          (recur (alts! [ws-sub select-chan]))))))
  (render-state
    [_ {:keys [time-bound datoms]}]
    (html [:div {:class "row visualizer-view js-visualizer-view"}
           (let [ds (filter (partial hide? time-bound) datoms)]
             (if-not (empty? ds)
               (om/build-all data-row-view ds {:key :id})
               (log/info l "no data")))])))

