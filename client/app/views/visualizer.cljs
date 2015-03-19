(ns app.views.visualizer
  (:require-macros [cljs.core.async.macros :refer [go-loop]])
  (:require [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.async :as async :refer [<! put! chan sub sliding-buffer]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]
            [app.views.viz.plot :as plot]
            [app.views.viz.table :as table]))

(def ^:private l (log/get-logger "viz"))


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
