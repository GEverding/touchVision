(ns client.views.viz.plot
  (:require-macros [cljs.core.async.macros :refer [go go-loop]]
                   [cljs.core.match.macros :refer [match]])
  (:require [strokes :refer (d3)]
            [dommy.utils :as utils]
            [dommy.core :as dommy]
            [om-tools.core :refer-macros [defcomponent]]
            [cljs.core.match :as m]
            [client.request :refer (r)]
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
                                                                :lineWidth 2}))]
     {:renderer (js/THREE.WebGLRenderer. (clj->js {:antialias true}))
      :camera (js/THREE.PerspectiveCamera. 45 (/ w h) 1 10000)
      :scene (js/THREE.Scene.)
      :scatter-plot (js/THREE.Object3D.)
      :line-geo line-geo
      :line-material line-material
      :line (js/THREE.Line. line-geo line-material)
      :p-mat (js/THREE.PointCloudMaterial. (clj->js {:vertexColors true
                                                       :size 10}))
      :w w
      :h h
      :shadow-datoms (atom [])
      :datoms (atom [])
      }))
  (will-mount
   [_]
   )
  (did-mount
   [_]
   (let [cb (r {:type :get :url (str "/recording/" (:recording-id @app))})]
     (go (let [datoms (get-in (<! cb) [:body :data])]
           (let [{:keys [renderer camera scene scatter-plot line-geo line-material w h p-mat]} (om/get-state owner)
                 x-event (-> d3 (.extent datoms #(:x %)))
                 y-event (-> d3 (.extent datoms #(:y %)))
                 z-event (-> d3 (.extent datoms #(:z %)))
                 x-scale (-> d3 .-scale (.linear) (.domain x-event) (.range [0 100]))
                 y-scale (-> d3 .-scale (.linear) (.domain y-event) (.range [0 100]))
                 z-scale (-> d3 .-scale (.linear) (.domain z-event) (.range [0 100]))
                 vpts {:x-max (nth x-event 1)
                       :x-center (/ (+ (nth x-event 1)
                                       (nth x-event 0)) 2)
                       :x-min (nth x-event 0)
                       :y-max (nth y-event 1)
                       :y-center (/ (+ (nth y-event 1)
                                       (nth y-event 0)) 2)
                       :y-min (nth y-event 0)
                       :z-max (nth z-event 1)
                       :z-center (/ (+ (nth z-event 1)
                                       (nth z-event 0)) 2)
                       :z-min (nth z-event 0)}
                 vs [(v (x-scale (:x-min vpts)) (y-scale (:y-center vpts)) (z-scale (:z-center vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-center vpts)) (z-scale (:z-center vpts)))
                     (v (x-scale (:x-center vpts)) (y-scale (:y-min vpts)) (z-scale (:z-center vpts))) (v (x-scale (:x-center vpts)) (y-scale (:y-max vpts)) (z-scale (:z-center vpts)))
                     (v (x-scale (:x-center vpts)) (y-scale (:y-center vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-center vpts)) (y-scale (:y-center vpts)) (z-scale (:z-min vpts)))

                     (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))

                     (v (x-scale (:x-min vpts)) (y-scale (:y-center vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-center vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-center vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-center vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-center vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-center vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-center vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-center vpts)))

                     (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))

                     (v (x-scale (:x-center vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-center vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-center vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-center vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))

                     (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-max vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))

                     (v (x-scale (:x-min vpts)) (y-scale (:y-center vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-min vpts)) (y-scale (:y-center vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-max vpts)) (y-scale (:y-center vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-max vpts)) (y-scale (:y-center vpts)) (z-scale (:z-max vpts)))
                     (v (x-scale (:x-center vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-center vpts)) (y-scale (:y-max vpts)) (z-scale (:z-min vpts)))
                     (v (x-scale (:x-center vpts)) (y-scale (:y-min vpts)) (z-scale (:z-min vpts))) (v (x-scale (:x-center vpts)) (y-scale (:y-min vpts)) (z-scale (:z-max vpts)))
                     ]]
             (println vpts)
             (.log js/console (count vs))
             (. renderer (setSize w h))
             (dommy/append! (om/get-node owner "plot") (.-domElement renderer))
             (. renderer (setClearColor 0xEEEEEE 1.0))
             (aset camera "position" "x" 200)
             (aset camera "position" "y" -100)
             (aset camera "position" "z" 100)
             (aset scatter-plot "rotation" "y" 0)
             (doseq [v vs]
               (-> line-geo .-vertices (.push v)))
             (.log js/console line-material)
             (. scatter-plot (add (js/THREE.Line. line-geo
                                                  line-material
                                                  (.-Lines js/THREE))))

             (let [point-geo (js/THREE.Geometry.)]
               (doseq [d datoms]
                 (let [x (x-scale (:x d))
                       y (y-scale (:y d))
                       z (z-scale (:z d))]
                   (-> point-geo .-vertices (.push (v x y z)))
                   (-> point-geo .-colors (.push (js/THREE.Color. "red")))))
               (let [points (js/THREE.PointCloud. point-geo p-mat)]
                 (.log js/console )
                 (. scatter-plot (add points))

                 (log/fine "scatter-plot")
                 (.log js/console scatter-plot)
                 (. scene (add scatter-plot))
                 (.log js/console scene)
                 (.log js/console camera)
                 (. camera (lookAt (.-position scene)))
                 (. renderer (render scene camera))
                 (.log js/console renderer)
                 (.log js/console (.-info renderer))
                 )))
           ))))
  (render-state
   [_ {:keys [datoms renderer]}]
   (let [] 
     (html [:div.plot-view {:ref "plot"}]))))
