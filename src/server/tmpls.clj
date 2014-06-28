(ns server.tmpls
  (:require [me.shenfeng.mustache :refer [gen-tmpls-from-resources]]
            [server.config :refer [cfg]]))

(defn add-info [data]
  (assoc data
    :dev? (= (cfg :profile) :dev)
    :prod? (= (cfg :profile) :prod)))

(gen-tmpls-from-resources "templates" [".tpl"] add-info)

