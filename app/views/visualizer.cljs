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
            [sablono.core :as html :refer-macros [html]]
            [client.views.viz.plot :as plot]
            [client.views.viz.table :as table]))

(def ^:private l (log/get-logger "viz"))


(defn hide? [bounds d]
  (let [{:keys [low high]} bounds
        t (get-in d [:timestamp])]
    (and (>= t low) (< t high))))

(defcomponent visualizer-view
  [app owner]
  (render
    [_]
    (html [:div.visualizer-view
           [:ul.nav.nav-tabs
            [:li.active
             [:a {:href "#3d"
                  :data-toggle "tab"
                  } "3D"] ]
            [:li
             [:a {:href "#raw"
                  :data-toggle "tab"
                  } "Raw"]]]
           [:div.tab-content.visualizer-container
            [:div.tab-pane.active#3d (plot/->plot-view app)]
            [:div.tab-pane#raw (table/->table-view app)]]
           ]))
  )
