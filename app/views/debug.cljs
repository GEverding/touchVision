(ns client.views.debug
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]]
                   [dommy.macros :refer [node sel sel1]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [cljs.core.match :as m]
            [cljs.core.async :as async :refer [<! put!]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [client.router :refer [navigate]]
            ))

(defn print-data "" [data]
  (str "{ " (map (fn [[k v]] (str k " " v "\n")) data) " }"))

(defn ^:private filter-range [x low high]
  (println (<= x high) (>= x low))
  (and  (<= x high) (>= x low)) )

(defn ^:private to-hide? [is-empty? low high ps]
  (map (fn [p]
         (let [ts (:timestamp p)]
           (match [(filter-range ts low high) is-empty?]
                  [true _] (assoc p :hidden false)
                  [false true] (-> p
                                   (assoc :hidden false)
                                   (assoc :visible true))
                  [_ _] (assoc p :hidden true))
           )) ps))

(defn ^:private update-selected [data owner e]
  (let [c (om/get-state owner :chan ) ]
    (put! c data)))

(defn data-row-view [data owner]
  (reify
    om/IRenderState
    (render-state [_ state]
      (let [ is-hidden? (:hidden data)]
      (.log js/console (print-data data))
      (html [:code {:class "row js-row-data data-row"
                    :style {:display (if is-hidden? "none" "block")}
                    :on-click #(update-selected data owner %)
                    } (print-data data)])))))

(defn debug-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      {:filtered false})
    om/IWillMount
    (will-mount [_]
      (let [c (:chan app) ]
        (go-loop []
              (let [m (<! c)
                    low (:low m) high (:high m)
                    brush-empty? (:brush-empty? m) ]
                (.log js/console "is brush empty? " brush-empty?)
                (.log js/console m)
                (om/update! app :filtered brush-empty?)
                (om/transact! app :data (partial to-hide? brush-empty? low high))
                (recur)))))

    om/IRender
    (render [_]
      (html [:div {:class "row debug-view js-debug-view"}
             (let [data (-> app :data reverse)]
               (.log js/console data)
               (if-not (empty? data)
                 (om/build-all data-row-view data {:init-state {:chan (:dwnld-chan app)}})
                 (.log js/console "no data"))
               )]))))
(defn controls [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "col-lg-12"}
             [:button {:class "btn btn-warning pull-right"
                       :type "button"
                       } [:i {:class "fa fa"}] " Reset"]
             [:button {:class "btn btn-info pull-right"
                       :type "button"
                       :on-click (fn [_] (navigate "config"))
                       } [:i {:class "fa fa-wrench"}] "Configuration"]
             ]))))
