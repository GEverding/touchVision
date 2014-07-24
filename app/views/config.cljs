(ns client.views.config
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

(defn side-bar [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:div {:class "row"}
             [:div.sidebar
              [:ul.nav-nav-sidebar
               [:li [:a {:href ""} "Glove"]]
               [:li [:a {:href ""} "Playback"]]
               ]]]))))

(defn controls [app owner]
  (reify
    om/IRender
    (render [_]
      (html [:div.container-fluid
             [:h2 "Controls"]]))))
