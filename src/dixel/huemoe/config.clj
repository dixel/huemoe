(ns dixel.huemoe.config
  (:require [mount.core :as mount]
            [clojure.edn :as edn]
            [environ.core :refer [env]]
            [clojure.string :as str]
            [taoensso.timbre :as log]))

(mount/defstate config
  :start (let [huemoe-env (env :huemoe-env)
               configuration (case huemoe-env
                               nil (edn/read-string (slurp ".config.edn"))
                               "env" env
                               (edn/read-string (slurp huemoe-env)))]
           (update configuration :telegram-user-whitelist
                   (fn [maybe-whitelist]
                     (cond
                       (set? maybe-whitelist) maybe-whitelist
                       (string? maybe-whitelist) (into
                                                  #{}
                                                  (str/split maybe-whitelist #","))
                       :default (do
                                  (log/error "couldn't find :telegram-user-whitelist "
                                             "nobody is allowed to use the bot currently")
                                  #{}))))))
