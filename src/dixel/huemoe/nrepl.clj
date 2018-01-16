(ns dixel.huemoe.nrepl
  (:require [clojure.tools.nrepl.server :as nrepl]
            [dixel.huemoe.config :as conf]
            [mount.core :as mount]))

(mount/defstate nrepl
  :start (nrepl/start-server :port (read-string (str (conf/config :nrepl-port))))
  :stop (nrepl/stop-server nrepl))
