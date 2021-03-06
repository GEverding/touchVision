(ns client.views.viz.plot
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:import [goog.events EventType])
  (:require [strokes :refer (d3)]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [goog.events :as events]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [client.request :refer (r)]
            [cljs.core.async :as async :refer [<! put! chan sub sliding-buffer alts!]]
            [cljs-log.core :as log]
            [om.dom :as dom :include-macros true]
            [om.core :as om :include-macros true]
            [client.colours :refer (pressure-colours)]
            [client.views.visualizer :refer (hide?)]
            [sablono.core :as html :refer-macros [html]]))

(def l (log/get-logger "3d"))
(strokes/bootstrap)


(defn events->chan
  "Given a target DOM element and event type return a channel of
  observed events. Can supply the channel to receive events as third
  optional argument."
  ([el event-type] (events->chan el event-type (chan)))
  ([el event-type c]
   (events/listen el event-type
                  (fn [e] (put! c e)))
   c))

(defn ^:private v [x y z]
  (js/THREE.Vector3. x y z))

(defn ^:pivate animate [renderer camera scene t]
  (.clear renderer)
  (. camera (lookAt (.-position scene)))
  (. renderer (render scene camera))
  (. js/window (requestAnimationFrame (partial animate renderer camera scene) (.-domElement renderer))))

(defn render [owner datoms]
  (let [{:keys [renderer
                camera
                scene
                scatter-plot
                line
                line-geo
                line-material
                points
                p-mat
                w h ]}
        (om/get-state owner)
        new-line-geo (js/THREE.Geometry.)
        x-event (-> d3 (.extent datoms #(:x %)))
        y-event (-> d3 (.extent datoms #(:y %)))
        z-event (-> d3 (.extent datoms #(:z %)))
        x-scale (-> d3 .-scale (.linear) (.domain x-event) (.range [-50 50]))
        y-scale (-> d3 .-scale (.linear) (.domain y-event) (.range [-50 50]))
        z-scale (-> d3 .-scale (.linear) (.domain z-event) (.range [-50 50]))
        vpts {:x-max (nth x-event 1)
              :x-cen (/ (+ (nth x-event 1)
                           (nth x-event 0)) 2)
              :x-min (nth x-event 0)
              :y-max (nth y-event 1)
              :y-cen (/ (+ (nth y-event 1)
                           (nth y-event 0)) 2)
              :y-min (nth y-event 0)
              :z-max (nth z-event 1)
              :z-cen (/ (+ (nth z-event 1)
                           (nth z-event 0)) 2)
              :z-min (nth z-event 0)}
        vs [(v (x-scale (:x-min vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-cen vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-cen vpts)))
            (v (x-scale (:x-cen vpts)) (y-scale (:y-min vpts)) (z-scale (:z-cen vpts))) (v (x-scale (:x-cen vpts)) (y-scale (:y-max vpts)) (z-scale (:z-cen vpts)))
            (v (x-scale (:x-cen vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-cen vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-cen vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-cen vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-cen vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-cen vpts)))
            (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-cen vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-cen vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-cen vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-cen vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-cen vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-cen vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-cen vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-cen vpts)))
            (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-min vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-max vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-cen vpts)) (z-scale (:z-max vpts)))
            (v (x-scale (:x-cen vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-cen vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
            (v (x-scale (:x-cen vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-cen vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))
            ]]
    (doseq [v vs]
      (-> new-line-geo .-vertices (.push v)))
    (. scatter-plot (remove line))
    (let [new-line (-> (js/THREE.BoxHelper. (js/THREE.Line. new-line-geo
                                                            line-material
                                                            (.-Lines js/THREE)))
                       )]
      (. (aget new-line "material" "color") (set 0x000000))
      (. scatter-plot (add new-line))
      (om/set-state! owner :line new-line))

    (let [point-geo (js/THREE.Geometry.)
          time-bound (om/get-state owner :time-bound)
          ds (filter (partial hide? time-bound) datoms)]

      (doseq [d ds]
        (let [x (x-scale (:x d))
              y (y-scale (:y d))
              z (z-scale (:z d))]
          (-> point-geo .-vertices (.push (v x y z)))
          (-> point-geo .-colors (.push (js/THREE.Color. (nth pressure-colours (int (:pressure d))))))))
      (let [new-points (js/THREE.PointCloud. point-geo p-mat)]
        (. scatter-plot (remove points))
        (. scatter-plot (add new-points))
        (om/set-state! owner :points new-points)
        (.clear renderer)
        (. camera (lookAt (.-position scene)))
        (. renderer (render scene camera))
        ))))

;; ref: http://bl.ocks.org/phil-pedruco/9852362
(defcomponent plot-view
  [app owner]
  (init-state
   [_]
   (let [w 950 h 500
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
      :points (js/THREE.PointCloud.)
      :p-mat (js/THREE.PointCloudMaterial. (clj->js {:vertexColors true
                                                     :size 10}))
      :w w
      :h h
      :datoms []
      :sx 0
      :sy 0
      :down false
      :time-bound {:low (.-MIN_VALUE js/Number) :high (.-MAX_VALUE js/Number)}
      }))
  (will-mount
   [_]
   (do
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
             ws-sub-chan (do
                           (om/update-state! owner :datoms
                                             (fn [datoms]
                                               (let [d (:data m)]
                                                 (if (vector? d)
                                                   (let [ds (filter #(< 0.3 (:pressure %)) d)]
                                                     (into [] (concat datoms (vec ds))))
                                                   (conj datoms d))
                                                 )))
                           (render owner (om/get-state owner :datoms)))
             s-chan (do
                           (om/set-state! owner :time-bound m)
                           (render owner (om/get-state owner :datoms)))
             e-chan (if (= m :reset)
                      (do
                        (om/set-state! owner :datoms [])
                        (render owner (om/get-state owner :datoms)))))
           (recur (alts! [ws-sub-chan s-chan e-chan]))))))
   )
  (did-mount
   [_]
   (do
     (let [{:keys [:renderer :camera :scene :scatter-plot :w :h]} (om/get-state owner)
           down (chan) up (chan) move (chan)]
       (events->chan (om/get-node owner "plot") EventType.MOUSEDOWN down)
       (events->chan (om/get-node owner "plot") EventType.MOUSEUP up)
       (events->chan (om/get-node owner "plot") EventType.MOUSEMOVE move)
       (go-loop [[event c] (alts! [down up move])]
         (condp = c
           down (do
                  (log/fine l "DOWN")
                  (om/set-state! owner :down true)
                  (om/set-state! owner :sx (.-clientX event))
                  (om/set-state! owner :sy (.-clientY event)))
           up (do
                (log/fine l "UP")
                (om/set-state! owner :down false))
           move (when (om/get-state owner :down)
                  (let [{:keys [sx sy]} (om/get-state owner)
                        dx (- (.-clientX event) sx)
                        dy (- (.-clientY event) sy)]
                    (aset scatter-plot "rotation" "y" (+ (aget scatter-plot "rotation" "y") (* dx 0.01)))
                    (aset camera "position" "y" (+ (aget camera "position" "y") dy))
                    (om/set-state! owner :sx (+ sx dx))
                    (om/set-state! owner :sy (+ sy dy))
                    ))
           (log/warning l "nothing!"))
         (recur (alts! [down up move]))))
     (let [{:keys [renderer camera scene scatter-plot w h]} (om/get-state owner)]
       (. renderer (setSize w h))
       (dommy/append! (om/get-node owner "plot") (.-domElement renderer))
       (. renderer (setClearColor 0xEEEEEE 1.0))
       (. scene (add scatter-plot))
       (aset camera "position" "x" 200)
       (aset camera "position" "y" -100)
       (aset camera "position" "z" 100)
       (aset scatter-plot "rotation" "y" 0)
       (render owner (om/get-state owner :datoms))
       (animate renderer camera scene (.getTime (js/Date.)))))

   )
  (render
   [_]
   (html [:div.plot-view {:ref "plot"}])))
