(ns app.router
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require
    [cljs.core.async :as async :refer [<! >! chan put! timeout]]
    [clojure.string :as s]
    [app.history :as H]))

(def ^:private routes (atom {}))

(def url-regex-patterns {:optional #"\((.*?\))"
                         :splat #"\*\w+"
                         :named #"(\(\?)?:\w+"
                         :escape #"[\-{}\[\]+?.,\\\^$|#\s]" })

(defn ^:private routeToRegExp
  "Convert Route to RegExp"
  [route]
  (let [escaped (s/replace route (:escape url-regex-patterns) "\\$&")
        optional (s/replace escaped (:optional url-regex-patterns) "(?:$1)?")
        named (s/replace optional (:named url-regex-patterns) #(if-not %2 "([^/?]+)" %1) )
        splatted (s/replace named (:splat url-regex-patterns) "([^?]*?)")]
    js/RegExp. (str "^" splatted "(?:\\?([\\s\\S]*))?$")))

(defn ^:private extract-url-params [route frag]
  (let [params (rest (.exec (js/RegExp. ( :regexp route)) frag))
        builder (fn [m] (if  (= (first (take-last 1 params)) m)
                          m ;; don't decode url (which is the last param)
                          (if-not (nil? m)
                            (js/decodeURIComponent m)
                             nil)) )]
     (remove nil? (map builder params))))

(defn find-route [path]
   (let [matcher (fn [r] (let [m (.match path (:regexp r))]
                           (.log js/console path)
                           (.log js/console r)
                           (.log js/console m)
                           m))
        matches (filter matcher @routes)
        match (first matches)]
   match))

(defn handle-change [path]
  (let [match (find-route path)
        handler (:handler match)
        handler-params (extract-url-params match path)
        ]
    (.log js/console (str "match " (:name match)))
    (apply handler handler-params)
    ))

(defn navigate
  ([path]
   (navigate path {:replace false}))
  ( [path options]
  (if-let [frag (H/navigate path options)]
    (.log js/console (str "navigating to " frag))
    false)))

(defn create-routes "Create and Start Router" [r]
  (let [paths (map #(:path %) r)
        compiled-routes (map #(assoc % :regexp (routeToRegExp (:path %))) r)
        history-chan (chan)]
    (reset! routes compiled-routes)
    (H/start history-chan)
    (go (while true
          (let [p (<! history-chan)]
            (.log js/console (str "go to " p))
            (handle-change p)))) ))
