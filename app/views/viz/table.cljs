(ns client.views.viz.table
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

(def ^:private l (log/get-logger "table"))

(defn print-data "" [data]
  (str "{ " (map (fn [[k v]] (str k " " v "\n")) data) " }"))

(defn ^:private filter-range [x low high]
  (println (<= x high) (>= x low))
  (and  (<= x high) (>= x low)) )

(defn hide? [bounds d]
  (let [{:keys [low high]} bounds
        t (get-in d [:timestamp])]
    (and (>= t low) (< t high))))

(defn ^:private update-selected [data owner e]
  (let [c (om/get-shared owner :download-chan ) ]
    (put! c data)))

(defcomponent data-row-view [data owner]
  (render
    [_]
    (let [d data]
    (html
      [:tr {:on-click #(update-selected data owner %) }
       [:td (:x d)]
       [:td (:y d)]
       [:td (:z d)]
       [:td (:pressure d)]
       [:td (.format (js/moment (:timestamp d)) "HH:mm:ss:SSS")]
       [:td (if (:is_new d)
              (/ (- (.getTime (js/Date.)) (:timestamp d)) 1000)
              "N/A")]]))))

(defcomponent table-view [app owner]
  (init-state
    [this]
    {:datoms []
     :time-bound {:low (.-MIN_VALUE js/Number) :high (.-MAX_VALUE js/Number)}})
  (will-mount
    [_]
    (let [{:keys [ws-chan select-chan event-bus]} (om/get-shared owner)
          e-chan (chan)
          s-chan (chan)
          ws-sub-chan (chan (sliding-buffer 25))]
      (async/tap (:bus event-bus) e-chan)
      (async/tap (:bus select-chan) s-chan)
      (sub (:pub ws-chan) :post ws-sub-chan)
      (go-loop
        [[m c] (alts! [ws-sub-chan s-chan e-chan])]
        (when m
          (condp = c
            ws-sub-chan (om/update-state! owner :datoms (fn [ds] (let [d (:data m)]
                                                                  (if (vector? d)
                                                                    (into [] (concat ds d))
                                                                    (conj ds d)))))
            e-chan (if (= m :reset)
                     (om/set-state! owner :datoms []))
            s-chan (do
                     (om/set-state! owner :time-bound m)))
          (recur (alts! [ws-sub-chan s-chan e-chan]))))))
  (render-state
    [_ {:keys [time-bound datoms]}]
    (html
     (let [ds (filter (partial hide? time-bound) datoms)]
       [:table.table.table-hover.table-striped
        [:thead
         [:tr
          [:th "X"]
          [:th "Y"]
          [:th "Z"]
          [:th "Pressure"]
          [:th "Timestamp"]
          [:th "Response Time"]]]
        [:tbody
         (if-not (empty? ds)
           (om/build-all data-row-view ds {:key :id})
           (log/info l "no data"))]
        [:tfoot
         [:tr [:span (str (count ds) " / " (count datoms))]]]]))))
