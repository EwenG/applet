(ns clj.applet.applet
  (:require [clojure.tools.nrepl.server]
            [clojure.core.async :as async]
            [clojure.edn :as edn])
  (:import [netscape.javascript JSObject])
  (:gen-class 
   :name clj.applet.Applet
   :extends java.applet.Applet
   :methods [[eventFromJs [String] void]]))

(def in-chan (async/mult (async/chan))) ;in means from the clojurescript/javascript app to the clojure app using the applet
(def out-chan (async/mult (async/chan))) ;out means from the clojure app using the applet to the clojurescript/javascript app

(defmacro run-privileged [& body]
  `(java.security.AccessController/doPrivileged 
    (reify java.security.PrivilegedAction 
      (~'run [~'this] ~@body))))

(def js-object-debug (atom nil))    ;Usefull for debugging at the repl
(def applet-debug (atom nil))       ;Usefull for debugging at the repl


(defn parse-ns-string [ns-string]
  (clojure.string/split ns-string #"[(.)(/)]"))

(defn get-member [jsobj member-names]
  (reduce #(.getMember %1 %2) 
          @js-object-debug 
          member-names))

(defn send-to-javascript [jsobj handler-param data]
  (let [handler-fn-splitted (-> handler-param 
                                parse-ns-string)
        handler-name (last handler-fn-splitted)
        namespace-splitted (butlast handler-fn-splitted)
        data-as-js (to-array [data])]
    (run-privileged (-> jsobj 
                        (get-member namespace-splitted) 
                        (.call handler-name data-as-js)))))

(defn start-nrepl-server []
  (try 
    (clojure.tools.nrepl.server/start-server :port 7888) 
    (catch Exception e (prn "Server already started"))))

(defn events-to-js [jsobj handler-param]
  (let [chan (async/tap out-chan (async/chan))]
    (async/go-loop 
     []
     (->> (async/<! chan) 
          (send-to-javascript jsobj handler-param))
     (recur))))






(defn -start [this]
  (when (= "true" (.getParameter this "start-nrepl"))
    (start-nrepl-server))
  (let [js-object (JSObject/getWindow this)]
    (reset! js-object-debug js-object)
    (reset! applet-debug this)
    (events-to-js
     js-object 
     (.getParameter this "handler-fn"))))

(defn -stop [this])

(defn -eventFromJs [this event]
  (async/go (->> event edn/read-string (async/>! (async/muxch* in-chan)))))






(defrecord Applet
    [in-chan out-chan])

(defn new-applet []
  (map->Applet {:in-chan in-chan
                :out-chan out-chan}))