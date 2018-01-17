(ns dixel.huemoe.core
  (:gen-class)
  (:require [cheshire.core :as json]
            [clj-http.client :as http]
            [mount.core :as mount]
            [cyrus-config.core :as cfg]
            [taoensso.timbre :as log]
            [dixel.huemoe.bot :as bot]
            [dixel.huemoe.nrepl :as nrepl]))

(defn -main [& args]
  (cfg/validate!)
  (log/info (cfg/show))
  (mount/start)
  (while true
    (Thread/sleep Long/MAX_VALUE)))
