(defproject touchVision "0.1.0"
  :description ""
  :url "https://github.com/GEverding/touchVision"
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.303.0-886421-alpha"]
                 [org.clojure/clojurescript "0.0-2234"]
                 [org.clojure/core.match "0.2.1"]
                 [org.clojure/tools.cli "0.2.2"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 ; Server
                 [compojure "1.1.6"]
                 [ring/ring-core "1.3.0-RC1"]
                 [ring/ring-devel "1.3.0-RC1"]
                 [ring/ring-json "0.3.1"]
                 [ring-mock "0.1.5"]
                 [http-kit "2.1.16"]
                 [lib-noir "0.8.3"]
                 [cheshire "5.3.1"]
                 [liberator "0.12.0-SNAPSHOT"]
                 [me.shenfeng/mustache "1.1"]
                 [com.taoensso/timbre "3.2.0"]
                 [clj-time "0.7.0"]
                 [com.datomic/datomic-free "0.9.4766"]
                 [prismatic/schema "0.2.4"]
                 ; Client
                 [prismatic/dommy "0.1.2"]
                 [sablono "0.2.17"]
                 [net.drib/strokes "0.5.1"]
                 [cljs-ajax "0.2.6"]
                 [prismatic/schema "0.2.5-SNAPSHOT"]
                 [prismatic/om-tools "0.2.1"]
                 [com.andrewmcveigh/cljs-time "0.1.5"]
                 [jarohen/chord "0.4.1"]
                 [om "0.6.3"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [lein-shell "0.3.0"]
            [codox "0.8.9"]
            [lein-cooper "0.0.1"] ]
  :source-paths ["src"]
  :test-paths ["test"]
  :repl-options {:init-ns user }
  :repositories {"sonatype-oss-public" "https://oss.sonatype.org/content/groups/public/"}
  :profiles { :dev {:source-paths ["dev" "src"]
                    :plugins [[lein-midje "3.1.3"]]
                    :dependencies [[org.clojure/tools.namespace "0.2.5-SNAPSHOT"]
                                   [org.clojure/java.classpath "0.2.0"]
                                   [midje "1.6.3"] ]}
             :prod {:aot :all
                    :hooks [leiningen.cljsbuild]
                    :main server.system } }
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
  :aliases  {"bower" ["shell" "bower" "install"]
             "scss" ["shell" "compass" "compile"]
             "dist" ["with-profile" "prod" "do" "bower," "uberjar," "cljsbuild" "once" "prod," "scss," "resource"]
             }
  :clean-targets [:target-path :compile-path "dist"]
  )
