(ns client.views.debug
  (:require-macros [cljs.core.async.macros :refer [go]]
                   [dommy.macros :refer [node sel sel1]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [chord.client :refer [ws-ch]]
            [cljs.core.async :as async :refer [<! >! chan]]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(defn print-data "doc-string" [data]
  (str "{ " (map (fn [[k v]] (str k " " v "\n")) data) " }"))

(defn data-row-view [data owner]
  (reify
    om/IRender
    (render [_]
      (.log js/console (print-data data))
      (html [:code {:class "row js-row-data"} (print-data data)]))))

(defn debug-view [app owner]
  (reify
    om/IInitState
    (init-state [this]
      )
    ;; om/IWillMount
    ;; (will-mount [_]
    ;;   (let [ws (:chan app) ]
    ;;     (.log js/console ws)
    ;;     (go (loop []
    ;;           (let [ m (:message (<! ws)) ]
    ;;             (.log js/console  (pr-str m))
    ;;             (om/transact! app [:data] (fn [ds] (conj ds {:msg (pr-str m)})) )
    ;;           (recur))
              ;; ))))
    om/IRenderState
    (render-state [_ state]
      (html [:div {:class "row debug-view js-debug-view"}
             (let [data (:data app)]
               (.log js/console data)
               (if-not (empty? data)
                 (om/build-all data-row-view data)
                 (.log js/console "no data"))
               )]))))
