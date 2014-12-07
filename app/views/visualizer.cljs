(ns client.views.visualizer
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [cljs.core.async :as async :refer [<! put! chan sub sliding-buffer]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

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

(defcomponent data-row-view [data owner]
  (render-state
    [_ state]
    (html [:code {:class "row js-row-data data-row"
                  ;; :style {:display (if is-hidden? "none" "block")}
                  :on-click #(update-selected data owner %)
                  } (print-data data)]) ) )

(defcomponent visualizer-view [app owner]
  (init-state [this] {:datoms [] })
  (will-mount
    [_]
    (let [ws (:ws-chan (om/get-shared owner))
          ch (chan (sliding-buffer 25))]
      (sub ws :post ch)
      (go-loop
        [m (<! ch)]
        (when m
          (do
            (.log js/console m)
            (om/update-state! owner :datoms #(conj % m))
            (recur (<! ch)))) )))
  (render-state
    [_ state]
    (html [:div {:class "row visuilizer-view js-visulizer-view"}
           (let [datoms (:datoms state)]
             (if-not (empty? datoms)
               (om/build-all data-row-view datoms {:key :id})
               (.log js/console "no data")))])))
