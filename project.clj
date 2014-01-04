(defproject clj.applet/applet "0.0.1-SNAPSHOT"
  :aot :all
  :uberjar-name "clojure_applet_aot.jar"
  :repositories {"local" ~(str (.toURI (java.io.File. "local_maven_repo")))}
  :dependencies [[org.clojure/clojure "1.5.1"]
                 [org.clojure/tools.nrepl "0.2.3"]
                 [org.clojure/core.async "0.1.267.0-0d7780-alpha"]
                 [plugin/plugin "1.0.0"]
                 [deploy/deploy "1.0.0"]])