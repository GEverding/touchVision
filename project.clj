(defproject touchVision "0.2.0"
  :description ""
  :url "https://github.com/GEverding/touchVision"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/clojurescript "0.0-2511"]
                 [org.clojure/tools.cli "0.3.1"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.clojure/math.numeric-tower "0.0.4"]
                 ; Server
                 [compojure "1.3.1"]
                 [com.novemberain/langohr "3.0.1"]
                 [ring/ring-core "1.3.2"]
                 [ring/ring-devel "1.3.2"]
                 [ring/ring-json "0.3.1"]
                 [ring/ring-defaults "0.1.3"]
                 [http-kit "2.1.19"]
                 [cheshire "5.4.0"]
                 [me.shenfeng/mustache "1.1"]
                 [com.taoensso/timbre "3.3.1"]
                 [clj-time "0.9.0"]
                 [prismatic/schema "0.3.3"]
                 [de.ubercode.clostache/clostache "1.4.0"]
                 [com.stuartsierra/component "0.2.2"]
                 [prismatic/plumbing "0.3.5"]
                 [aprint "0.1.1"]
                 [environ "1.0.0"]
                 [ch.qos.logback/logback-classic "1.1.2"]

                 ;; Database
                 [yesql "0.4.0"]
                 [org.postgresql/postgresql "9.3-1102-jdbc41"]
                 [com.jolbox/bonecp "0.8.0.RELEASE"]
                 [org.clojure/java.jdbc "0.3.6"]
                 [clojure.jdbc/clojure.jdbc-c3p0 "0.3.1"]

                 ; Client
                 [prismatic/dommy "1.0.0"]
                 [sablono "0.2.22"]
                 [net.drib/strokes "0.5.1"]
                 [cljs-ajax "0.3.8"]
                 [prismatic/om-tools "0.3.10"]
                 [com.andrewmcveigh/cljs-time "0.3.0"]
                 [jarohen/chord "0.5.0" :exclusions [org.clojure/clojure]]
                 [org.om/om "0.8.0"]
                 [GEverding/cljs-log "0.1.0-SNAPSHOT"]]
  :plugins [[lein-cljsbuild "1.0.4"]
            [lein-cooper "0.0.1"] ]
  :source-paths ["src"]
  :test-paths ["test"]
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :profiles {:dev {:source-paths ["dev" "src"]
                   :dependencies [[cider/cider-nrepl "0.9.0-SNAPSHOT"]
                                  [org.clojure/tools.nrepl "0.2.7"]]
                   :repl-options {:init-ns user
                                  :nrepl-middleware
                                  [cider.nrepl.middleware.apropos/wrap-apropos
                                   cider.nrepl.middleware.classpath/wrap-classpath
                                   cider.nrepl.middleware.complete/wrap-complete
                                   cider.nrepl.middleware.format/wrap-format
                                   cider.nrepl.middleware.info/wrap-info
                                   cider.nrepl.middleware.inspect/wrap-inspect
                                   cider.nrepl.middleware.macroexpand/wrap-macroexpand
                                   cider.nrepl.middleware.ns/wrap-ns
                                   cider.nrepl.middleware.pprint/wrap-pprint
                                   cider.nrepl.middleware.resource/wrap-resource
                                   cider.nrepl.middleware.stacktrace/wrap-stacktrace
                                   cider.nrepl.middleware.test/wrap-test
                                   cider.nrepl.middleware.trace/wrap-trace
                                   cider.nrepl.middleware.undef/wrap-undef]}}
             :prod {:aot :all
                    :hooks [leiningen.cljsbuild]
                    :main server.system}}
  :cljsbuild {
    :builds [{:id "dev"
              :source-paths ["app"]
              :compiler {
                         :output-to "resources/public/js/touchVision.js"
                         :output-dir "resources/public/js/out"
                         :source-map "resources/public/js/touchVision.js.map"
                         :pretty-print true
                         :optimizations :none } }
              {:id "prod"
               :source-paths ["app"]
               :compiler {:output-to "resources/cljs/touchVision.min.js"
                          :output-dir "resources/cljs"
                          ;; TODO: Testing Purposes - Remove for Prod
                          :source-map "resources/cljs/touchVision.min.js.map"
                          :optimizations :advanced
                          :pretty-print false
                          :preamble ["react/react.min.js"]
                          :externs ["resources/public/js/lib/jquery.min.js"
                                    "resources/public/js/lib/bootstrap.min.js"
                                    "resources/public/js/lib/lodash.min.js"
                                    "resources/public/js/lib/d3.min.js"
                                    "react/externs/react.js" ] }}
             ]}
  :clean-targets [:target-path :compile-path "dist"])
