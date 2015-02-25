(ns client.views.viz.plot
  (:require-macros [cljs.core.async.macros :refer [go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [strokes :refer (d3)]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [cljs.core.async :as async :refer [<! put! chan sub sliding-buffer]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [sablono.core :as html :refer-macros [html]]))

(def l (log/get-logger "3d"))
(strokes/bootstrap)

(defn ^:private v [x y z]
  (js/THREE.Vector3. x y z))

;; ref: http://bl.ocks.org/phil-pedruco/9852362
(defcomponent plot-view
  [app owner]
  (init-state
   [_]
   (let [w 960 h 500
         line-geo (js/THREE.Geometry.)
         line-material (js/THREE.LineBasicMaterial.  (clj->js  {:color 0x000000
                                                                :lineWidth 1}))]
     {:renderer (js/THREE.WebGLRenderer. (clj->js {:antialias true}))
      :camera (js/THREE.PerspectiveCamera. 45 (/ w h) 1 10000)
      :scene (js/THREE.Scene.)
      :scatter-plot (js/THREE.Object3D.)
      :line-geo line-geo
      :line-material line-material
      :line (js/THREE.Line. line-geo line-material)
      :p-mat (js/THREE.ParticleBasicMaterial. (clj->js {:vertexColors true
                                                       :size 10}))
      :w w
      :h h
      :x-scale (-> d3 .-scale (.linear) (.range [-50 50]))
      :y-scale (-> d3 .-scale (.linear) (.range [-50 50]))
      :z-scale (-> d3 .-scale (.linear) (.range [-50 50]))
      }))
  (will-mount
   [_]
   (let [{:keys [renderer camera scene scatter-plot line w h]} (om/get-state owner)]
     (. renderer (setSize w h))
     (. renderer (setClearColor 0xEEEEEE 1.0))
     (aset camera "position" "x" 200)
     (aset camera "position" "y" -100)
     (aset camera "position" "z" 100)
     (. scene (add scatter-plot))
     (aset scatter-plot "rotation" "y" 0)
     (aset line "type" (.-Lines js/THREE))
     (. scatter-plot (add line))
     ))
  (did-mount
   [_]
   (let [{:keys [renderer camera scene scatter-plot w h]} (om/get-state owner)]
     (dommy/append! (om/get-node owner "plot") (.-domElement renderer)))
   )
  (render-state
   [_ state]
   (html [:div.plot-view {:ref "plot"}])))
