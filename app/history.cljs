(ns client.history
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async :refer [<! >! chan put! timeout]]
    [goog.events :as events]
    [clojure.string :as s]
    [goog.events.EventType :as EventType])
  )

(def state (atom {:root "/" :started false}))

(defn ^:private routeStripper
  "Cached regex for stripping a leading hash/slash and trailing space."
  [path]
  (let [r  (js/RegExp. "^[#\\/]|\\s+$")
        ret (.replace path r "")]
    ret))

(defn ^:private rootStripper
  "Cached regex for stripping leading and trailing slashes."
  [path]
  (let [r  (js/RegExp. "^\\/+|\\/+$")
        ret (.replace path r "/") ]
    ret))

(defn ^:private pathStripper
  "Cached regex for stripping urls of hash."
  [path]
  (let [r (js/RegExp. "#.*$")
        ret (.replace path r "") ]
    ret))

(defn ^:private getPath []
  (let [loc (.-location js/window)
        full-path (js/decodeURI (str (.-pathname loc) (.-search loc)))
        trim-length (- (count full-path) (count (:root state)))
        path (apply str (take-last trim-length full-path)) ]
    (.log js/console (str "getPath: " path))
    path))

(defn ^:private getFragment [frag]
  (if-let [f frag]
    (do
      (.log js/console (str "getFragment: frag: " frag))
      (.log js/console (str "getFragment: f: " (routeStripper f)))
      (routeStripper f))
    (do
      (.log js/console (str "getFragment getPath: " (routeStripper (getPath))))
      (routeStripper (getPath)))))

(defn ^:private load-url [fragment]
  (let [c (:chan @state)]
    (.log js/console (str "new state " @state))
    (.log js/console (str "going to " fragment))
    (go
      (let [f (getFragment fragment)]
        (>! c f)))))

(defn ^:private check-url [f]
  (let [new-frag (getFragment f)
        old-frag (:fragment @state) ]
    (.log js/console (str "check-rul f: " f))
    (.log js/console (str "old fragment: " old-frag))
    (.log js/console (str "new fragment: " new-frag))
    (.log js/console (str "change? " (not= new-frag old-frag)))
    (if (not= new-frag old-frag)
      (do
        (reset! state (assoc @state :fragment new-frag))
        (load-url new-frag))
      false
      )))

(defn ^:private on-popstate [e]
 (check-url nil) )

(defn start [route-chan]
  (let [frag (getFragment nil) ]
    (reset! state {:root "/"
                   :started true
                   :fragment frag
                   :chan route-chan })
    (goog.events/listen js/window EventType/POPSTATE on-popstate)
    (load-url frag)))

(defn stop [])

(defn navigate [fragment options]
  (let [history (.-history js/window)
        frag (getFragment fragment)
        clean-frag  (js/decodeURI (pathStripper frag))
        url (str (:root @state) frag)
        url-clean (if (and (= clean-frag "") (not= url "/"))
                    (subs url 0 (dec (count url)))
                    url) ]
    (.log js/console (str "fragment " fragment))
    (.log js/console (str "frag: " frag))
    (.log js/console (str "frag clean: " clean-frag))
    (.log js/console (str "url clean: " url-clean))
    (if (= frag (:fragment @state))
           nil
           (do
             (if-not (:replace options)
               (.pushState history nil (-> js/window
                                            .-document
                                            .-title) url-clean)
               (.replaceState history nil (-> js/window
                                            .-document
                                            .-title) url-clean)
                )
           (check-url frag)
           frag))))

