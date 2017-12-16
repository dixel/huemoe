(ns huemoe.config
  (:require [mount.core :as mount]
            [clojure.edn :as edn]
            [environ.core :refer [env]]))

(mount/defstate config
  :start (let [huemoe-env (env :huemoe-env)]
           (case huemoe-env
             nil (edn/read-string (slurp ".config.edn"))
             "env" env
             (edn/read-string (slurp huemoe-env)))))
