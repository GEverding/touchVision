(ns client.views.viz.plot
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

;; (def l (log/get-logger "3d"))

(defcomponent plot-view
  [app owner]
  (render [_] (html [:p "3D"])))
